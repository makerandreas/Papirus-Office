package com.example

import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.io.File

/**
 * Plan 8B end-to-end rendering tests for the user-authored fixture shapes:
 * BAB-prefixed headings, numId=0 "no-man's-land" de-numbered headings, and
 * the post-edit TOC snapshot.
 */
class DocxAuthoredShapeTest {

    @Test
    fun `sample 6 no-man's-land preface headings have no BAB label`() {
        // KATA PENGANTAR, DAFTAR ISI, DAFTAR PUSTAKA are Judul1 (heading 1)
        // paragraphs whose w:pPr/w:numPr has w:numId w:val="0". Per Writer
        // Guide "multiple paragraphs on one entry list" technique, these
        // must render bare text with no numbering label prepended.
        val parsed = parser().parseDocument(File("tests/inky/Sample-6.docx"), bypassCache = true).get()
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
        // numId=15 through Judul1. The level-0 format is upperRoman with
        // lvlText "BAB %1" and suff="space", so labels are "BAB I ", "BAB II ",
        // "BAB III ". The w:br authored between the roman numeral and the
        // title text (the user's hand-edit) is preserved as '\n' so the label
        // prepended at the first run reads "BAB I\nPENDAHULUAN".
        val parsed = parser().parseDocument(File("tests/inky/Sample-6.docx"), bypassCache = true).get()
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
        // After the user's cleanup (backspace at start of PENDAHULUAN joins
        // BAB and I with an auto-space, then a typed space replaces the
        // deleted line break), TOC1 entries for the chapters read as
        // "BAB I PENDAHULUAN....". This test only checks that the TOC
        // entries are the cached result text (no TOC field codes leaked)
        // and that the entry text begins with "BAB" after cleanup.
        val parsed = parser().parseDocument(File("tests/inky/Sample-6.docx"), bypassCache = true).get()
        val toc = parsed.authoredIndexes.firstOrNull { it.kind == com.makerandreas.papirusoffice.data.DocumentIndexKind.TABLE_OF_CONTENT }
        assertNotNull("TOC authored index present", toc)
        val chapterEntries = toc!!.entries.filter { it.level == 1 && "BAB" in it.text }
        assertTrue("TOC has chapter entries starting with BAB", chapterEntries.isNotEmpty())
        // No TOC field instruction leak.
        assertFalse("no PAGEREF leakage", parsed.plainText.contains("PAGEREF"))
        assertFalse("no TOC \\\\o instr leakage", parsed.plainText.contains("TOC \\o") || parsed.plainText.contains("HYPERLINK \\l"))
    }

    private fun parser(): OfficeDocumentParser = OfficeDocumentParser(RuntimeEnvironment.getApplication())
}
