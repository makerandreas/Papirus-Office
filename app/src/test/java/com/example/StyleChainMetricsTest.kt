package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.LayoutUnits
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.StyleResolver
import com.makerandreas.papirusoffice.data.toOfficeDocument
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Plan 5d metric style chain assertions.
 *
 * Verifies:
 * 1. Sample-1 DOCX Normal: 11 pt, after 160 twips, line 276 auto (1.15 factor).
 * 2. Sample-4 DOCX cascade order: docDefaults line 360 (1.5 factor), firstLine 720,
 *    Normal inherits it, TidakAdaSpasi overrides line 240 (1.0 factor).
 * 3. Plan 8A commit 1: heading sizes and colour arrive through the linked
 *    character style (`w:link` in Sample 4, the `X`/`X Char` name convention in
 *    Samples 1, 2, 5 and 6), while Samples 2 and 5 keep their own 14 pt level-1
 *    size because the paragraph style wins for the properties it sets.
 * 4. Sample-3 DOCX and ODT Heading 1: resolves to 20 pt.
 * 5. Per-paragraph numbering state: inherited from the style chain in Sample 6,
 *    explicitly suppressed by `w:numId 0` in Sample 2.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StyleChainMetricsTest {

    private fun parseOfficeDoc(name: String) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)
        val file = SampleMatrix.findTestFile(name)
        assertTrue("$name must exist", file.exists() && file.length() > 0)
        val parsed = parser.parseDocument(file, bypassCache = true)
        assertFalse("$name parse failed: ${parsed.failureReason}", parsed.isParsingFailed)
        parsed
    }

    @Test
    fun sample1DocxNormalStyleHasElevenPointsAndSpecificSpacing() {
        val parsed = parseOfficeDoc("Sample-1.docx")
        val doc = parsed.toOfficeDocument()
        val normal = doc.styles.paragraphStyles["Normal"] ?: doc.styles.defaultParagraphStyle
        assertNotNull("Sample-1.docx must resolve Normal style", normal)
        assertEquals(11f, normal!!.fontSizeSp, 0.5f)
        assertEquals(LayoutUnits.twipsToUnits(160), normal.spaceAfterUnits, 0.5f)
        assertEquals(1.15f, normal.lineHeightFactor, 0.05f)
    }

    @Test
    fun sample4DocxCascadeOrderFromDocDefaultsToSpecificStyles() {
        val parsed = parseOfficeDoc("Sample-4.docx")
        val doc = parsed.toOfficeDocument()
        val styles = doc.styles.paragraphStyles

        // Normal inherits docDefaults line 360 (1.5x) and firstLine 720 (48 units)
        val normal = styles["Normal"] ?: doc.styles.defaultParagraphStyle
        assertNotNull(normal)
        assertEquals(1.5f, normal!!.lineHeightFactor, 0.05f)
        assertEquals(LayoutUnits.twipsToUnits(720), normal.firstLineIndentUnits, 0.5f)

        // TidakAdaSpasi (No Spacing) overrides with line 240 (1.0x)
        val noSpacing = styles["TidakAdaSpasi"]
        assertNotNull(noSpacing)
        assertEquals(1.0f, noSpacing!!.lineHeightFactor, 0.05f)

        // Judul (Title) has sz 56 (28 pt), line 240 (1.0x), after 80 twips
        val title = styles["Judul"]
        assertNotNull(title)
        assertEquals(28f, title!!.fontSizeSp, 0.5f)
        assertEquals(1.0f, title.lineHeightFactor, 0.05f)
    }

    /**
     * Plan 8A commit 1 read the linked character styles, so the level-1 heading
     * is the 20 pt `Heading1Char` size carried over the 12 pt `w:docDefaults`
     * body size. Before commit 1 this resolved to 12 pt, which is why the DOCX
     * page counts move and the matrix is re-measured.
     */
    @Test
    fun sample6DocxLevelOneHeadingTakesItsNameConventionTwin() {
        val parsed = parseOfficeDoc("Sample-6.docx")
        val doc = parsed.toOfficeDocument()
        val heading1 = StyleResolver.resolveParagraphStyle("Judul1", doc.styles)
        assertEquals(20f, heading1.fontSizeSp, 0.5f)
        assertTrue("Sample-6 heading must be bold", heading1.isBold)
        assertEquals("0F4761", heading1.colorHex?.removePrefix("#")?.uppercase())
        assertTrue(
            "the twin must also be exposed for run resolution",
            doc.styles.characterStyles.containsKey("Heading1Char")
        )
    }

    @Test
    fun sample1DocxHeadingSizesComeFromTheLinkedCharacterStyles() {
        val doc = parseOfficeDoc("Sample-1.docx").toOfficeDocument()
        assertEquals(20f, StyleResolver.resolveParagraphStyle("Judul1", doc.styles).fontSizeSp, 0.5f)
        assertEquals(16f, StyleResolver.resolveParagraphStyle("Judul2", doc.styles).fontSizeSp, 0.5f)
        assertEquals(14f, StyleResolver.resolveParagraphStyle("Judul3", doc.styles).fontSizeSp, 0.5f)
    }

    /** Owner decision 2026-10-06 (audit-019 section 4.1): own properties beat the linked twin. */
    @Test
    fun sample2AndSample5OwnLevelOneSizeBeatsTheLinkedCharacterStyle() {
        for (name in listOf("Sample-2.docx", "Sample-5.docx")) {
            val doc = parseOfficeDoc(name).toOfficeDocument()
            val heading1 = StyleResolver.resolveParagraphStyle("Judul1", doc.styles)
            assertEquals("$name own 14 pt must win", 14f, heading1.fontSizeSp, 0.5f)
            // The twin keeps its own 20 pt value for runs that name it directly.
            assertEquals(20f, doc.styles.characterStyles.getValue("Heading1Char").fontSizeSp!!, 0.5f)
        }
    }

    @Test
    fun sample4DocxExplicitLinksResolveWithTheirOwnColour() {
        val doc = parseOfficeDoc("Sample-4.docx").toOfficeDocument()
        val heading1 = StyleResolver.resolveParagraphStyle("Judul1", doc.styles)
        assertEquals(20f, heading1.fontSizeSp, 0.5f)
        assertEquals("365F91", heading1.colorHex?.removePrefix("#")?.uppercase())
    }

    @Test
    fun docxParagraphNumberingStateSurvivesParsing() {
        fun refs(name: String) = parseOfficeDoc(name).elements.mapNotNull { element ->
            when (element) {
                is OfficeDocumentElement.Paragraph -> element.numbering
                is OfficeDocumentElement.Heading -> element.numbering
                else -> null
            }
        }

        val sample6 = refs("Sample-6.docx")
        assertTrue(
            "Sample-6 headings copy numId 15 from their style chain",
            sample6.any { it.fromStyle && it.numId == 15 }
        )
        val sample2 = refs("Sample-2.docx")
        assertTrue(
            "Sample-2 carries direct numId 0 suppressions",
            sample2.any { it.suppressed && it.numId == 0 }
        )
        assertTrue(
            "Sample-2 headings also inherit numId 1 from their styles",
            sample2.any { it.fromStyle && it.numId == 1 }
        )
    }

    @Test
    fun sample3Heading1IsTwentyPtInBothFormats() {
        val parsedDocx = parseOfficeDoc("Sample-3.docx")
        val docDocx = parsedDocx.toOfficeDocument()
        val docxH1 = StyleResolver.resolveParagraphStyle("Judul1", docDocx.styles)
        assertEquals(20f, docxH1.fontSizeSp, 0.5f)

        val parsedOdt = parseOfficeDoc("Sample-3.odt")
        val docOdt = parsedOdt.toOfficeDocument()
        val odtH1 = StyleResolver.resolveParagraphStyle("Heading_20_1", docOdt.styles)
        assertEquals(20f, odtH1.fontSizeSp, 0.5f)
    }
}
