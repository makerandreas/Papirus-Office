package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SampleFilesCompatibilityTest {

    private fun findTestFile(candidates: List<String>): File? {
        val bases = listOf("", "../", "../../")
        for (candidate in candidates) {
            for (base in bases) {
                val f = File(base + candidate)
                if (f.exists() && f.length() > 0) return f
            }
        }
        return null
    }

    @Test
    fun testSample1XlsxParsing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)
        val file = findTestFile(listOf(
            "tests/Sample-1.xlsx",
            "tests/cellina/Sample-1.xlsx",
            "tests/Sample 1.xlsx"
        ))
        assertNotNull("Sample-1.xlsx file should exist in tests directory", file)
        println("Found XLSX test file: ${file!!.absolutePath}, size: ${file.length()}")

        val parsedDoc = parser.parseDocument(file, bypassCache = true)
        assertFalse("XLSX parsing should succeed without failure: ${parsedDoc.failureReason}", parsedDoc.isParsingFailed)
        assertTrue("XLSX document should contain parsed elements", parsedDoc.elements.isNotEmpty())

        val tableElements = parsedDoc.elements.filterIsInstance<OfficeDocumentElement.Table>()
        assertTrue("XLSX should contain at least one Table element", tableElements.isNotEmpty())

        val firstTable = tableElements.first()
        assertTrue("XLSX table should contain rows", firstTable.rows.isNotEmpty())
        println("XLSX parsed table '${firstTable.name}' with ${firstTable.rows.size} rows and ${firstTable.numColumns} columns.")

        // Verify sharedStrings resolved text in rows
        val allCellTexts = firstTable.rows.flatMap { it.cells }.map { it.text }
        assertTrue("Cell texts should be extracted", allCellTexts.any { it.isNotBlank() })
        println("Sample cells: ${allCellTexts.filter { it.isNotBlank() }.take(10)}")
    }

    @Test
    fun testSample1OdsParsing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)
        val file = findTestFile(listOf(
            "tests/Sample-1.ods",
            "tests/cellina/Sample-1.ods",
            "tests/Sample 1.ods"
        ))
        assertNotNull("Sample-1.ods file should exist in tests directory", file)
        println("Found ODS test file: ${file!!.absolutePath}, size: ${file.length()}")

        val parsedDoc = parser.parseDocument(file, bypassCache = true)
        assertFalse("ODS parsing should succeed without failure: ${parsedDoc.failureReason}", parsedDoc.isParsingFailed)
        assertTrue("ODS document should contain parsed elements", parsedDoc.elements.isNotEmpty())

        val tableElements = parsedDoc.elements.filterIsInstance<OfficeDocumentElement.Table>()
        assertTrue("ODS should contain at least one Table element", tableElements.isNotEmpty())

        val firstTable = tableElements.first()
        assertTrue("ODS table should contain rows", firstTable.rows.isNotEmpty())
        println("ODS parsed table '${firstTable.name}' with ${firstTable.rows.size} rows and ${firstTable.numColumns} columns.")

        val allCellTexts = firstTable.rows.flatMap { it.cells }.map { it.text }
        assertTrue("Cell texts should be extracted in ODS", allCellTexts.any { it.isNotBlank() })
        println("Sample ODS cells: ${allCellTexts.filter { it.isNotBlank() }.take(10)}")
    }

    @Test
    fun testSample1PptxParsing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)
        val file = findTestFile(listOf(
            "tests/Sample-1.pptx",
            "tests/slidia/Sample-1.pptx",
            "tests/Sample 1.pptx"
        ))
        assertNotNull("Sample-1.pptx file should exist in tests directory", file)
        println("Found PPTX test file: ${file!!.absolutePath}, size: ${file.length()}")

        val parsedDoc = parser.parseDocument(file, bypassCache = true)
        assertFalse("PPTX parsing should succeed without failure: ${parsedDoc.failureReason}", parsedDoc.isParsingFailed)
        assertTrue("PPTX document should contain parsed elements", parsedDoc.elements.isNotEmpty())
        assertTrue("PPTX document should report slide count", parsedDoc.pageCount > 0)
        println("PPTX parsed ${parsedDoc.pageCount} slides, elements count: ${parsedDoc.elements.size}")

        val headings = parsedDoc.elements.filterIsInstance<OfficeDocumentElement.Heading>()
        assertTrue("PPTX should have slide headings", headings.isNotEmpty())
        println("First PPTX slide heading: ${headings.first().text}")
    }

    @Test
    fun testSample1OdpParsing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)
        val file = findTestFile(listOf(
            "tests/Sample-1.odp",
            "tests/slidia/Sample-1.odp",
            "tests/Sample 1.odp"
        ))
        assertNotNull("Sample-1.odp file should exist in tests directory", file)
        println("Found ODP test file: ${file!!.absolutePath}, size: ${file.length()}")

        val parsedDoc = parser.parseDocument(file, bypassCache = true)
        assertFalse("ODP parsing should succeed without failure: ${parsedDoc.failureReason}", parsedDoc.isParsingFailed)
        assertTrue("ODP document should contain parsed elements", parsedDoc.elements.isNotEmpty())
        println("ODP elements count: ${parsedDoc.elements.size}, plainText length: ${parsedDoc.plainText.length}")

        // Check that text content from slides (like FRAKTUR HUMERUS or slide titles) was parsed
        val headingsAndParagraphs = parsedDoc.elements.mapNotNull {
            when (it) {
                is OfficeDocumentElement.Heading -> it.text
                is OfficeDocumentElement.Paragraph -> it.text
                else -> null
            }
        }
        assertTrue("ODP should have text elements", headingsAndParagraphs.any { it.isNotBlank() })
        println("Sample ODP texts: ${headingsAndParagraphs.filter { it.isNotBlank() }.take(5)}")
    }
}
