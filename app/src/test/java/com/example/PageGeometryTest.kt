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
     * The numbers below are the *declared* `fo:margin-*` values. Sample-5.odt
     * (OnlyOffice export) carries Word's 1 in top margin as a 2.54 cm
     * fixed-height header on the layouts the body uses (Mpm2..6), not as a
     * margin, so the declared 0 cm top margin is not a 0 cm body top
     * (audit-007 finding B). The header/footer assertions pin that.
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
        // 21cm x 29.71cm at 96 units/inch; declared margins top 0cm, bottom 1cm, sides 2.54cm
        assertEquals(793.7f, spec.widthDp, 1.5f)
        assertEquals(1122.9f, spec.heightDp, 1.5f)
        assertEquals(0f, spec.marginTopDp, 0.5f)
        assertEquals(37.8f, spec.marginBottomDp, 1f)
        assertEquals(96.0f, spec.marginStartDp, 0.5f)
        assertEquals(96.0f, spec.marginEndDp, 0.5f)
        // "Standard" master page resolves to Mpm1, not the trailing MasterPage* layouts
        assertEquals("Mpm1", spec.name)
        assertTrue("page-layouts must be addressable by name", parsedDoc.styles.pageStyles.containsKey("Mpm1"))
        // Mpm1 has no header and a 1 cm minimum-height footer.
        assertEquals(0f, spec.headerHeightDp, 0.01f)
        assertEquals(37.8f, spec.footerHeightDp, 1f)

        // The body starts on MasterPage2 -> Mpm2, whose header is svg:height="2.54cm"
        // (ODF 1.4 Part 3 §20.407.2) over fo:min-height="0cm": body top is 1 in.
        assertEquals("MasterPage2", parsedDoc.styles.firstMasterPageName)
        assertEquals("Mpm2", parsedDoc.styles.masterPages["MasterPage2"])
        val bodyLayout = parsedDoc.styles.pageStyleForMaster("MasterPage2")
        assertNotNull("Mpm2 must be readable through its master page", bodyLayout)
        bodyLayout!!
        assertEquals(96f, bodyLayout.headerHeightDp, 0.5f)
        assertEquals(37.8f, bodyLayout.footerHeightDp, 1f)
        assertEquals(0f, bodyLayout.marginTopDp, 0.5f)
        assertEquals(96f, bodyLayout.bodyTopDp, 0.5f)
        assertEquals(1122.9f - 37.8f - 37.8f, bodyLayout.bodyBottomDp, 2f)
        // Plan 5A: the paginator still flows from the declared margin; PR 16a moves it to bodyTopDp.
        assertEquals(spec.marginTopDp, 0f, 0.01f)
    }

    @Test
    fun sample1OdtFooterMinHeightBecomesFooterHeight() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parsedDoc = OfficeDocumentParser(context).parseDocument(findTestFile("tests/inky/Sample-1.odt"), bypassCache = true)
        assertFalse(parsedDoc.isParsingFailed)
        val spec = parsedDoc.styles.defaultPageStyle
        assertNotNull(spec)
        spec!!
        // LibreOffice 7.0.4: 1in top, 0.3937in bottom margin plus a 0.6063in footer = 1in body bottom.
        assertEquals("Mpm1", spec.name)
        assertEquals(96f, spec.marginTopDp, 0.5f)
        assertEquals(37.8f, spec.marginBottomDp, 0.5f)
        assertEquals(0f, spec.headerHeightDp, 0.01f)
        assertEquals(58.2f, spec.footerHeightDp, 0.5f)
        assertEquals(spec.heightDp - 96f, spec.bodyBottomDp, 0.6f)
        assertEquals("Standard", parsedDoc.styles.firstMasterPageName)
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
