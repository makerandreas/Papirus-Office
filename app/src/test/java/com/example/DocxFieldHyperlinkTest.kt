package com.example

import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Plan 8B Commit 2: verify that DOCX hyperlinks, bookmarks and complex
 * fields are parsed correctly: hyperlink anchors/URLs reach runs, bookmark
 * names reach paragraphs, and TOC/SEQ/PAGEREF instruction text never leaks
 * into plain text.
 *
 * These tests build a minimal DOCX zip in a temp file so they run on JVM
 * without needing a Gradle/Android device context beyond the Robolectric
 * application that OfficeDocumentParser already uses.
 */
class DocxFieldHyperlinkTest {

    @Test
    fun `complex field TOC instruction is stripped from plain text and cached result stays`() {
        val docx = buildDocx(
            documentXml = """
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
            <w:body>
              <w:p><w:r><w:fldChar w:fldCharType="begin"/></w:r>
                <w:r><w:instrText xml:space="preserve"> TOC \o "1-9" \z </w:instrText></w:r>
                <w:r><w:fldChar w:fldCharType="separate"/></w:r>
                <w:r><w:t>Daftar Isi</w:t></w:r>
                <w:r><w:fldChar w:fldCharType="end"/></w:r></w:p>
              <w:p><w:r><w:t>Normal body text.</w:t></w:r></w:p>
            </w:body></w:document>
            """.trimIndent(),
            withNumbering = false,
            withRels = false
        )
        val parsed = parser().parseDocument(docx, bypassCache = true).get()
        val plain = parsed.plainText
        assertFalse("TOC instruction must not leak to plain text", plain.contains("TOC"))
        assertFalse("instrText backslash must not leak", plain.contains("\\"))
        assertTrue("cached field result must remain", plain.contains("Daftar Isi"))
        assertTrue("following body text must remain", plain.contains("Normal body text."))
        docx.delete()
    }

    @Test
    fun `SEQ field instruction does not leak into plain text`() {
        val docx = buildDocx(
            documentXml = """
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
            <w:body>
              <w:p><w:r><w:t>Gambar </w:t></w:r>
                <w:r><w:fldChar w:fldCharType="begin"/></w:r>
                <w:r><w:instrText xml:space="preserve"> SEQ Figure \* ARABIC </w:instrText></w:r>
                <w:r><w:fldChar w:fldCharType="separate"/></w:r>
                <w:r><w:t>1</w:t></w:r>
                <w:r><w:fldChar w:fldCharType="end"/></w:r></w:p>
            </w:body></w:document>
            """.trimIndent(),
            withNumbering = false,
            withRels = false
        )
        val parsed = parser().parseDocument(docx, bypassCache = true).get()
        val plain = parsed.plainText
        assertFalse("SEQ instruction must not leak", plain.contains("SEQ"))
        assertTrue("cached SEQ result must remain", plain.contains("1"))
        docx.delete()
    }

    @Test
    fun `hyperlink anchor is attached to runs`() {
        val docx = buildDocx(
            documentXml = """
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
                        xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
            <w:body>
              <w:p><w:hyperlink w:anchor="_TOC000009"><w:r><w:t>BAB I PENDAHULUAN</w:t></w:r></w:hyperlink></w:p>
            </w:body></w:document>
            """.trimIndent(),
            withNumbering = false,
            withRels = false
        )
        val parsed = parser().parseDocument(docx, bypassCache = true).get()
        val para = parsed.elements.filterIsInstance<com.makerandreas.papirusoffice.data.OfficeDocumentElement.Paragraph>().first()
        assertTrue("paragraph must have runs", para.runs.isNotEmpty())
        assertEquals("hyperlink anchor must be #_TOC...", "#_TOC000009", para.runs.first().hyperlink)
        docx.delete()
    }

    @Test
    fun `bookmark names are attached to the paragraph`() {
        val docx = buildDocx(
            documentXml = """
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
            <w:body>
              <w:p><w:bookmarkStart w:id="0" w:name="_TocStart"/><w:r><w:t>hello</w:t></w:r><w:bookmarkEnd w:id="0"/></w:p>
            </w:body></w:document>
            """.trimIndent(),
            withNumbering = false,
            withRels = false
        )
        val parsed = parser().parseDocument(docx, bypassCache = true).get()
        val para = parsed.elements.filterIsInstance<com.makerandreas.papirusoffice.data.OfficeDocumentElement.Paragraph>().first()
        assertTrue("paragraph must carry bookmark", "_TocStart" in para.bookmarks)
        docx.delete()
    }

    @Test
    fun `sample 6 plain text has no TOC instrText leakage`() {
        val parsed = parser().parseDocument(File("tests/inky/Sample-6.docx"), bypassCache = true).get()
        val plain = parsed.plainText
        // The TOC field in Sample-6 writes an instrText " TOC \o \"1 - 9\" \\z "
        // which must not appear in plainText. Word also writes a cached result,
        // so TOC entry headings themselves must still be present.
        assertFalse("TOC instruction must not leak", plain.contains("TOC \\\\o"))
        assertFalse("instr text must not leak", " \\\\o " in plain || "\\\"1" in plain)
        // First TOC entry text (from cached result) should still appear.
        assertTrue("first TOC entry text must remain", plain.contains("KATA PENGANTAR") || plain.contains("DAFTAR ISI"))
    }

    private fun parser(): OfficeDocumentParser = OfficeDocumentParser(RuntimeEnvironment.getApplication())

    private fun buildDocx(
        documentXml: String,
        withNumbering: Boolean,
        withRels: Boolean
    ): File {
        val tmp = Files.createTempFile("docx-field-test", ".docx").toFile()
        ZipOutputStream(tmp.outputStream()).use { zos ->
            fun entry(name: String, body: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(body.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            entry(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="xml" ContentType="application/xml"/>
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
""" + if (withNumbering) """<Override PartName="/word/numbering.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.numbering+xml"/>""" else "" + """
</Types>"""
            )
            entry(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
            )
            val docRels = buildString {
                append("""<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
                if (withNumbering) append("""<Relationship Id="rIdNum" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering" Target="numbering.xml"/>""")
                if (withRels) append("""<Relationship Id="rIdLink" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink" Target="http://example.com" TargetMode="External"/>""")
                append("</Relationships>")
            }
            entry("word/_rels/document.xml.rels", docRels)
            entry(
                "word/styles.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Aptos" w:hAnsi="Aptos"/><w:sz w:val="24"/></w:rPr></w:rPrDefault><w:pPrDefault><w:pPr><w:spacing w:after="160" w:line="276"/></w:pPr></w:pPrDefault></w:docDefaults>
<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/></w:style>
</w:styles>"""
            )
            if (withNumbering) entry(
                "word/numbering.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:numbering xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:abstractNum w:abstractNumId="0"><w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="decimal"/><w:lvlText w:val="%1."/></w:lvl></w:abstractNum>
<w:num w:numId="1"><w:abstractNumId w:val="0"/></w:num>
</w:numbering>"""
            )
            entry("word/document.xml", documentXml)
        }
        return tmp
    }
}
