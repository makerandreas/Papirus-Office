package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.NumberingCounterState
import com.makerandreas.papirusoffice.data.NumberingLevelSpec
import com.makerandreas.papirusoffice.data.NumberingSpec
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.OfficeHeading
import com.makerandreas.papirusoffice.data.OfficeListItem
import com.makerandreas.papirusoffice.data.odf.SvXMLImport
import com.makerandreas.papirusoffice.data.toOfficeDocument
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
class OdtListNumberingTest {

    /** Zero-width joiners and authored line breaks make raw heading text unreadable; compare normalised. */
    private fun normalize(text: String): String =
        text.replace("\u200b", "").replace(Regex("\\s+"), " ").trim()

    @Test
    fun counterStateFormatsRomanArabicAlphaAndResetsDeeperLevels() {
        val spec = NumberingSpec(
            name = "Makalah_20_Default",
            levels = mapOf(
                1 to NumberingLevelSpec(level = 1, numFormat = "I", numPrefix = "BAB ", numSuffix = "", displayLevels = 1),
                2 to NumberingLevelSpec(level = 2, numFormat = "1", numPrefix = "", numSuffix = "", displayLevels = 2),
                3 to NumberingLevelSpec(level = 3, numFormat = "1", numPrefix = "", numSuffix = "", displayLevels = 3),
                4 to NumberingLevelSpec(level = 4, numFormat = "a", numPrefix = "", numSuffix = ")", displayLevels = 1)
            )
        )
        val counter = NumberingCounterState()
        assertEquals("BAB I", counter.advance(spec, 1))
        assertEquals("1.1", counter.advance(spec, 2))
        assertEquals("1.2", counter.advance(spec, 2))
        assertEquals("1.2.1", counter.advance(spec, 3))
        assertEquals("a)", counter.advance(spec, 4))
        assertEquals("b)", counter.advance(spec, 4))

        // Advancing level 1 to chapter 2 resets levels 2..4.
        assertEquals("BAB II", counter.advance(spec, 1))
        assertEquals("2.1", counter.advance(spec, 2))
        assertEquals("2.1.1", counter.advance(spec, 3))
        assertEquals("2.1.2", counter.advance(spec, 3))
        assertEquals("a)", counter.advance(spec, 4))
    }

    @Test
    fun paragraphAutoStyleListStyleNameResolvesHierarchicalNumberingAcrossLists() {
        val importer = SvXMLImport()
        val stylesXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
              <office:styles>
                <style:style style:name="Heading_20_1" style:family="paragraph" style:default-outline-level="1"/>
                <style:style style:name="Heading_20_2" style:family="paragraph" style:default-outline-level="2"/>
                <style:style style:name="Heading_20_3" style:family="paragraph" style:default-outline-level="3"/>
                <text:list-style style:name="Makalah_20_Default">
                  <text:list-level-style-number text:level="1" style:num-prefix="BAB " style:num-format="I"/>
                  <text:list-level-style-number text:level="2" text:display-levels="2" style:num-format="1"/>
                  <text:list-level-style-number text:level="3" text:display-levels="3" style:num-format="1"/>
                </text:list-style>
              </office:styles>
            </office:document-styles>
        """.trimIndent()

        val contentXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
              <office:automatic-styles>
                <style:style style:name="P_Unnumbered" style:family="paragraph"
                    style:parent-style-name="Heading_20_1" style:list-style-name=""/>
                <style:style style:name="P_H1" style:family="paragraph"
                    style:parent-style-name="Heading_20_1" style:list-style-name="Makalah_20_Default"/>
                <style:style style:name="P_H2" style:family="paragraph"
                    style:parent-style-name="Heading_20_2" style:list-style-name="Makalah_20_Default"/>
                <style:style style:name="P_H3" style:family="paragraph"
                    style:parent-style-name="Heading_20_3" style:list-style-name="Makalah_20_Default"/>
              </office:automatic-styles>
              <office:body>
                <office:text>
                  <text:h text:style-name="P_Unnumbered" text:outline-level="1">KATA PENGANTAR</text:h>
                  <text:list>
                    <text:list-item>
                      <text:p text:style-name="P_H1">PENDAHULUAN</text:p>
                    </text:list-item>
                  </text:list>
                  <text:list>
                    <text:list-item>
                      <text:list>
                        <text:list-item>
                          <text:p text:style-name="P_H2">Latar Belakang</text:p>
                        </text:list-item>
                      </text:list>
                    </text:list-item>
                  </text:list>
                  <text:list>
                    <text:list-item>
                      <text:p text:style-name="P_H1">PEMBAHASAN</text:p>
                    </text:list-item>
                  </text:list>
                  <text:list>
                    <text:list-item>
                      <text:list>
                        <text:list-item>
                          <text:p text:style-name="P_H2">Pengertian Talenta</text:p>
                        </text:list-item>
                        <text:list-item>
                          <text:list>
                            <text:list-item>
                              <text:p text:style-name="P_H3">Definisi dan Etimologi</text:p>
                            </text:list-item>
                          </text:list>
                        </text:list-item>
                      </text:list>
                    </text:list-item>
                  </text:list>
                </office:text>
              </office:body>
            </office:document-content>
        """.trimIndent()

        val parsed = importer.parseOdfXml(contentXml, "makalah.odt", stylesXmlContent = stylesXml)
        assertFalse(parsed.isParsingFailed)
        val headings = parsed.elements.filterIsInstance<OfficeDocumentElement.Heading>()
        assertEquals(
            listOf(
                "KATA PENGANTAR",
                "BAB I PENDAHULUAN",
                "1.1 Latar Belakang",
                "BAB II PEMBAHASAN",
                "2.1 Pengertian Talenta",
                "2.1.1 Definisi dan Etimologi"
            ),
            headings.map { normalize(it.text) }
        )
    }

