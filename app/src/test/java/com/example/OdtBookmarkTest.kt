package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.navigation.DocumentIndexEngine
import com.makerandreas.papirusoffice.data.navigation.NavigatorCategories
import com.makerandreas.papirusoffice.data.navigation.NavigatorCategoryAvailability
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
class OdtBookmarkTest {

    @Test
    fun bookmarksCategoryIsClassifiedAsParsedDocumentClass() {
        val cat = NavigatorCategories.of("bookmarks")
        assertEquals(NavigatorCategoryAvailability.PARSED_DOCUMENT_CLASS, cat.availability)
        assertTrue(cat.elementClasses.contains("OfficeBookmark"))
    }

    @Test
    fun allSixOdtSamplesExposeExactBookmarkCountsInDocumentAndNavigatorIndex() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)
        val expectedCounts = mapOf(
            "Sample-1.odt" to 0,
            "Sample-2.odt" to 15,
            "Sample-3.odt" to 0,
            "Sample-4.odt" to 22,
            "Sample-5.odt" to 14,
            "Sample-6.odt" to 46
        )

        for ((fileName, expected) in expectedCounts) {
            val file = SampleMatrix.findTestFile(fileName)
            val parsed = parser.parseDocument(file, bypassCache = true)
            assertFalse("$fileName parse failed: ${parsed.failureReason}", parsed.isParsingFailed)
            val doc = parsed.toOfficeDocument()
            assertEquals("$fileName OfficeDocument.bookmarks count", expected, doc.bookmarks.size)

            val index = DocumentIndexEngine(doc).reindex()
            assertEquals("$fileName Navigator index.bookmarks count", expected, index.bookmarks.size)

            val writerDoc = OdtDocumentParser().parse(file)
            assertEquals("$fileName OdtDocumentParser.bookmarks count", expected, writerDoc.bookmarks.size)
        }
    }
}
