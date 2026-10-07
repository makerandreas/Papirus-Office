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
        val parsed = parseSync()
        val headings = parsed.elements.filterIsInstance<OfficeDocumentElement.Heading>()
        val bab1 = headings.firstOrNull { it.text.contains("PENDAHULUAN") }
        assertNotNull("PENDAHULUAN heading present", bab1)
        assertTrue("PENDAHULUAN must start with 'BAB ' prefix from lvlText",
            bab1!!.text.startsWith("BAB "))
        assertTrue("BAB I prefix must precede PENDAHULUAN",
            bab1.text.removePrefix("BAB ").startsWith("I"))

        val bab2 = headings.firstOrNull { it.text.contains("PEMBAHASAN") }
        assertNotNull("PEMBAHASAN heading present", bab2)
        assertTrue("PEMBAHASAN starts with BAB II",
            bab2!!.text.startsWith("BAB II"))

        val bab3 = headings.firstOrNull { it.text.contains("PENUTUP") }
        assertNotNull("PENUTUP heading present", bab3)
        assertTrue("PENUTUP starts with BAB III",
            bab3!!.text.startsWith("BAB III"))
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
