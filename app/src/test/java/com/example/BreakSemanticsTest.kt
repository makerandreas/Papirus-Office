package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Plan 5d break semantics test.
 *
 * Verifies that:
 * 1. Ephemeral soft breaks (w:lastRenderedPageBreak, text:soft-page-break) are NOT page breaks.
 * 2. Editable plain text contains zero page break marker artifacts (plan-03 3.17).
 * 3. Real authored breaks (w:br type=page, intermediate w:sectPr, fo:break-before=page)
 *    are accurately parsed and preserved in the document element list.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BreakSemanticsTest {

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
    fun noPlainTextContainsPageBreakMarkers() {
        for (fileName in SampleMatrix.fileNames) {
            val parsed = parseOfficeDoc(fileName)
            assertFalse(
                "$fileName plainText must not contain page break markers",
                parsed.plainText.contains("--- Page Break ---")
            )
        }
    }

    @Test
    fun odtAuthoredBreaksMatchInventory() {
        // Sample-1: 0, Sample-2: 2, Sample-3: 0, Sample-4: 2, Sample-5: 2, Sample-6: 2
        val expected = mapOf(
            "Sample-1.odt" to 0,
            "Sample-2.odt" to 2,
            "Sample-3.odt" to 0,
            "Sample-4.odt" to 2,
            "Sample-5.odt" to 2,
            "Sample-6.odt" to 2
        )
        for ((name, count) in expected) {
            val parsed = parseOfficeDoc(name)
            val breakCount = parsed.elements.count { it is OfficeDocumentElement.PageBreak } +
                parsed.elements.sumOf { when (it) {
                    is OfficeDocumentElement.Paragraph -> it.pageBreakOffsets.size
                    is OfficeDocumentElement.Heading -> it.pageBreakOffsets.size
                    else -> 0
                } } + parsed.sectionStarts.count { it.elementIndex > 0 && it.kind != com.makerandreas.papirusoffice.data.SectionStartKind.CONTINUOUS }
            assertEquals("$name authored break count", count, breakCount)
        }
    }

    @Test
    fun docxAuthoredBreaksMatchInventory() {
        // Sample-1: 0, Sample-2: 6, Sample-3: 0, Sample-4: 7, Sample-5: 6, Sample-6: 6
        val expected = mapOf(
            "Sample-1.docx" to 0,
            "Sample-2.docx" to 6,
            "Sample-3.docx" to 0,
            "Sample-4.docx" to 7,
            "Sample-5.docx" to 6,
            "Sample-6.docx" to 6
        )
        for ((name, count) in expected) {
            val parsed = parseOfficeDoc(name)
            val breakCount = parsed.elements.count { it is OfficeDocumentElement.PageBreak } +
                parsed.elements.sumOf { when (it) {
                    is OfficeDocumentElement.Paragraph -> it.pageBreakOffsets.size
                    is OfficeDocumentElement.Heading -> it.pageBreakOffsets.size
                    else -> 0
                } } + parsed.sectionStarts.count { it.elementIndex > 0 && it.kind != com.makerandreas.papirusoffice.data.SectionStartKind.CONTINUOUS }
            assertEquals("$name authored break count", count, breakCount)
        }
    }
}
