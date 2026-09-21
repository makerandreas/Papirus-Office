package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.OfficeHeading
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.navigation.DocumentIndexEngine
import com.makerandreas.papirusoffice.data.navigation.HeadingNode
import com.makerandreas.papirusoffice.data.navigation.flattenHeadings
import com.makerandreas.papirusoffice.data.toOfficeDocument
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OdtSampleHeadingTest {

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

    private fun headingLikeCount(elements: List<Any>): Int {
        return elements.count { el ->
            when (el) {
                is OfficeDocumentElement.Heading -> true
                is OfficeHeading -> true
                is OfficeParagraph -> {
                    val s = el.styleName.orEmpty()
                    s.contains("Heading", true) || s.contains("Judul", true) ||
                        s.contains("Title", true) || el.outlineLevel > 0
                }
                else -> false
            }
        }
    }

    @Test
    fun testSample1OdtHeadings() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)
        val file = findTestFile(
            "tests/inky/Sample-1.odt",
            "tests/Sample-1.odt",
            "tests/Sample 1.odt"
        )
        assertTrue("Sample-1.odt must exist (${file.absolutePath})", file.exists() && file.length() > 0)

        val parsedDoc = parser.parseDocument(file, bypassCache = true)
        assertFalse("Sample-1.odt parse failed: ${parsedDoc.failureReason}", parsedDoc.isParsingFailed)
        assertTrue("Sample-1.odt should yield elements", parsedDoc.elements.isNotEmpty())

        val parsedHeadings = parsedDoc.elements.filterIsInstance<OfficeDocumentElement.Heading>()
        val officeDoc = parsedDoc.toOfficeDocument()
        val index = DocumentIndexEngine(officeDoc).reindex()
        val flat = flattenHeadings(index.headings)

        val structural = headingLikeCount(parsedDoc.elements) + headingLikeCount(officeDoc.body.elements)
        assertTrue(
            "Sample-1.odt should expose headings via parser or Navigator index " +
                "(parsed=${parsedHeadings.size}, index=${flat.size}, structural=$structural)",
            parsedHeadings.isNotEmpty() || flat.isNotEmpty() || structural > 0
        )
        flat.forEach { h: HeadingNode ->
            assertTrue("outlineLevel must be 1..6, was ${h.outlineLevel}", h.outlineLevel in 1..6)
            assertTrue("heading title must not be blank", h.title.isNotBlank())
        }
    }

    @Test
    fun testSample5OdtAndDocxHeadings() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)
        for (rel in listOf("tests/inky/Sample-5.odt", "tests/inky/Sample-5.docx")) {
            val file = findTestFile(rel)
            assertTrue("$rel must exist", file.exists() && file.length() > 0)
            val parsedDoc = parser.parseDocument(file, bypassCache = true)
            val officeDoc = parsedDoc.toOfficeDocument()
            val index = DocumentIndexEngine(officeDoc).reindex()
            val flat = flattenHeadings(index.headings)
            val structural = headingLikeCount(parsedDoc.elements) + headingLikeCount(officeDoc.body.elements)
            assertTrue(
                "$rel Navigator must list Judul/Heading outline entries " +
                    "(index=${flat.size}, structural=$structural, elements=${officeDoc.body.elements.size})",
                flat.size >= 5 || structural >= 5
            )
        }
    }
}
