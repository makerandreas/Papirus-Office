package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.DocumentIndexKind
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Plan 8B end-to-end smoke tests pinning the three user-authored shapes
 * (BAB-prefixed headings, numId=0 "no-man's-land" de-numbered headings,
 * and TOC entries with cached result text).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocxAuthoredShapeTest {

    @Test
    fun `sample 6 preface no-man's-land headings have no BAB label`() {
        // KATA PENGANTAR, DAFTAR ISI, DAFTAR PUSTAKA are Judul1 paragraphs with
        // direct w:numId=0 (the backspace de-number per Writer Guide). They
        // must render bare text without any BAB/roman/arabic label prepended.
        val parsed = parseSync()
        val headings = parsed.elements.filterIsInstance<OfficeDocumentElement.Heading>()

        for (name in listOf("KATA PENGANTAR", "DAFTAR ISI", "DAFTAR PUSTAKA")) {
            val h = headings.firstOrNull { it.text.contains(name) }
            assertNotNull("$name heading present", h)
            assertFalse("$name must NOT start with BAB (numId=0 suppresses labels)",
                h!!.text.startsWith("BAB ", ignoreCase = true))
            assertFalse("$name must not start with a decimal or roman label",
                h.text.matches(Regex("^[0-9IVXLCDM]+[ .].*")))
        }
    }

    @Test
    fun `sample 6 chapter headings receive BAB prefix and a chapter number from lvlText`() {
        val parsed = parseSync()
        val headings = parsed.elements.filterIsInstance<OfficeDocumentElement.Heading>()
        // Sample-6.docx carries exactly three paragraphs whose effective numbering is
        // numId 15 at ilvl 0, in this document order (measured from the fixture: 42
        // paragraphs inherit numId 15 through Judul1/Judul2/Judul3, three of them at
        // level 0). NumberingCounterState therefore hands them BAB 1, BAB 2 and BAB 3,
        // and asserting the ordered triple is what proves the counter advanced instead
        // of repeating one label three times.
        val numbers = mutableListOf<String>()
        for (name in listOf("PENDAHULUAN", "PEMBAHASAN", "PENUTUP")) {
            val h = headings.firstOrNull { it.text.contains(name) }
            assertNotNull("$name heading present", h)
            assertTrue("$name must start with 'BAB ' prefix read from lvlText",
                h!!.text.startsWith("BAB "))
            val digits = h.text.removePrefix("BAB ").takeWhile { it.isDigit() }
            assertTrue("$name must carry a chapter number after 'BAB ', got '${h.text}'",
                digits.isNotEmpty())
            numbers.add(digits)
        }
        assertEquals("the three chapters number 1, 2, 3 in document order",
            listOf("1", "2", "3"), numbers)
    }

    @Test
    fun `sample 6 TOC entries are cached result text without field instruction leakage`() {
        val parsed = parseSync()
        val toc = parsed.authoredIndexes.firstOrNull { it.kind == DocumentIndexKind.TABLE_OF_CONTENT }
        assertNotNull("TOC authored index present", toc)
        // Stronger than isNotEmpty(): the cached entry text in the fixture carries the
        // BAB chapter labels, so a TOC parsed as empty or as bare page numbers fails
        // here. The per-level counts are deliberately not pinned: which level an entry
        // lands on is a property of the authoredIndexes model, and pinning a number
        // this sandbox cannot execute would be a guess dressed as an assertion.
        assertTrue("TOC has entries", toc!!.entries.isNotEmpty())
        assertTrue("TOC entries keep the cached BAB chapter labels",
            toc.entries.any { "BAB" in it.text })
        // No field instruction leakage.
        assertFalse("PAGEREF must not leak", parsed.plainText.contains("PAGEREF"))
        assertFalse("TOC \\o instr must not leak",
            parsed.plainText.contains("TOC \\o") || parsed.plainText.contains("HYPERLINK \\l"))
    }

    private fun parseSync() = runBlocking {
        OfficeDocumentParser(ApplicationProvider.getApplicationContext<Context>())
            .parseDocument(SampleMatrix.findTestFile("Sample-6.docx"), bypassCache = true)
    }
}
