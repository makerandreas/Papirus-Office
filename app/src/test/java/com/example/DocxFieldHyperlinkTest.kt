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
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Plan 8B Commit 2: DOCX hyperlinks, bookmarks and complex-field parsing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
        val parsed = parseSync(docx)
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
        val parsed = parseSync(docx)
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
        val parsed = parseSync(docx)
        val para = parsed.elements.filterIsInstance<OfficeDocumentElement.Paragraph>().first()
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
        val parsed = parseSync(docx)
        val para = parsed.elements.filterIsInstance<OfficeDocumentElement.Paragraph>().first()
        assertTrue("paragraph must carry bookmark", "_TocStart" in para.bookmarks)
        docx.delete()
    }

    @Test
    fun `sample 6 plain text keeps cached TOC entries without PAGEREF leakage`() = runBlocking {
        val parser = OfficeDocumentParser(ApplicationProvider.getApplicationContext<Context>())
        val parsed = parser.parseDocument(SampleMatrix.findTestFile("Sample-6.docx"), bypassCache = true)
        val plain = parsed.plainText
        // Both leakage vectors, not one or the other: PAGEREF is the entry target and
        // TOC \o is the field instruction that opened the block. Covering only one
        // leaves the other free to regress.
        assertFalse("PAGEREF must not leak into plain text", plain.contains("PAGEREF"))
        assertFalse("TOC instruction must not leak into plain text", plain.contains("TOC \\o"))
        assertTrue("first TOC entry text must remain",
            plain.contains("KATA PENGANTAR") || plain.contains("DAFTAR ISI"))
    }

    private fun parseSync(file: File) = runBlocking {
        OfficeDocumentParser(ApplicationProvider.getApplicationContext<Context>())
            .parseDocument(file, bypassCache = true)
    }

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
            entry("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="xml" ContentType="application/xml"/>
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
""" + if (withNumbering) """<Override PartName="/word/numbering.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.numbering+xml"/>""" else "" + """
</Types>""")
            entry("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""")
            val docRels = buildString {
                append("""<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
                if (withNumbering) append("""<Relationship Id="rIdNum" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering" Target="numbering.xml"/>""")
                if (withRels) append("""<Relationship Id="rIdLink" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink" Target="http://example.com" TargetMode="External"/>""")
                append("</Relationships>")
            }
            entry("word/_rels/document.xml.rels", docRels)
            entry("word/styles.xml", """<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Aptos" w:hAnsi="Aptos"/><w:sz w:val="24"/></w:rPr></w:rPrDefault></w:docDefaults>
<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/></w:style>
</w:styles>""")
            if (withNumbering) entry("word/numbering.xml", """<w:numbering xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:abstractNum w:abstractNumId="0"><w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="decimal"/><w:lvlText w:val="%1."/></w:lvl></w:abstractNum>
<w:num w:numId="1"><w:abstractNumId w:val="0"/></w:num>
</w:numbering>""")
            entry("word/document.xml", documentXml)
        }
        return tmp
    }
}
