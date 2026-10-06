package com.example

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.CharacterStyle
import com.makerandreas.papirusoffice.data.DocumentStyles
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.OfficeParsedDocument
import com.makerandreas.papirusoffice.data.OfficeRuns
import com.makerandreas.papirusoffice.data.OfficeTextRun
import com.makerandreas.papirusoffice.data.ParagraphStyle
import com.makerandreas.papirusoffice.data.StyleResolver
import com.makerandreas.papirusoffice.data.toOfficeDocument
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Plan 8A commit 2 gate: the DOCX run model.
 *
 * One run is built per `w:r`, with its own tri-state character properties, so a
 * flag cannot leak into the next run and an explicit negative (`w:val="0"`) can
 * turn off what the paragraph style set. Every case parses a synthetic package
 * built inside the test, so the assertions are about the rule rather than about
 * a fixture (audit-019 section 4.2).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocxRunFormattingTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val wNs = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"

    private fun docx(styles: String = "", body: String): OfficeParsedDocument = runBlocking {
        val file = File.createTempFile("docx-run-formatting-", ".docx", context.cacheDir)
        try {
            ZipOutputStream(file.outputStream()).use { zip ->
                fun part(name: String, xml: String) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(xml.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
                part("word/styles.xml", "<w:styles xmlns:w=\"$wNs\">$styles</w:styles>")
                part("word/document.xml", "<w:document xmlns:w=\"$wNs\"><w:body>$body</w:body></w:document>")
            }
            OfficeDocumentParser(context).parseDocument(file, bypassCache = true).also {
                assertFalse(it.failureReason, it.isParsingFailed)
            }
        } finally {
            OfficeDocumentParser.clearCacheForFile(file.absolutePath)
            file.delete()
        }
    }

    private fun style(id: String, type: String = "paragraph", name: String? = null, rPr: String = ""): String =
        buildString {
            append("<w:style w:type=\"$type\" w:styleId=\"$id\"><w:name w:val=\"${name ?: id}\"/>")
            if (rPr.isNotEmpty()) append("<w:rPr>$rPr</w:rPr>")
            append("</w:style>")
        }

    private fun run(text: String, rPr: String = ""): String =
        "<w:r>" + (if (rPr.isEmpty()) "" else "<w:rPr>$rPr</w:rPr>") + "<w:t>$text</w:t></w:r>"

    private fun paragraph(pPr: String, vararg runs: String): String =
        "<w:p>" + (if (pPr.isEmpty()) "" else "<w:pPr>$pPr</w:pPr>") + runs.joinToString("") + "</w:p>"

    private fun firstParagraph(parsed: OfficeParsedDocument) =
        parsed.elements.filterIsInstance<OfficeDocumentElement.Paragraph>().single()

    private fun firstHeading(parsed: OfficeParsedDocument) =
        parsed.elements.filterIsInstance<OfficeDocumentElement.Heading>().single()

    /** The single display paragraph of the document, which is what display consumes. */
    private fun displayParagraph(parsed: OfficeParsedDocument): Pair<OfficeParagraph, DocumentStyles> {
        val document = parsed.toOfficeDocument()
        val paragraph = document.body.elements.filterIsInstance<OfficeParagraph>().single()
        return paragraph to document.styles
    }

    /**
     * Span the run override added for `[start, end)`. The base span covers the
     * whole paragraph, and a run that covers the whole paragraph has the same
     * range, so take the last match: overrides are appended after the base.
     */
    private fun spanFor(annotated: AnnotatedString, start: Int, end: Int) =
        annotated.spanStyles.last { it.start == start && it.end == end }.item

    @Test
    fun oneRunPerAuthoredRunAndNoFlagLeaks() {
        val parsed = docx(
            body = paragraph(
                "",
                run("plain "),
                run("bold", "<w:b/>"),
                run(" plain")
            )
        )
        val paragraph = firstParagraph(parsed)
        assertEquals("plain bold plain", paragraph.text)
        assertEquals("the runs must cover the paragraph exactly", paragraph.text, paragraph.runs.joinToString("") { it.text })
        assertEquals(3, paragraph.runs.size)
        assertNull("a run that states nothing stays null", paragraph.runs[0].isBold)
        assertEquals(true, paragraph.runs[1].isBold)
        assertNull("bold must not leak past its own run", paragraph.runs[2].isBold)
    }

    @Test
    fun explicitNegativeTurnsOffParagraphBold() {
        val parsed = docx(
            styles = style("StrongBody", rPr = "<w:b/><w:sz w:val=\"32\"/>"),
            body = paragraph(
                "<w:pStyle w:val=\"StrongBody\"/>",
                run("bold "),
                run("plain", "<w:b w:val=\"0\"/><w:i w:val=\"false\"/>")
            )
        )
        val (paragraph, styles) = displayParagraph(parsed)
        val base = StyleResolver.resolveParagraphStyle(paragraph.styleName, styles)
        assertTrue("the paragraph style is bold", base.isBold)
        assertEquals(16f, base.fontSizeSp, 0.01f)

        val annotated = OfficeRuns.toAnnotatedString(paragraph, styles, 1f, Color.Black)
        assertEquals("the base span carries the paragraph bold", FontWeight.Bold, annotated.spanStyles.first().item.fontWeight)
        val override = spanFor(annotated, 5, paragraph.text.length)
        assertEquals("w:b w:val=0 must subtract the paragraph bold", FontWeight.Normal, override.fontWeight)
        assertEquals("the run recorded the explicit negative", false, paragraph.runs[1].isBold)
        assertNull("the first run kept the paragraph bold", paragraph.runs[0].isBold)
    }

    @Test
    fun paragraphMarkRunPropertiesNeverReachTheRuns() {
        val parsed = docx(body = paragraph("<w:rPr><w:b/><w:vanish/></w:rPr>", run("text")))
        val paragraph = firstParagraph(parsed)
        assertNull("the paragraph mark's own w:rPr is not run formatting", paragraph.runs.single().isBold)
        assertNull("and it does not hide the text either", paragraph.runs.single().isHidden)

        val (displayed, styles) = displayParagraph(parsed)
        val annotated = OfficeRuns.toAnnotatedString(displayed, styles, 1f, Color.Black)
        assertTrue(
            "nothing in the paragraph is bold",
            annotated.spanStyles.none { it.item.fontWeight == FontWeight.Bold }
        )
    }

    @Test
    fun runStyleResolvesThroughTheCharacterStyleTable() {
        val parsed = docx(
            styles = style("Strong", type = "character", name = "Strong", rPr = "<w:b/><w:color w:val=\"FF0000\"/>"),
            body = paragraph("", run("marked", "<w:rStyle w:val=\"Strong\"/>"))
        )
        val paragraph = firstParagraph(parsed)
        assertEquals("Strong", paragraph.runs.single().styleName)
        assertNull("the run itself states no bold; its style does", paragraph.runs.single().isBold)

        val (displayed, styles) = displayParagraph(parsed)
        val annotated = OfficeRuns.toAnnotatedString(displayed, styles, 1f, Color.Black)
        val override = spanFor(annotated, 0, "marked".length)
        assertEquals(FontWeight.Bold, override.fontWeight)
        assertEquals(Color(0xFFFF0000), override.color)
    }

    @Test
    fun directRunFormattingBeatsTheRunStyle() {
        val parsed = docx(
            styles = style("Big", type = "character", name = "Big", rPr = "<w:sz w:val=\"40\"/><w:b/>"),
            body = paragraph(
                "",
                run("direct", "<w:rStyle w:val=\"Big\"/><w:sz w:val=\"24\"/><w:b w:val=\"0\"/>"),
                run("styled", "<w:rStyle w:val=\"Big\"/>")
            )
        )
        val paragraph = firstParagraph(parsed)
        assertEquals("the direct w:sz is the run's own value", 12f, paragraph.runs[0].fontSizeSp!!, 0.01f)
        assertEquals("the explicit negative is the run's own value", false, paragraph.runs[0].isBold)
        assertNull("the second run states nothing; its style supplies the 20 pt", paragraph.runs[1].fontSizeSp)

        val (displayed, styles) = displayParagraph(parsed)
        val annotated = OfficeRuns.toAnnotatedString(displayed, styles, 1f, Color.Black)
        val direct = spanFor(annotated, 0, 6)
        val styled = spanFor(annotated, 6, 12)
        assertEquals("direct w:sz 24 wins over the run style", 12f, direct.fontSize!!.value, 0.01f)
        assertEquals(FontWeight.Normal, direct.fontWeight)
        assertEquals(20f, styled.fontSize!!.value, 0.01f)
        assertEquals(FontWeight.Bold, styled.fontWeight)
    }

    @Test
    fun runColourHighlightAndVanishReachTheDisplayedSpan() {
        val parsed = docx(
            body = paragraph(
                "",
                run("red", "<w:color w:val=\"00AA00\"/><w:highlight w:val=\"cyan\"/>"),
                run("ghost", "<w:vanish/>")
            )
        )
        val paragraph = firstParagraph(parsed)
        assertEquals("00AA00", paragraph.runs[0].colorHex)
        assertEquals("cyan", paragraph.runs[0].highlight)
        assertEquals(true, paragraph.runs[1].isHidden)

        val (displayed, styles) = displayParagraph(parsed)
        val annotated = OfficeRuns.toAnnotatedString(displayed, styles, 1f, Color.Black)
        val coloured = spanFor(annotated, 0, 3)
        assertEquals(Color(0xFF00AA00), coloured.color)
        assertEquals(Color(0xFF00FFFF), coloured.background)
        val ghost = spanFor(annotated, 3, 8)
        assertEquals(Color.Transparent, ghost.color)
    }

    @Test
    fun tabsBreaksAndSpacesStayInsideTheirOwnRun() {
        val parsed = docx(
            body = paragraph(
                "",
                "<w:r><w:rPr><w:b/></w:rPr><w:t>a</w:t><w:tab/><w:t>b</w:t></w:r>",
                "<w:r><w:t>x</w:t><w:br/><w:t>y</w:t></w:r>"
            )
        )
        val paragraph = firstParagraph(parsed)
        assertEquals("a\tb", paragraph.runs[0].text)
        assertEquals(true, paragraph.runs[0].isBold)
        assertEquals("x\ny", paragraph.runs[1].text)
        assertNull(paragraph.runs[1].isBold)
        assertEquals(paragraph.text, paragraph.runs.joinToString("") { it.text })
    }

    @Test
    fun textTheFileWroteOutsideARunBecomesANeutralRun() {
        val parsed = docx(
            styles = style("Numbered", rPr = "<w:b/>"),
            body = paragraph(
                "<w:pStyle w:val=\"Numbered\"/><w:numPr><w:numId w:val=\"7\"/></w:numPr>",
                run("item")
            )
        )
        val paragraph = firstParagraph(parsed)
        val runs = paragraph.runs
        assertEquals("the placeholder bullet is not authored run text", "\u2022 ", runs.first().text)
        assertNull("the filler run states nothing", runs.first().isBold)
        assertEquals("item", runs.last().text)
        assertEquals(paragraph.text, runs.joinToString("") { it.text })
    }

    @Test
    fun headingRunsAreCarriedLikeParagraphRuns() {
        val parsed = docx(
            styles = style("Judul1", name = "heading 1", rPr = "<w:b/>"),
            body = paragraph("<w:pStyle w:val=\"Judul1\"/>", run("Heading "), run("mix", "<w:i/>"))
        )
        val heading = firstHeading(parsed)
        assertEquals("Heading mix", heading.text)
        assertEquals(heading.text, heading.runs.joinToString("") { it.text })
        assertNull("the heading style states bold, not the run", heading.runs[0].isBold)
        assertEquals("the italic is the second run's own declaration", true, heading.runs[1].isItalic)

        // The same paragraph shape the paginator builds, so display resolves the
        // heading style's bold over run 1 and the run's italic over run 2.
        val (displayed, styles) = displayParagraph(parsed)
        val annotated = OfficeRuns.toAnnotatedString(displayed, styles, 1f, Color.Black)
        assertEquals(FontWeight.Bold, annotated.spanStyles.first().item.fontWeight)
        val italicRun = spanFor(annotated, "Heading ".length, heading.text.length)
        assertEquals(FontWeight.Bold, italicRun.fontWeight)
        assertEquals(FontStyle.Italic, italicRun.fontStyle)
    }

    /**
     * The comparison side of the same rule, without a file: a run's own value
     * always wins, so `false` subtracts, and a character style only fills the
     * gap the run leaves.
     */
    @Test
    fun mergeRunSubtractsAndPrefersDirectFormatting() {
        val base = ParagraphStyle("Body", fontSizeSp = 12f, isBold = true, isItalic = true)
        val styles = DocumentStyles(
            characterStyles = mapOf("Emph" to CharacterStyle("Emph", fontSizeSp = 18f, colorHex = "#123456"))
        )

        val subtract = OfficeRuns.mergeRun(OfficeTextRun(text = "x", isBold = false, isItalic = false), base, styles)
        assertFalse("a run can turn bold off", subtract.isBold)
        assertFalse("and italic off", subtract.isItalic)
        assertEquals("untouched properties still come from the paragraph", 12f, subtract.fontSizeSp, 0.01f)

        val fromStyle = OfficeRuns.mergeRun(OfficeTextRun(text = "x", characterStyle = "Emph"), base, styles)
        assertEquals(18f, fromStyle.fontSizeSp, 0.01f)
        assertEquals("#123456", fromStyle.colorHex)
        assertTrue("the character style does not cancel the paragraph bold", fromStyle.isBold)

        val directWins = OfficeRuns.mergeRun(
            OfficeTextRun(text = "x", characterStyle = "Emph", fontSizeSp = 9f, colorHex = "#ABCDEF"),
            base,
            styles
        )
        assertEquals(9f, directWins.fontSizeSp, 0.01f)
        assertEquals("#ABCDEF", directWins.colorHex)
    }
}
