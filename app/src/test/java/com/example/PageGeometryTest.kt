package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.DocumentBody
import com.makerandreas.papirusoffice.data.DocumentMetadata
import com.makerandreas.papirusoffice.data.DocumentStyles
import com.makerandreas.papirusoffice.data.LayoutEngine
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.PageStyleSpec
import com.makerandreas.papirusoffice.data.odf.SvXMLImport
import com.makerandreas.papirusoffice.data.util.OdfLength
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PageGeometryTest {

    private fun findTestFile(vararg candidates: String): File {
        val bases = listOf("", "../", "../../")
        for (candidate in candidates) {
            for (base in bases) {
                val f = File(base + candidate)
                if (f.exists() && f.length() > 0) return f
            }
        }
        return File(candidates.first())
    }

    /**
     * Collabora 26.04 writes one page layout into every regenerated `.odt`
     * (audit-008 §2): A4, 2.54 cm top and sides, 1 cm bottom margin plus a
     * footer of fo:min-height 1.54 cm, so the body bottom sits 2.54 cm above
     * the page edge, like the DOCX twin.
     */
    @Test
    fun sample5OdtDeclaresA4WithDeclaredMargins() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)
        val file = findTestFile("tests/inky/Sample-5.odt")
        assertTrue("Sample-5.odt must exist (${file.absolutePath})", file.exists() && file.length() > 0)

        val parsedDoc = parser.parseDocument(file, bypassCache = true)
        assertFalse("Sample-5.odt parse failed: ${parsedDoc.failureReason}", parsedDoc.isParsingFailed)

        val spec = parsedDoc.styles.defaultPageStyle
        assertNotNull("Sample-5.odt declares style:page-layout, expected a resolved page box", spec)
        spec!!
        // 21.001cm x 29.7cm at 96 units/inch; margins top 2.54cm, bottom 1cm, sides 2.54cm
        assertEquals(793.7f, spec.widthDp, 1.5f)
        assertEquals(1122.5f, spec.heightDp, 1.5f)
        assertEquals(96f, spec.marginTopDp, 0.5f)
        assertEquals(37.8f, spec.marginBottomDp, 1f)
        assertEquals(96.0f, spec.marginStartDp, 0.5f)
        assertEquals(96.0f, spec.marginEndDp, 0.5f)
        assertEquals("Mpm1", spec.name)
        assertTrue("page-layouts must be addressable by name", parsedDoc.styles.pageStyles.containsKey("Mpm1"))
        // No header; the footer's 1.54 cm minimum height completes the 2.54 cm bottom.
        assertEquals(0f, spec.headerHeightDp, 0.01f)
        assertEquals(58.2f, spec.footerHeightDp, 0.5f)
        assertEquals(96f, spec.bodyTopDp, 0.5f)
        assertEquals(spec.heightDp - 96f, spec.bodyBottomDp, 0.6f)

        // The first body paragraph's automatic style names master page Standard -> Mpm1.
        assertEquals("Standard", parsedDoc.styles.firstMasterPageName)
        assertEquals("Mpm1", parsedDoc.styles.masterPages["Standard"])
        assertEquals("Mpm2", parsedDoc.styles.masterPages["Converted1"])
    }

    /**
     * No regenerated fixture carries a fixed-height header any more (the
     * OnlyOffice exports of audit-007 finding B were replaced), so the
     * svg:height-over-fo:min-height rule (ODF 1.4 Part 3 §20.407.2, §20.212)
     * is pinned on a minimal styles.xml instead.
     */
    @Test
    fun fixedHeaderHeightWinsOverMinHeight() {
        val stylesXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
                xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0">
              <office:automatic-styles>
                <style:page-layout style:name="Mpm1">
                  <style:page-layout-properties fo:page-width="21cm" fo:page-height="29.7cm"
                      fo:margin-top="0cm" fo:margin-bottom="1cm" fo:margin-left="2.54cm" fo:margin-right="2.54cm"/>
                  <style:header-style>
                    <style:header-footer-properties svg:height="2.54cm" fo:min-height="0cm"/>
                  </style:header-style>
                  <style:footer-style>
                    <style:header-footer-properties fo:min-height="1cm"/>
                  </style:footer-style>
                </style:page-layout>
              </office:automatic-styles>
              <office:master-styles>
                <style:master-page style:name="Standard" style:page-layout-name="Mpm1"/>
              </office:master-styles>
            </office:document-styles>
        """.trimIndent()
        val contentXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
              <office:body><office:text><text:p>body</text:p></office:text></office:body>
            </office:document-content>
        """.trimIndent()
        val parsed = SvXMLImport().parseOdfXml(contentXml, "fixed-header.odt", stylesXmlContent = stylesXml)
        assertFalse(parsed.isParsingFailed)
        val spec = parsed.styles.pageStyleForMaster("Standard")
        assertNotNull("Mpm1 must be readable through its master page", spec)
        spec!!
        assertEquals(0f, spec.marginTopDp, 0.5f)
        assertEquals(96f, spec.headerHeightDp, 0.5f)
        assertEquals(37.8f, spec.footerHeightDp, 1f)
        assertEquals(96f, spec.bodyTopDp, 0.5f)
        assertEquals(spec.heightDp - 37.8f - 37.8f, spec.bodyBottomDp, 2f)
    }

    @Test
    fun sample1OdtFooterMinHeightBecomesFooterHeight() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parsedDoc = OfficeDocumentParser(context).parseDocument(findTestFile("tests/inky/Sample-1.odt"), bypassCache = true)
        assertFalse(parsedDoc.isParsingFailed)
        val spec = parsedDoc.styles.defaultPageStyle
        assertNotNull(spec)
        spec!!
        // Collabora 26.04: 2.54cm top, 1cm bottom margin plus a 1.54cm footer = 2.54cm body bottom.
        assertEquals("Mpm1", spec.name)
        assertEquals(96f, spec.marginTopDp, 0.5f)
        assertEquals(37.8f, spec.marginBottomDp, 0.5f)
        assertEquals(0f, spec.headerHeightDp, 0.01f)
        assertEquals(58.2f, spec.footerHeightDp, 0.5f)
        assertEquals(spec.heightDp - 96f, spec.bodyBottomDp, 0.6f)
        // The first paragraph names no master page, which ODF resolves to Standard.
        assertEquals(null, parsedDoc.styles.firstMasterPageName)
    }

    @Test
    fun docxPageSpecKeepsHeaderAndFooterInsideTheMargins() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parsedDoc = OfficeDocumentParser(context).parseDocument(findTestFile("tests/inky/Sample-5.docx"), bypassCache = true)
        assertFalse(parsedDoc.isParsingFailed)
        val spec = parsedDoc.styles.defaultPageStyle!!
        // WordprocessingML: header/footer live inside w:pgMar, so the body rectangle is the margin rectangle.
        assertEquals(0f, spec.headerHeightDp, 0.01f)
        assertEquals(0f, spec.footerHeightDp, 0.01f)
        assertEquals(spec.marginTopDp, spec.bodyTopDp, 0.01f)
        assertEquals(spec.contentBottomDp, spec.bodyBottomDp, 0.01f)
    }

    @Test
    fun sample5DocxPageBoxMatchesA4AndOneInchMargins() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)
        val file = findTestFile("tests/inky/Sample-5.docx")
        assertTrue("Sample-5.docx must exist (${file.absolutePath})", file.exists() && file.length() > 0)

        val parsedDoc = parser.parseDocument(file, bypassCache = true)
        assertFalse("Sample-5.docx parse failed: ${parsedDoc.failureReason}", parsedDoc.isParsingFailed)

        val spec = parsedDoc.styles.defaultPageStyle
        assertNotNull("w:sectPr declares w:pgSz, expected a resolved page box", spec)
        spec!!
        // 11907 x 16840 twips at 96/inch; w:pgMar 1440 on all sides = 1in = 96 units
        assertEquals(793.8f, spec.widthDp, 1.5f)
        assertEquals(1122.7f, spec.heightDp, 1.5f)
        assertEquals(96f, spec.marginTopDp, 0.5f)
        assertEquals(96f, spec.marginBottomDp, 0.5f)
        assertEquals(96f, spec.marginStartDp, 0.5f)
        assertEquals(96f, spec.marginEndDp, 0.5f)
    }

    @Test
    fun layoutEnginePaginatesInsideDeclaredPageBox() {
        val spec = PageStyleSpec(
            name = "test600x800",
            widthDp = 600f,
            heightDp = 800f,
            marginTopDp = 20f,
            marginBottomDp = 30f,
            marginStartDp = 40f,
            marginEndDp = 60f
        )
        val longText = "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore. ".repeat(6)
        val document = OfficeDocument(
            metadata = DocumentMetadata(title = "geometry"),
            styles = DocumentStyles(defaultPageStyle = spec),
            body = DocumentBody(
                elements = List(40) { OfficeParagraph(text = if (it == 0) longText else "Short paragraph $it") }
            )
        )

        val result = LayoutEngine(spec).performLayout(document)

        assertTrue("40 paragraphs at a 800-unit page must overflow to a second page", result.pages.size >= 2)
        val first = result.pages.first()
        assertEquals(600f, first.widthDp, 0.01f)
        assertEquals(800f, first.heightDp, 0.01f)
        val firstElement = first.elements.first()
        assertEquals(40f, firstElement.bounds.left, 0.01f)
        assertEquals(20f, firstElement.bounds.top, 0.01f)
        assertTrue(
            "content must stop at the right margin (600-60)",
            first.elements.all { it.bounds.right <= 540f + 0.01f }
        )
        val secondFirst = result.pages[1].elements.first()
        assertEquals("page break restarts flow at the top margin", 20f, secondFirst.bounds.top, 0.01f)
    }

    @Test
    fun layoutEngineKeepsLetterFallbackWithoutDeclaredGeometry() {
        val document = OfficeDocument(
            metadata = DocumentMetadata(title = "fallback"),
            body = DocumentBody(elements = listOf(OfficeParagraph(text = "Just one paragraph to place.")))
        )

        val result = LayoutEngine().performLayout(document)

        val page = result.pages.first()
        assertEquals(816f, page.widthDp, 0.01f)
        assertEquals(1056f, page.heightDp, 0.01f)
        assertEquals(40f, page.elements.first().bounds.left, 0.01f)
        assertEquals(50f, page.elements.first().bounds.top, 0.01f)
    }

    @Test
    fun odfLengthsConvertToNinetySixUnitsPerInch() {
        assertEquals(96f, OdfLength.toLayoutUnits("2.54cm"), 0.1f)
        assertEquals(96f, OdfLength.toLayoutUnits("1in"), 0.01f)
        assertEquals(96f, OdfLength.toLayoutUnits("72pt"), 0.1f)
        assertEquals(96f, OdfLength.toLayoutUnits("6pc"), 0.1f)
        assertEquals(10f, OdfLength.toLayoutUnits("10px"), 0.01f)
        assertEquals(96f, OdfLength.toLayoutUnits("96"), 0.01f)
        assertEquals(5f, OdfLength.toLayoutUnits(null, fallback = 5f), 0.01f)
        assertEquals(5f, OdfLength.toLayoutUnits("abc", fallback = 5f), 0.01f)
        assertEquals(96f, OdfLength.twipsToLayoutUnits(1440), 0.01f)
        assertEquals(793.8f, OdfLength.twipsToLayoutUnits(11907), 0.1f)
    }
}
