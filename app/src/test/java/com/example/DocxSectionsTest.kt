package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
 * Plan 8B Commit 4: DOCX sectPr pgNumType/titlePg metadata and lastRenderedPageBreak hygiene.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocxSectionsTest {

    @Test
    fun `sample 6 sectPr blocks expose titlePg and lowerRoman then arabic page numbering`() {
        val parsed = parseSync(File("tests/inky/Sample-6.docx"))
        assertTrue("at least two sections found", parsed.sectionStarts.size >= 2)
        assertEquals("specs line up with sections",
            parsed.sectionStarts.size, parsed.docxSectionBreakSpecs.size)
        // First section (cover page) carries titlePg and uses lowerRoman page numbers.
        val first = parsed.docxSectionBreakSpecs.first()
        assertTrue("cover has titlePg", first.titlePg)
        assertEquals("cover uses lowerRoman", "lowerRoman", first.pageNumberFormat)
        // At least one later section restarts numbering at 1 (TOC or body).
        val restart = parsed.docxSectionBreakSpecs.firstOrNull { it.pageNumberStart == 1 }
        assertNotNull("some section restarts page numbers at 1", restart)
    }

    @Test
    fun `lastRenderedPageBreak does not swallow following text`() {
        // w:lastRenderedPageBreak is a layout hint and must not suppress the
        // w:t that follows it; the text 'HELLO' must remain visible.
        val docx = buildDocx("""
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>
<w:p><w:pPr><w:sectPr><w:pgNumType w:fmt="lowerRoman" w:start="1"/><w:titlePg/></w:sectPr></w:pPr></w:p>
<w:p><w:r><w:br w:type="page"/></w:r><w:r><w:lastRenderedPageBreak/><w:t>HELLO</w:t></w:r></w:p>
<w:p><w:sectPr/></w:p>
</w:body></w:document>""")
        val parsed = parseSync(docx)
        val ps = parsed.elements.filterIsInstance<OfficeDocumentElement.Paragraph>()
        assertTrue("HELLO text preserved past lrpb", ps.any { "HELLO" in it.text })
        docx.delete()
    }

    private fun parseSync(file: File) = runBlocking {
        OfficeDocumentParser(ApplicationProvider.getApplicationContext<Context>())
            .parseDocument(file, bypassCache = true)
    }

    private fun buildDocx(documentXml: String): File {
        val tmp = Files.createTempFile("docx-sections-test", ".docx").toFile()
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
