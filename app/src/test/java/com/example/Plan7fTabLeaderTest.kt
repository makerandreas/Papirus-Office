package com.example

import androidx.compose.ui.text.AnnotatedString
import com.example.modules.inky.ParagraphProjection
import com.makerandreas.papirusoffice.data.AdvanceSource
import com.makerandreas.papirusoffice.data.DocumentStyles
import com.makerandreas.papirusoffice.data.LayoutEngine
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.ParagraphStyle
import com.makerandreas.papirusoffice.data.ParagraphTabStop
import com.makerandreas.papirusoffice.data.PageStyleSpec
import com.makerandreas.papirusoffice.data.TableAdvanceSource
import com.makerandreas.papirusoffice.data.TabAlignment
import com.makerandreas.papirusoffice.data.odf.OdtImportPipeline
import com.makerandreas.papirusoffice.data.odf.SvXMLImport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Plan 7F leader-commit gate: ODF textual leaders only; no line-style painter. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Plan7fTabLeaderTest {
    private val body = ParagraphStyle("Body", fontFamily = "Times New Roman")
    private val spec = PageStyleSpec.FALLBACK.copy(widthDp = 320f, marginStartDp = 20f, marginEndDp = 20f)

    private fun layout(paragraph: OfficeParagraph, stops: List<ParagraphTabStop>, source: AdvanceSource = TableAdvanceSource) =
        LayoutEngine(spec, advanceSource = source).layoutParagraph(
            0,
            paragraph,
            DocumentStyles(defaultParagraphStyle = body.copy(tabStops = stops))
        )

    @Test
    fun allSixOdtFixturesImportTheirDeclaredTextLeaderSubset() {
        val expectedRawCounts = mapOf(1 to 0, 2 to 2, 3 to 0, 4 to 2, 5 to 2, 6 to 3)
        for (sample in SampleMatrix.sampleNumbers) {
            val file = SampleMatrix.findTestFile(SampleMatrix.odtName(sample))
            val parsed = OdtImportPipeline().parse(file)
            assertFalse("${file.name}: ${parsed.failureReason}", parsed.isParsingFailed)

            val rawCount = parsed.odtPackageData!!.entries
                .filterKeys { it == "content.xml" || it == "styles.xml" }
                .values
                .sumOf { bytes -> Regex("style:leader-text\\s*=").findAll(bytes.toString(Charsets.UTF_8)).count() }
            assertEquals("${file.name} raw textual-leader declarations", expectedRawCounts.getValue(sample), rawCount)

            val importedStops = buildList {
                parsed.styles.defaultParagraphStyle?.let { addAll(it.tabStops) }
                parsed.styles.paragraphStyles.values.forEach { addAll(it.tabStops) }
            }
            val importedLeaders = importedStops.filter { it.leaderText != null }
            assertTrue("${file.name} should import at least its declared leader stops", importedLeaders.size >= rawCount)
            assertTrue("${file.name} has no unsupported line-only claim", importedLeaders.all { it.leaderText == "." })
            if (rawCount == 0) assertTrue("${file.name} should remain leader-free", importedLeaders.isEmpty())
        }
    }

    @Test
    fun textLeaderWinsOverLineAttributesAndLineOnlyStyleIsDeferred() {
        val withText = parseStyledParagraph(
            "style:leader-style=\"dotted\" style:leader-type=\"none\" style:leader-text=\".\""
        )
        val stop = withText.styles.paragraphStyles.getValue("Leader").tabStops.single()
        assertEquals(".", stop.leaderText)
        val withTextParagraph = withText.elements
            .filterIsInstance<com.makerandreas.papirusoffice.data.OfficeDocumentElement.Paragraph>().single()
        val withTextLayout = LayoutEngine(spec, advanceSource = TableAdvanceSource).layoutParagraph(
            0, OfficeParagraph(withTextParagraph.text, withTextParagraph.styleName), withText.styles
        )
        assertTrue("explicit text remains active even when line type is none", withTextLayout.lines.single().tabLeaders.isNotEmpty())

        val lineOnly = parseStyledParagraph("style:leader-style=\"dotted\" style:leader-type=\"single\"")
        assertEquals(null, lineOnly.styles.paragraphStyles.getValue("Leader").tabStops.single().leaderText)
        val paragraph = lineOnly.elements
            .filterIsInstance<com.makerandreas.papirusoffice.data.OfficeDocumentElement.Paragraph>().single()
        val lineOnlyLayout = LayoutEngine(spec, advanceSource = TableAdvanceSource).layoutParagraph(
            0, OfficeParagraph(paragraph.text, paragraph.styleName), lineOnly.styles
        )
        assertTrue("line-style-only support is explicitly deferred", lineOnlyLayout.lines.single().tabLeaders.isEmpty())
    }

    @Test
    fun leadersFillOnlyTheAlreadyReservedGapForEverySupportedTabAlignment() {
        val paragraph = OfficeParagraph("\t12.3")
        val alignments = listOf(
            TabAlignment.LEFT,
            TabAlignment.RIGHT,
            TabAlignment.CENTER,
            TabAlignment.DECIMAL
        )
        for (alignment in alignments) {
            val withLeader = layout(paragraph, listOf(ParagraphTabStop(100f, alignment, "."))).lines.single()
            val withoutLeader = layout(paragraph, listOf(ParagraphTabStop(100f, alignment))).lines.single()
            assertEquals("$alignment caret advances", withoutLeader.caretAdvances, withLeader.caretAdvances)
            assertEquals("$alignment reserved width", withoutLeader.width, withLeader.width, 0f)
            assertEquals("$alignment leader source offset", 0, withLeader.tabLeaders.single().sourceOffset)
            assertEquals(".", withLeader.tabLeaders.single().requestedText)
            assertTrue(withLeader.tabLeaders.single().widthUnits <= withLeader.caretAdvances[1] + 0.001f)
        }

        val cleared = layout(paragraph, listOf(
            ParagraphTabStop(48f, TabAlignment.CLEAR, "*"),
            ParagraphTabStop(100f, TabAlignment.LEFT, ".")
        )).lines.single()
        assertEquals(100f, cleared.caretAdvances[1], 0.001f)
        assertEquals(listOf("."), cleared.tabLeaders.map { it.requestedText })
    }

    @Test
    fun unsupportedCharacterFallsBackAndNarrowGapsDoNotEmitLeaders() {
        val fallback = layout(OfficeParagraph("\tlabel"), listOf(ParagraphTabStop(80f, leaderText = "§"))).lines.single()
        val leader = fallback.tabLeaders.single()
        assertEquals("§", leader.requestedText)
        assertTrue(leader.fallbackApplied)
        assertTrue(leader.renderedText.isNotEmpty())
        assertTrue(leader.renderedText.all { it == '.' })

        val narrow = layout(OfficeParagraph("\tlabel"), listOf(ParagraphTabStop(2f, leaderText = "."))).lines.single()
        assertEquals(2f, narrow.caretAdvances[1], 0.001f)
        assertTrue(narrow.tabLeaders.isEmpty())

        val invalid = parseStyledParagraph("style:leader-text=\"..\"")
        assertEquals(null, invalid.styles.paragraphStyles.getValue("Leader").tabStops.single().leaderText)
    }

    @Test
    fun displayProjectionAddsLeaderGlyphsWithoutChangingSourceOrSelectionOffsets() {
        val paragraph = OfficeParagraph("A\tB")
        val styles = DocumentStyles(defaultParagraphStyle = body.copy(
            tabStops = listOf(ParagraphTabStop(100f, leaderText = "."))
        ))
        val line = LayoutEngine(spec, advanceSource = TableAdvanceSource).layoutParagraph(0, paragraph, styles).lines.single()
        val leader = line.tabLeaders.single()
        val transformed = ParagraphProjection(listOf(line), emptyList(), 1f, paragraph, styles)
            .filter(AnnotatedString(paragraph.text))
        val mapping = transformed.offsetMapping
        val tabDisplayStart = mapping.originalToTransformed(1)

        assertTrue(transformed.text.text.contains(leader.renderedText))
        assertEquals(1, mapping.transformedToOriginal(tabDisplayStart))
        repeat(leader.renderedText.length) { index ->
            assertEquals("inserted leader maps to its source tab", 1, mapping.transformedToOriginal(tabDisplayStart + index))
        }
        assertEquals(2, mapping.transformedToOriginal(mapping.originalToTransformed(2)))
        assertEquals("A\tB", paragraph.text)
        assertEquals(paragraph.text.length, mapping.transformedToOriginal(transformed.text.length))
    }

    private fun parseStyledParagraph(tabStopAttributes: String) = SvXMLImport().parseOdfXml(
        """
        <office:document-content
            xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
            xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
            xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
          <office:automatic-styles>
            <style:style style:name="Leader" style:family="paragraph">
              <style:paragraph-properties>
                <style:tab-stops><style:tab-stop style:position="50pt" $tabStopAttributes/></style:tab-stops>
              </style:paragraph-properties>
            </style:style>
          </office:automatic-styles>
          <office:body><office:text><text:p text:style-name="Leader">A<text:tab/>B</text:p></office:text></office:body>
        </office:document-content>
        """.trimIndent(),
        "plan-7f-leader.odt"
    )
}
