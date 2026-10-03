package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.LayoutUnits
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.OfficeParsedDocument
import com.makerandreas.papirusoffice.data.odf.SvXMLImport
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Tiny source fixtures distinguish inheritance/reset and before/after, not just break counts. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ParagraphStyleSemanticsTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun odf(styles: String, body: String = "<text:p>Text</text:p>"): OfficeParsedDocument {
        val xml = """<office:document-content office:version="1.4"
            xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
            xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
            xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
            xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
            <office:automatic-styles>$styles</office:automatic-styles>
            <office:body><office:text>$body</office:text></office:body>
            </office:document-content>"""
        return SvXMLImport().parseOdfXml(xml, "semantics.odt").also {
            assertFalse(it.failureReason, it.isParsingFailed)
        }
    }

    private fun odfStyle(name: String, properties: String, parent: String? = null): String =
        """<style:style style:name="$name" style:family="paragraph" ${parent?.let { "style:parent-style-name=\"$it\"" } ?: ""}>
            <style:paragraph-properties $properties/></style:style>"""

    private fun sequence(doc: OfficeParsedDocument): List<String> = doc.elements.map {
        when (it) {
            is OfficeDocumentElement.PageBreak -> "BREAK"
            is OfficeDocumentElement.Paragraph -> "P:${it.text}"
            is OfficeDocumentElement.Heading -> "H:${it.text}"
            else -> "OTHER"
        }
    }

    @Test
    fun odfBreakAfterFollowsParagraphAndBreakBeforePrecedesIt() {
        val styles = odfStyle("After", """fo:break-after='page'""") +
            odfStyle("Before", """fo:break-before='page'""")
        val after = odf(styles, """<text:p text:style-name="After">A</text:p><text:p>B</text:p>""")
        assertEquals(listOf("P:A", "BREAK", "P:B"), sequence(after))
        assertFalse(after.styles.paragraphStyles.getValue("After").pageBreakBefore)
        assertTrue(after.styles.paragraphStyles.getValue("After").pageBreakAfter)
        val before = odf(styles, """<text:p>A</text:p><text:p text:style-name="Before">B</text:p>""")
        assertEquals(listOf("P:A", "BREAK", "P:B"), sequence(before))
    }

    @Test
    fun odfEmptyParagraphAndNestedHeadingRetainAuthoredBoundaries() {
        val styles = odfStyle("After", """fo:break-after='page'""") +
            odfStyle("Before", """fo:break-before='page'""")
        val parsed = odf(styles, """<text:p text:style-name="After"/>
            <text:section text:name="S"><text:h text:style-name="Before" text:outline-level="1">B</text:h></text:section>""")
        assertEquals(listOf("P:", "BREAK", "BREAK", "H:B"), sequence(parsed))
        assertFalse(parsed.plainText.contains("Page Break"))
    }

    @Test
    fun odfAutoAndNormalClearInheritedBreaksKeepsAndFixedLineHeight() {
        val styles = odfStyle("Base", """fo:break-before='page' fo:break-after='page' fo:keep-with-next='always' fo:line-height='24pt' fo:margin-bottom='12pt'""") +
            odfStyle("Percent", """fo:line-height='115%'""", "Base") +
            odfStyle("Reset", """fo:break-before='auto' fo:break-after='auto' fo:keep-with-next='auto' fo:line-height='normal' fo:margin-bottom='0pt'""", "Percent")
        val parsed = odf(styles, """<text:p text:style-name="Reset">A</text:p>""")
        val percent = parsed.styles.paragraphStyles.getValue("Percent")
        assertNull(percent.lineHeightExactUnits)
        assertEquals(1.15f, percent.lineHeightFactor, 0.0001f)
        assertTrue(percent.lineHeightUsesFontSize)
        val reset = parsed.styles.paragraphStyles.getValue("Reset")
        assertEquals(listOf("P:A"), sequence(parsed))
        assertFalse(reset.pageBreakBefore)
        assertFalse(reset.pageBreakAfter)
        assertFalse(reset.keepWithNext)
        assertFalse(reset.lineHeightUsesFontSize)
        assertNull(reset.lineHeightExactUnits)
        assertEquals(1f, reset.lineHeightFactor, 0f)
        assertEquals(0f, reset.spaceAfterUnits, 0f)
    }

    @Test
    fun odfMissingDeclarationInheritsAndExactResetsPercentageBasis() {
        val styles = odfStyle("Percent", """fo:line-height='150%'""") +
            odfStyle("Inherited", "", "Percent") +
            odfStyle("Exact", """fo:line-height='18pt'""", "Percent")
        val parsed = odf(styles)
        val inherited = parsed.styles.paragraphStyles.getValue("Inherited")
        assertTrue(inherited.lineHeightUsesFontSize)
        assertEquals(1.5f, inherited.lineHeightFactor, 0f)
        val exact = parsed.styles.paragraphStyles.getValue("Exact")
        assertFalse(exact.lineHeightUsesFontSize)
        assertEquals(24f, exact.lineHeightExactUnits!!, 0f)
    }

    private fun docx(styles: String, body: String, settings: String = ""): OfficeParsedDocument = runBlocking {
        val file = File.createTempFile("style-semantics-", ".docx", context.cacheDir)
        try {
            ZipOutputStream(file.outputStream()).use { zip ->
                fun part(name: String, xml: String) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(xml.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
                part("word/settings.xml", """<w:settings xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">$settings</w:settings>""")
                part("word/styles.xml", """<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">$styles</w:styles>""")
                part("word/document.xml", """<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:body>$body</w:body></w:document>""")
            }
            OfficeDocumentParser(context).parseDocument(file, bypassCache = true).also {
                assertFalse(it.failureReason, it.isParsingFailed)
            }
        } finally {
            OfficeDocumentParser.clearCacheForFile(file.absolutePath)
            file.delete()
        }
    }

    private fun wordStyle(name: String, properties: String, parent: String? = null): String =
        """<w:style w:type="paragraph" w:styleId="$name"><w:name w:val="$name"/>
            ${parent?.let { "<w:basedOn w:val=\"$it\"/>" } ?: ""}<w:pPr>$properties</w:pPr></w:style>"""

    private fun wordParagraph(properties: String, text: String = "A"): String =
        """<w:p><w:pPr>$properties</w:pPr><w:r><w:t>$text</w:t></w:r></w:p>"""

    @Test
    fun docxAutoOverridesExactAndMinimumThroughStylesAndDirectProperties() {
        val defaults = """<w:docDefaults><w:pPrDefault><w:pPr><w:spacing w:line="480" w:lineRule="exact"/></w:pPr></w:pPrDefault></w:docDefaults>"""
        val styles = defaults + wordStyle("Normal", "") +
            wordStyle("Minimum", """<w:spacing w:line="360" w:lineRule="atLeast"/>""", "Normal") +
            wordStyle("Auto", """<w:spacing w:line="276" w:lineRule="auto"/>""", "Minimum")
        val parsed = docx(styles, wordParagraph("""<w:pStyle w:val="Minimum"/><w:spacing w:line="240"/>"""))
        val normal = parsed.styles.paragraphStyles.getValue("Normal")
        assertEquals(LayoutUnits.twipsToUnits(480), normal.lineHeightExactUnits!!, 0f)
        val minimum = parsed.styles.paragraphStyles.getValue("Minimum")
        assertNull(minimum.lineHeightExactUnits)
        assertEquals(LayoutUnits.twipsToUnits(360), minimum.lineHeightMinimumUnits!!, 0f)
        val auto = parsed.styles.paragraphStyles.getValue("Auto")
        assertNull(auto.lineHeightExactUnits)
        assertNull(auto.lineHeightMinimumUnits)
        assertEquals(1.15f, auto.lineHeightFactor, 0f)
        val p = parsed.elements.filterIsInstance<OfficeDocumentElement.Paragraph>().single()
        val direct = parsed.styles.paragraphStyles.getValue(p.styleName!!)
        assertNull(direct.lineHeightExactUnits)
        assertNull(direct.lineHeightMinimumUnits)
        assertEquals(1f, direct.lineHeightFactor, 0f)
    }

    @Test
    fun docxDirectExactAndMinimumReplaceInheritedAuto() {
        val styles = wordStyle("Normal", """<w:spacing w:line="360" w:lineRule="auto"/>""")
        val parsed = docx(styles,
            wordParagraph("""<w:spacing w:line="480" w:lineRule="atLeast"/>""", "minimum") +
            wordParagraph("""<w:spacing w:line="120" w:lineRule="exact"/>""", "exact"))
        val ps = parsed.elements.filterIsInstance<OfficeDocumentElement.Paragraph>()
            .map { parsed.styles.paragraphStyles.getValue(it.styleName!!) }
        assertNull(ps[0].lineHeightExactUnits)
        assertEquals(32f, ps[0].lineHeightMinimumUnits!!, 0f)
        assertNull(ps[1].lineHeightMinimumUnits)
        assertEquals(8f, ps[1].lineHeightExactUnits!!, 0f)
    }

    @Test
    fun docxMinimumInDefaultsSurvivesUnrelatedDirectSpacing() {
        val defaults = """<w:docDefaults><w:pPrDefault><w:pPr><w:spacing w:line="600" w:lineRule="atLeast"/></w:pPr></w:pPrDefault></w:docDefaults>"""
        val parsed = docx(defaults, wordParagraph("""<w:spacing w:after="0"/>"""))
        val p = parsed.elements.filterIsInstance<OfficeDocumentElement.Paragraph>().single()
        val style = parsed.styles.paragraphStyles.getValue(p.styleName!!)
        assertEquals(40f, style.lineHeightMinimumUnits!!, 0f)
        assertNull(style.lineHeightExactUnits)
    }

    @Test
    fun docxExplicitFalseAndZeroOverrideDefaultsAndParents() {
        val defaults = """<w:docDefaults><w:pPrDefault><w:pPr><w:keepNext/><w:pageBreakBefore/></w:pPr></w:pPrDefault></w:docDefaults>"""
        val styles = defaults + wordStyle("Normal", """<w:spacing w:after="240"/>""") +
            wordStyle("Reset", """<w:keepNext w:val="0"/><w:pageBreakBefore w:val="false"/><w:spacing w:after="0"/>""", "Normal")
        val parsed = docx(styles, wordParagraph("""<w:pStyle w:val="Normal"/><w:keepNext w:val="off"/><w:pageBreakBefore w:val="0"/><w:spacing w:after="0"/>"""))
        assertTrue(parsed.styles.paragraphStyles.getValue("Normal").keepWithNext)
        assertTrue(parsed.styles.paragraphStyles.getValue("Normal").pageBreakBefore)
        val p = parsed.elements.filterIsInstance<OfficeDocumentElement.Paragraph>().single()
        for (name in listOf("Reset", p.styleName!!)) {
            val style = parsed.styles.paragraphStyles.getValue(name)
            assertFalse(style.keepWithNext)
            assertFalse(style.pageBreakBefore)
            assertEquals(0f, style.spaceAfterUnits, 0f)
        }
    }
    @Test
    fun docxTabDefinitionsDoNotBecomeTextAndCascadeWithClearAndSettings() {
        val styles = wordStyle("Normal", """<w:tabs><w:tab w:val="left" w:pos="720"/></w:tabs><w:keepLines/><w:widowControl/>""") +
            wordStyle("Child", """<w:tabs><w:tab w:val="clear" w:pos="720"/><w:tab w:val="right" w:pos="1440"/></w:tabs><w:keepLines w:val="0"/><w:widowControl w:val="false"/>""", "Normal")
        val parsed = docx(styles, """<w:p><w:pPr><w:pStyle w:val="Child"/><w:tabs><w:tab w:val="center" w:pos="2160"/></w:tabs></w:pPr>
            <w:r><w:t>A</w:t><w:tab/><w:t>B</w:t></w:r></w:p>""", """<w:defaultTabStop w:val="960"/>""")
        val p = parsed.elements.filterIsInstance<OfficeDocumentElement.Paragraph>().single()
        assertEquals("A\tB", p.text)
        val style = parsed.styles.paragraphStyles.getValue(p.styleName!!)
        assertEquals(listOf(48f, 96f, 144f), style.tabStops.map { it.positionUnits })
        assertEquals(com.makerandreas.papirusoffice.data.TabAlignment.CLEAR, style.tabStops.first().alignment)
        assertEquals(64f, style.defaultTabIntervalUnits, 0f)
        assertFalse(style.keepTogether)
        assertEquals(1, style.widows)
        assertTrue(style.collapseSpacing)
    }

    @Test
    fun odfTabStopListAndKeepWidowOrphanDeclarationsAreResolved() {
        val styles = """<style:style style:name="Base" style:family="paragraph"><style:paragraph-properties
            fo:keep-together="always" fo:orphans="3" fo:widows="4" style:tab-stop-distance="2cm">
            <style:tab-stops><style:tab-stop style:position="3cm" style:type="right"/></style:tab-stops>
            </style:paragraph-properties></style:style>
            <style:style style:name="Child" style:family="paragraph" style:parent-style-name="Base">
            <style:paragraph-properties fo:keep-together="auto"><style:tab-stops/></style:paragraph-properties></style:style>"""
        val parsed = odf(styles)
        val base = parsed.styles.paragraphStyles.getValue("Base")
        assertTrue(base.keepTogether)
        assertEquals(3, base.orphans)
        assertEquals(4, base.widows)
        assertEquals(LayoutUnits.cmToUnits(2f), base.defaultTabIntervalUnits, 0.01f)
        assertEquals(LayoutUnits.cmToUnits(3f), base.tabStops.single().positionUnits, 0.01f)
        val child = parsed.styles.paragraphStyles.getValue("Child")
        assertFalse(child.keepTogether)
        assertTrue(child.tabStops.isEmpty())
        assertEquals(4, child.widows)
    }

    @Test
    fun docxInlinePageBreaksRetainOneLogicalParagraphAndSourceOffsets() {
        val parsed = docx("", """<w:p><w:r><w:t xml:space="preserve"> A </w:t><w:br w:type="page"/><w:t>B</w:t><w:br w:type="page"/></w:r></w:p>""")
        val p = parsed.elements.filterIsInstance<OfficeDocumentElement.Paragraph>().single()
        assertEquals(" A B", p.text)
        assertEquals(listOf(3, 4), p.pageBreakOffsets)
        assertFalse(parsed.elements.any { it is OfficeDocumentElement.PageBreak })
    }

    @Test
    fun docxSectionTypeBelongsToTheSectionItDescribesNotTheFollowingOne() {
        val parsed = docx("", """<w:p><w:pPr><w:sectPr><w:type w:val="oddPage"/></w:sectPr></w:pPr><w:r><w:t>A</w:t></w:r></w:p>
            <w:p><w:pPr><w:sectPr><w:type w:val="continuous"/></w:sectPr></w:pPr><w:r><w:t>B</w:t></w:r></w:p>
            <w:p><w:r><w:t>C</w:t></w:r></w:p><w:sectPr><w:type w:val="evenPage"/></w:sectPr>""")
        assertEquals(listOf(0, 1, 2), parsed.sectionStarts.map { it.elementIndex })
        assertEquals(listOf(com.makerandreas.papirusoffice.data.SectionStartKind.ODD_PAGE,
            com.makerandreas.papirusoffice.data.SectionStartKind.CONTINUOUS,
            com.makerandreas.papirusoffice.data.SectionStartKind.EVEN_PAGE), parsed.sectionStarts.map { it.kind })
        assertFalse(parsed.elements.any { it is OfficeDocumentElement.PageBreak })
    }

    @Test
    fun odfMasterPageAssignmentRetainsItsAuthoredPageBoundary() {
        val styles = """<style:style style:name="Chapter" style:family="paragraph" style:master-page-name="Next"/>"""
        val parsed = odf(styles, """<text:p>A</text:p><text:p text:style-name="Chapter">B</text:p>""")
        assertEquals("Next", parsed.styles.paragraphStyles.getValue("Chapter").masterPageName)
        // This is not a cached soft break or an fo:break-before declaration.
        assertFalse(parsed.styles.paragraphStyles.getValue("Chapter").pageBreakBefore)
    }

}
