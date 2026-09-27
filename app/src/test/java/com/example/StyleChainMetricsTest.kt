package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.LayoutUnits
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
 * 3. Sample-6 DOCX TextMaker headings: resolve to 12 pt bold.
 * 4. Sample-3 DOCX and ODT Heading 1: resolves to 20 pt.
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

    @Test
    fun sample6DocxHeadingsAreTwelvePtBold() {
        val parsed = parseOfficeDoc("Sample-6.docx")
        val doc = parsed.toOfficeDocument()
        val heading1 = StyleResolver.resolveParagraphStyle("Judul1", doc.styles)
        assertEquals(12f, heading1.fontSizeSp, 0.5f)
        assertTrue("Sample-6 heading must be bold", heading1.isBold)
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
