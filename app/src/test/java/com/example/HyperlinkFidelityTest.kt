package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.OfficeHeading
import com.makerandreas.papirusoffice.data.OfficeListItem
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.OfficeTextRun
import com.makerandreas.papirusoffice.data.odf.SvXMLImport
import com.makerandreas.papirusoffice.data.toOfficeDocument
import com.makerandreas.papirusoffice.data.writer.OdtDocumentParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HyperlinkFidelityTest {

    @Test
    fun textAPreservesHrefAcrossDirectCharactersNestedSpansAndTabs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val importer = SvXMLImport(context)
        val stylesXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0">
              <office:styles>
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
                xmlns:xlink="http://www.w3.org/1999/xlink">
              <office:automatic-styles/>
              <office:body>
                <office:text>
                  <text:p text:style-name="Standard">See <text:a xlink:href="https://example.org/docs">Official <text:span text:style-name="TBold">Guide</text:span><text:tab/>1</text:a> now.</text:p>
                </office:text>
              </office:body>
            </office:document-content>
        """.trimIndent()

        val parsed = importer.parseOdfXml(contentXml, "links.odt", stylesXmlContent = stylesXml)
        assertFalse(parsed.isParsingFailed)
        val doc = parsed.toOfficeDocument()
        val para = doc.body.elements.filterIsInstance<OfficeParagraph>().single()

        assertEquals("See Official Guide\t1 now.", para.text)
        assertEquals(para.text, para.runs.joinToString("") { it.text })

        val linkedRuns = para.runs.filter { it.hyperlink == "https://example.org/docs" }
        assertEquals("Official Guide\t1", linkedRuns.joinToString("") { it.text })
        val guideRun = linkedRuns.first { it.text == "Guide" }
        assertTrue("Nested TBold span inside text:a must remain bold", guideRun.isBold)
        assertTrue("Hyperlink runs must be underlined", guideRun.isUnderline)
    }

    @Test
    fun sampleOdtFixturesPreserveBodyAndTocHyperlinksWithSynchronizedRuns() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)
        val expectedMinHyperlinkElements = mapOf(
            "Sample-2.odt" to 15,
            "Sample-4.odt" to 2,
            "Sample-5.odt" to 14,
            "Sample-6.odt" to 45
        )

        for ((fileName, expectedMin) in expectedMinHyperlinkElements) {
            val file = SampleMatrix.findTestFile(fileName)
            val parsed = parser.parseDocument(file, bypassCache = true)
            assertFalse("$fileName parse failed: ${parsed.failureReason}", parsed.isParsingFailed)
            val doc = parsed.toOfficeDocument()

            var linkedElementCount = 0
            val allLinks = mutableListOf<OfficeTextRun>()
            for (el in doc.body.elements) {
                val (text, runs) = when (el) {
                    is OfficeParagraph -> el.text to el.runs
                    is OfficeHeading -> el.text to el.runs
                    is OfficeListItem -> el.text to el.runs
                    else -> continue
                }
                if (runs.isNotEmpty()) {
                    assertEquals(
                        "$fileName run concatenation must equal element text",
                        text,
                        runs.joinToString("") { it.text }
                    )
                }
                val elLinks = runs.filter { !it.hyperlink.isNullOrBlank() }
                if (elLinks.isNotEmpty()) {
                    linkedElementCount++
                    allLinks.addAll(elLinks)
                }
            }
            assertTrue(
                "$fileName expected at least $expectedMin elements with hyperlinks, got $linkedElementCount",
                linkedElementCount >= expectedMin
            )

            // Secondary OdtDocumentParser parity check.
            val writerDoc = OdtDocumentParser().parse(file)
            val writerLinks = writerDoc.body.elements.flatMap { el ->
                when (el) {
                    is OfficeParagraph -> el.runs
                    is OfficeHeading -> el.runs
                    is OfficeListItem -> el.runs
                    else -> emptyList()
                }
            }.filter { !it.hyperlink.isNullOrBlank() }
            assertTrue("$fileName OdtDocumentParser must also extract hyperlinks", writerLinks.isNotEmpty())
        }
    }
}
