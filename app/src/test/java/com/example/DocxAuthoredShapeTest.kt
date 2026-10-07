package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.DocumentIndexKind
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
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
 * Plan 8B end-to-end rendering tests for the user-authored fixture shapes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocxAuthoredShapeTest {

    @Test
    fun `sample 6 no-man's-land preface headings have no BAB label`() {
        val parsed = parseSync()
        val headings = parsed.elements.filterIsInstance<OfficeDocumentElement.Heading>()
        val kata = headings.firstOrNull { it.text.contains("KATA PENGANTAR") }
        assertNotNull("KATA PENGANTAR heading present", kata)
        assertFalse("KATA PENGANTAR must NOT start with BAB (numId=0 suppresses)",
            kata!!.text.startsWith("BAB ", ignoreCase = true))
        assertFalse("KATA PENGANTAR must not start with a roman numeral",
            kata.text.matches(Regex("^[IVXLCDM]+[ .].*")))

        val daftarIsi = headings.firstOrNull { it.text.contains("DAFTAR ISI") }
        assertNotNull("DAFTAR ISI heading present", daftarIsi)
        assertFalse("DAFTAR ISI must NOT start with BAB",
            daftarIsi!!.text.startsWith("BAB ", ignoreCase = true))

        val daftarPustaka = headings.firstOrNull { it.text.contains("DAFTAR PUSTAKA") }
        assertNotNull("DAFTAR PUSTAKA heading present", daftarPustaka)
        assertFalse("DAFTAR PUSTAKA must NOT start with BAB",
            daftarPustaka!!.text.startsWith("BAB ", ignoreCase = true))
    }

    @Test
    fun `sample 6 chapter headings render BAB prefix with trailing space`() {
        // The three body chapters (PENDAHULUAN, PEMBAHASAN, PENUTUP) inherit
        // numId=15 through Judul1. The level-0 format is decimal with lvlText
        // "BAB %1" and suff="space", so labels are "BAB 1 ", "BAB 2 ", "BAB 3 "
        // (note: the saved fixture uses decimal numbering, not upperRoman;
        // the prefix "BAB " is read verbatim from lvlText). The authored
        // w:br between the numeral and the title text is preserved as '\n'.
        val parsed = parseSync()
        val headings = parsed.elements.filterIsInstance<OfficeDocumentElement.Heading>()
        val bab1 = headings.firstOrNull { it.text.contains("PENDAHULUAN") }
        assertNotNull("PENDAHULUAN heading present", bab1)
        assertTrue("PENDAHULUAN must start with 'BAB ' prefix from lvlText",
            bab1!!.text.startsWith("BAB "))
        assertTrue("BAB 1 prefix must precede PENDAHULUAN",
            bab1.text.removePrefix("BAB ").startsWith("1"))

        val bab2 = headings.firstOrNull { it.text.contains("PEMBAHASAN") }
        assertNotNull("PEMBAHASAN heading present", bab2)
        assertTrue("PEMBAHASAN starts with BAB 2",
            bab2!!.text.startsWith("BAB 2"))

        val bab3 = headings.firstOrNull { it.text.contains("PENUTUP") }
        assertNotNull("PENUTUP heading present", bab3)
        assertTrue("PENUTUP starts with BAB 3",
            bab3!!.text.startsWith("BAB 3"))
    }

    @Test
    fun `sample 6 TOC entries after user cleanup show BAB I PENDAHULUAN shape`() {
        val parsed = parseSync()
        val toc = parsed.authoredIndexes.firstOrNull { it.kind == DocumentIndexKind.TABLE_OF_CONTENT }
        assertNotNull("TOC authored index present", toc)
        val chapterEntries = toc!!.entries.filter { it.level == 1 && "BAB" in it.text }
        assertTrue("TOC has chapter entries starting with BAB", chapterEntries.isNotEmpty())
        assertFalse("no PAGEREF leakage", parsed.plainText.contains("PAGEREF"))
        assertFalse("no TOC \\o instr leakage",
            parsed.plainText.contains("TOC \\o") || parsed.plainText.contains("HYPERLINK \\l"))
    }

    private fun parseSync() = runBlocking {
        OfficeDocumentParser(ApplicationProvider.getApplicationContext<Context>())
            .parseDocument(File("tests/inky/Sample-6.docx"), bypassCache = true)
    }
}
