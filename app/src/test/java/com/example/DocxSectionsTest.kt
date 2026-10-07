package com.example

import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Plan 8B Commit 4: DOCX sectPr pgNumType/titlePg metadata and
 * lastRenderedPageBreak hygiene (H-6, H-6b, H-7).
 */
class DocxSectionsTest {

    @Test
    fun `sample 6 sectPr blocks expose titlePg and lowerRoman then arabic page numbering`() {
        val parsed = parser().parseDocument(File("tests/inky/Sample-6.docx"), bypassCache = true).get()
        // Sample-6 has 5 sections: cover, KATA PENGANTAR, DAFTAR ISI (lowerRoman
        // start=1 on the first body section), BAB I, BAB II (arabic restart at 1).
        assertTrue("at least one section found", parsed.sectionStarts.size >= 2)
        assertEquals("specs line up with sections",
            parsed.sectionStarts.size, parsed.docxSectionBreakSpecs.size)
        val first = parsed.docxSectionBreakSpecs.first()
        assertTrue("cover has titlePg", first.titlePg)
        assertEquals("cover uses lowerRoman", "lowerRoman", first.pageNumberFormat)
        val arabicRestart = parsed.docxSectionBreakSpecs
            .firstOrNull { it.pageNumberStart == 1 && it.pageNumberFormat == null }
        assertNotNull("a body section restarts arabic page numbers at 1", arabicRestart)
    }

    @Test
    fun `lastRenderedPageBreak does not introduce extra breaks or swallow text`() {
        val docx = buildDocx("""
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>
<w:p><w:pPr><w:sectPr><w:pgNumType w:fmt="lowerRoman" w:start="1"/><w:titlePg/></w:sectPr></w:pPr></w:p>
<w:p><w:r><w:br w:type="page"/></w:r><w:r><w:lastRenderedPageBreak/><w:t>HELLO</w:t></w:r></w:p>
<w:p><w:sectPr/></w:p>
</w:body></w:document>""")
        val parsed = parser().parseDocument(docx, bypassCache = true).get()
        val ps = parsed.elements.filterIsInstance<OfficeDocumentElement.Paragraph>()
        val authoredBreaks = ps.sumOf { it.pageBreakOffsets.size }
        assertEquals("one authored break (w:br), no phantom lrpb break", 1, authoredBreaks)
        assertTrue("HELLO text preserved", ps.any { it.text == "HELLO" })
        docx.delete()
    }

    private fun parser(): OfficeDocumentParser = OfficeDocumentParser(RuntimeEnvironment.getApplication())

    private fun buildDocx(documentXml: String): File {
        val tmp = Files.createTempFile("docx-sections-test", ".docx").toFile()
        ZipOutputStream(tmp.outputStream()).use { zos ->
            fun entry(name: String, body: String) {
                zos.putNextEntry(ZipEntry(name)); zos.write(body.toByteArray(Charsets.UTF_8)); zos.closeEntry()
            }
            entry("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="xml" ContentType="application/xml"/>
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>""")
            entry("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""")
            entry("word/_rels/document.xml.rels", """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""")
            entry("word/styles.xml", """<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:docDefaults><w:rPrDefault><w:rPr><w:rFonts w:ascii="Aptos"/><w:sz w:val="24"/></w:rPr></w:rPrDefault></w:docDefaults>
<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/></w:style>
</w:styles>""")
            entry("word/document.xml", documentXml.trimIndent())
        }
        return tmp
    }
}