    @Test
    fun sample6OdtOutlineNumberingMatchesAuthoredSpec() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = SampleMatrix.findTestFile("Sample-6.odt")
        val parsed = OfficeDocumentParser(context).parseDocument(file, bypassCache = true)
        assertFalse("Sample-6.odt parse failed: ${parsed.failureReason}", parsed.isParsingFailed)
        val doc = parsed.toOfficeDocument()

        val headingTexts = doc.body.elements.filterIsInstance<OfficeHeading>().map { normalize(it.text) }

        // Front/back matter with style:list-style-name="" must stay unnumbered.
        assertTrue("KATA PENGANTAR must stay unnumbered: $headingTexts", headingTexts.contains("KATA PENGANTAR"))
        assertTrue("DAFTAR ISI must stay unnumbered: $headingTexts", headingTexts.contains("DAFTAR ISI"))
        assertTrue("DAFTAR PUSTAKA must stay unnumbered: $headingTexts", headingTexts.contains("DAFTAR PUSTAKA"))

        // Chapter and multi-level sub-headings from <text:outline-style>.
        assertTrue("Expected 'BAB 1 PENDAHULUAN' in $headingTexts", headingTexts.contains("BAB 1 PENDAHULUAN"))
        assertTrue("Expected '1.1 Latar Belakang' in $headingTexts", headingTexts.contains("1.1 Latar Belakang"))
        assertTrue("Expected '1.2 Rumusan Masalah' in $headingTexts", headingTexts.contains("1.2 Rumusan Masalah"))
        assertTrue("Expected 'BAB 2 PEMBAHASAN' in $headingTexts", headingTexts.contains("BAB 2 PEMBAHASAN"))
        assertTrue(
            "Expected '2.1.1 Definisi dan Etimologi Talenta' in $headingTexts",
            headingTexts.contains("2.1.1 Definisi dan Etimologi Talenta")
        )
        assertTrue("Expected 'BAB 3 PENUTUP' in $headingTexts", headingTexts.contains("BAB 3 PENUTUP"))
        assertTrue("Expected '3.1 Kesimpulan' in $headingTexts", headingTexts.contains("3.1 Kesimpulan"))
        assertTrue("Expected '3.2 Saran' in $headingTexts", headingTexts.contains("3.2 Saran"))
        assertTrue(
            "Expected '3.2.1 Bagi Siswa dan Pemuda Kristen' in $headingTexts",
            headingTexts.contains("3.2.1 Bagi Siswa dan Pemuda Kristen")
        )
    }

    @Test
    fun sample6OdtListItemsCarryOrderedAndBulletLabelsAtTheLevelFontSize() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = SampleMatrix.findTestFile("Sample-6.odt")
        val parsed = OfficeDocumentParser(context).parseDocument(file, bypassCache = true)
        val doc = parsed.toOfficeDocument()
        val listItems = doc.body.elements.filterIsInstance<OfficeListItem>()
        assertTrue("Sample-6.odt must produce OfficeListItem elements", listItems.size >= 40)

        // Ordered list items in Rumusan Masalah (1..4) and Tujuan Penulisan (1..).
        // The label keeps the separator space the renderer adds, so compare trimmed.
        val orderedBullets = listItems.filter { it.isOrdered }.map { it.bullet }
        val orderedLabels = orderedBullets.map { it.trim() }
        assertTrue(
            "Expected arabic ordered labels 1. .. 4. in $orderedBullets",
            orderedLabels.containsAll(listOf("1.", "2.", "3.", "4."))
        )
        // Sample-6 authors arabic labels in every ordered list; several declare no
        // num-suffix, so the bare form is authored too and must survive.
        assertTrue("Expected a bare arabic label ('1') in $orderedBullets", orderedLabels.contains("1"))
        assertTrue("Counters must pass nine (expected '10.'): $orderedBullets", orderedLabels.contains("10."))

        // Bullet items use the middle dot without doubling the suffix. The bullet
        // level's own <style:text-properties> declares the label font family (Symbol,
        // the authored bullet-glyph face); the numbered levels' ListLabel_* text
        // styles declare Aptos. Neither declares a size, so the label-font fact is a
        // family, not a point size.
        val bulletItems = listItems.filter { !it.isOrdered }
        assertTrue("Expected middle-dot bullet items in Sample-6.odt", bulletItems.isNotEmpty())
        assertTrue(
            "Bullet glyph must not double suffix: ${bulletItems.map { it.bullet }.distinct()}",
            bulletItems.none { it.bullet.contains("\u00b7\u00b7") }
        )
        val bulletLabelFamilies = bulletItems.map { it.labelFontFamily }.distinct()
        assertTrue(
            "Sample-6 bullet levels declare the Symbol label face: $bulletLabelFamilies",
            bulletLabelFamilies == listOf("Symbol")
        )
    }

    @Test
    fun sample4OdtListWrappedHeadingsFormatAlphaLabelsWithoutAdvancingOuterWrapper() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = SampleMatrix.findTestFile("Sample-4.odt")
        val parsed = OfficeDocumentParser(context).parseDocument(file, bypassCache = true)
        assertFalse(parsed.isParsingFailed)
        val doc = parsed.toOfficeDocument()
        val headingTexts = doc.body.elements.filterIsInstance<OfficeHeading>().map { normalize(it.text) }

        assertTrue("Expected 'A. Latar Belakang' in $headingTexts", headingTexts.contains("A. Latar Belakang"))
        assertTrue("Expected 'B. Rumusan Masalah' in $headingTexts", headingTexts.contains("B. Rumusan Masalah"))
        assertTrue("Expected 'C. Tujuan' in $headingTexts", headingTexts.contains("C. Tujuan"))
        assertTrue(
            "Expected 'A. Konsep Dasar Teori Kultivasi' in $headingTexts",
            headingTexts.contains("A. Konsep Dasar Teori Kultivasi")
        )
        assertTrue(
            "Expected 'B. Temuan Utama Penelitian Gerbner' in $headingTexts",
            headingTexts.contains("B. Temuan Utama Penelitian Gerbner")
        )
        assertTrue("Expected 'A. Kesimpulan' in $headingTexts", headingTexts.contains("A. Kesimpulan"))
        assertTrue("Expected 'B. Saran' in $headingTexts", headingTexts.contains("B. Saran"))
    }
}
