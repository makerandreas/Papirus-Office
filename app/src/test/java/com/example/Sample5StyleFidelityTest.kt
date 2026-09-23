package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.DocumentStyles
import com.makerandreas.papirusoffice.data.LayoutEngine
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.OfficeHeading
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.PageStyleSpec
import com.makerandreas.papirusoffice.data.ParagraphStyle
import com.makerandreas.papirusoffice.data.StyleResolver
import com.makerandreas.papirusoffice.data.odf.SvXMLImport
import com.makerandreas.papirusoffice.data.odf.parseOdfFontSizePt
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
import java.io.File
import kotlin.math.abs

/**
 * PR C: ODF property cascade, D2 span resolution, Sample-5 heading sizes vs
 * the parsed ODT (Judul1 is 14pt, not the Word 20pt Aptos Display Char).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Sample5StyleFidelityTest {

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

    @Test
    fun fontSizePtParserKeepsPoints() {
        assertEquals(12f, parseOdfFontSizePt("12pt"))
        assertEquals(14f, parseOdfFontSizePt("14pt"))
        assertEquals(null, parseOdfFontSizePt("100%"))
        assertEquals(null, parseOdfFontSizePt(null))
    }

    @Test
    fun mappedHeadingStyleBeatsHeuristic() {
        val mapped = DocumentStyles(
            paragraphStyles = mapOf("Judul1" to ParagraphStyle("Judul1", fontSizeSp = 14f, isBold = true))
        )
        assertEquals(14f, StyleResolver.resolveParagraphStyle("Judul1", mapped).fontSizeSp)
        assertEquals(24f, StyleResolver.resolveParagraphStyle("Heading 1", DocumentStyles()).fontSizeSp)
        assertEquals(20f, StyleResolver.resolveParagraphStyle("Heading 2", DocumentStyles()).fontSizeSp)
        assertEquals(16f, StyleResolver.resolveParagraphStyle("Heading 3", DocumentStyles()).fontSizeSp)
    }

    @Test
    fun emptyTabel1SpanIsNotBoldWhileTBoldIs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val importer = SvXMLImport(context)
        val stylesXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0">
              <office:styles>
                <style:style style:name="Tabel1" style:family="text">
                  <style:text-properties/>
                </style:style>
                <style:style style:name="TBold" style:family="text">
                  <style:text-properties fo:font-weight="bold"/>
                </style:style>
              </office:styles>
            </office:document-styles>
        """.trimIndent()
        val contentXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
                xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0">
              <office:automatic-styles/>
              <office:body>
                <office:text>
                  <text:p text:style-name="Standard">
                    <text:span text:style-name="Tabel1">table</text:span>
                    <text:span text:style-name="TBold">bold</text:span>
                  </text:p>
                </office:text>
              </office:body>
            </office:document-content>
        """.trimIndent()

        val parsed = importer.parseOdfXml(contentXml, "d2.odt", stylesXmlContent = stylesXml)
        assertFalse(parsed.isParsingFailed)
        assertFalse("empty Tabel1 must not inherit bold from the letter b", importer.resolveSpanFormatting("Tabel1").isBold)
        assertTrue(importer.resolveSpanFormatting("TBold").isBold)
        val para = parsed.elements.filterIsInstance<OfficeDocumentElement.Paragraph>().single()
        val tabel = para.runs.first { it.text == "table" }
        val bold = para.runs.first { it.text == "bold" }
        assertFalse("Tabel1 run must not be bold: $tabel", tabel.isBold)
        assertTrue("TBold run must be bold: $bold", bold.isBold)
        assertEquals("Tabel1", tabel.styleName)
        val charStyles = parsed.styles.characterStyles
        assertFalse(charStyles.getValue("Tabel1").isBold)
        assertTrue(charStyles.getValue("TBold").isBold)
        assertEquals(null, charStyles.getValue("Tabel1").fontSizeSp)
    }

    @Test
    fun sample5HeadingSizesMatchParsedOdtNotWordHeuristic() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = findTestFile("tests/inky/Sample-5.odt")
        assertTrue("Sample-5.odt must exist (${file.absolutePath})", file.exists() && file.length() > 0)
        val parsed = OfficeDocumentParser(context).parseDocument(file, bypassCache = true)
        assertFalse("Sample-5.odt parse failed: ${parsed.failureReason}", parsed.isParsingFailed)
        val office = parsed.toOfficeDocument()
        val paragraphs = office.styles.paragraphStyles

        val judul1 = paragraphs["Judul1"]
        assertNotNull("Judul1 must be imported as a paragraph style", judul1)
        assertTrue(
            "Judul1 ODT size is 14pt, got ${judul1!!.fontSizeSp}",
            abs(judul1.fontSizeSp - 14f) <= 1f
        )
        assertTrue("Judul1 is bold in the ODT", judul1.isBold)

        val judul2 = paragraphs["Judul2"]
        assertNotNull("Judul2 must be imported as a paragraph style", judul2)
        assertTrue(
            "Judul2 inherits Normal 12pt, got ${judul2!!.fontSizeSp}",
            abs(judul2.fontSizeSp - 12f) <= 1f
        )
        assertTrue(
            "mapped Judul1 must beat the Heading-1 24pt heuristic",
            abs(StyleResolver.resolveParagraphStyle("Judul1", office.styles).fontSizeSp - 14f) <= 1f
        )
        assertTrue(
            "mapped Judul2 must beat the Heading-2 20pt heuristic",
            abs(StyleResolver.resolveParagraphStyle("Judul2", office.styles).fontSizeSp - 12f) <= 1f
        )

        fun parentChain(name: String?): List<String> {
            val chain = ArrayList<String>(4)
            var curr = name
            var depth = 0
            while (!curr.isNullOrBlank() && depth < 8 && chain.add(curr)) {
                curr = paragraphs[curr]?.parentStyleName
                depth++
            }
            return chain
        }
        val headingSizes = office.body.elements.mapNotNull { element ->
            val styleName = when (element) {
                is OfficeHeading -> element.styleName
                is OfficeParagraph -> element.styleName
                else -> null
            }
            if (!parentChain(styleName).any { it.contains("Judul1", ignoreCase = true) }) {
                return@mapNotNull null
            }
            StyleResolver.resolveParagraphStyle(styleName, office.styles).fontSizeSp
        }
        val headings = office.body.elements.filterIsInstance<OfficeHeading>()
        if (headingSizes.isNotEmpty()) {
            assertTrue(
                "Judul1-backed headings must stay 14±1, not 24: $headingSizes names=${headings.map { it.styleName }}",
                headingSizes.all { abs(it - 14f) <= 1f }
            )
        } else {
            assertTrue("Sample-5 must expose heading elements", headings.isNotEmpty())
            headings.forEach { heading ->
                val name = heading.styleName ?: return@forEach
                val mapped = paragraphs[name] ?: return@forEach
                val resolved = StyleResolver.resolveParagraphStyle(name, office.styles)
                assertEquals(mapped.fontSizeSp, resolved.fontSizeSp, 0.01f)
                assertTrue(
                    "mapped heading $name must not use the 24pt heuristic (got ${resolved.fontSizeSp})",
                    abs(resolved.fontSizeSp - 24f) > 1f
                )
            }
        }

        val spec = office.styles.defaultPageStyle ?: PageStyleSpec.FALLBACK
        val pages = LayoutEngine(spec).performLayout(office).pages.size
        assertTrue("Sample-5 pagination must stay in 12..30, was $pages", pages in 12..30)
    }
}
