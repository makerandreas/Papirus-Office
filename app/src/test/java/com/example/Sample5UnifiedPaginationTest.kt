package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.LayoutEngine
import com.makerandreas.papirusoffice.data.OfficeDocElement
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.OfficeImage
import com.makerandreas.papirusoffice.data.PageStyleSpec
import com.makerandreas.papirusoffice.data.toOfficeDocument
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Guards the unified Viewer/Editor stack (audit-003 P0-1, PR B1): both modes
 * paginate through LayoutEngine over the parsed document, so Sample-5's page
 * count must converge toward the 18 pages LibreOffice reports instead of the
 * historical 61/73. Window stays loose until PR C's metric work tightens it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Sample5UnifiedPaginationTest {

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

    private fun parseOfficeDoc(name: String) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)
        val file = findTestFile("tests/inky/$name")
        assertTrue("$name must exist (${file.absolutePath})", file.exists() && file.length() > 0)
        val parsed = parser.parseDocument(file, bypassCache = true)
        assertFalse("$name parse failed: ${parsed.failureReason}", parsed.isParsingFailed)
        parsed
    }

    @Test
    fun sample5OdtPageCountConvergesUnderUnifiedPagination() {
        val parsed = parseOfficeDoc("Sample-5.odt")
        val officeDoc = parsed.toOfficeDocument()
        val spec = officeDoc.styles.defaultPageStyle ?: PageStyleSpec.FALLBACK
        val layout = LayoutEngine(spec).performLayout(officeDoc)
        assertTrue(
            "Sample-5.odt laid out to ${layout.pages.size} pages; target is 18 ± 3, interim window 12..30", 
            layout.pages.size in 12..30
        )
    }

    @Test
    fun sample5DocxPageCountConvergesUnderUnifiedPagination() {
        val parsed = parseOfficeDoc("Sample-5.docx")
        val officeDoc = parsed.toOfficeDocument()
        val spec = officeDoc.styles.defaultPageStyle ?: PageStyleSpec.FALLBACK
        val layout = LayoutEngine(spec).performLayout(officeDoc)
        assertTrue(
            "Sample-5.docx laid out to ${layout.pages.size} pages; target is 18 ± 3, interim window 12..30",
            layout.pages.size in 12..30
        )
    }

    @Test
    fun sample5ImagesSurviveParseIntoLayoutElements() {
        val parsed = parseOfficeDoc("Sample-5.odt")
        val officeDoc = parsed.toOfficeDocument()
        val images = officeDoc.body.elements.filter {
            it is OfficeImage || it is OfficeDocElement.ImageElement
        }
        assertTrue("Sample-5.odt must expose image elements to the shared element loop", images.isNotEmpty())

        val layout = LayoutEngine(officeDoc.styles.defaultPageStyle ?: PageStyleSpec.FALLBACK)
            .performLayout(officeDoc)
        val laidOutImages = layout.pages.flatMap { it.elements }.count {
            it.element is OfficeImage || it.element is OfficeDocElement.ImageElement
        }
        assertTrue("images must be placed on pages, not dropped by pagination", laidOutImages > 0)
        assertTrue(
            "extracted image payloads must exist for rendered placeholders",
            parsed.extractedImages.isNotEmpty()
        )
    }
}
