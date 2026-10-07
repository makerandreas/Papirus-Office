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
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Plan 8B Commit 3: DOCX table geometry and TOC snapshot detection.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocxTableTocTest {

    @Test
    fun `sample 6 table parses 5 columns from tblGrid with header row`() {
        val parsed = parseSync(File("tests/inky/Sample-6.docx"))
        val tables = parsed.elements.filterIsInstance<OfficeDocumentElement.Table>()
        assertEquals("Sample-6 has 1 table", 1, tables.size)
        val table = tables.first()
        assertEquals("5 columns from tblGrid", 5, table.columns.size)
        assertTrue("first row is header", table.rows.first().isHeader)
    }

    @Test
    fun `synthetic table with gridSpan records column span on the first cell`() {
        val docx = buildDocx("""
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
            <w:body>
              <w:tbl>
                <w:tblPr><w:tblW w:w="0" w:type="auto"/></w:tblPr>
                <w:tblGrid><w:gridCol w:w="1000"/><w:gridCol w:w="1000"/><w:gridCol w:w="1000"/></w:tblGrid>
                <w:tr>
                  <w:tc><w:tcPr><w:gridSpan w:val="2"/></w:tcPr><w:p><w:r><w:t>spanning</w:t></w:r></w:p></w:tc>
                  <w:tc><w:p><w:r><w:t>c</w:t></w:r></w:p></w:tc>
                </w:tr>
                <w:tr>
                  <w:tc><w:p><w:r><w:t>a</w:t></w:r></w:p></w:tc>
                  <w:tc><w:p><w:r><w:t>b</w:t></w:r></w:p></w:tc>
                  <w:tc><w:p><w:r><w:t>c</w:t></w:r></w:p></w:tc>
                </w:tr>
              </w:tbl>
            </w:body></w:document>
            """.trimIndent())
        val parsed = parseSync(docx)
        val table = parsed.elements.filterIsInstance<OfficeDocumentElement.Table>().first()
        assertEquals("3 columns", 3, table.numColumns)
        assertEquals("first cell spans 2", 2, table.rows[0].cells[0].columnSpan)
        docx.delete()
    }

    @Test
    fun `sample 6 TOC snapshot is detected with toc1 toc2 toc3 entries`() {
        val parsed = parseSync(File("tests/inky/Sample-6.docx"))
        val toc = parsed.authoredIndexes.firstOrNull { it.kind == DocumentIndexKind.TABLE_OF_CONTENT }
        assertNotNull("Sample-6 must expose a TOC index", toc)
        assertEquals("45 TOC entries (6+11+28)", 45, toc!!.entries.size)
        val byLevel = toc.entries.groupBy { it.level }
        assertEquals(6, byLevel[1]?.size ?: 0)
        assertEquals(11, byLevel[2]?.size ?: 0)
        assertEquals(28, byLevel[3]?.size ?: 0)
    }

    @Test
    fun `sample 4 TOC does not list unused toc 3-9 style definitions`() {
        val parsed = parseSync(File("tests/inky/Sample-4.docx"))
        val toc = parsed.authoredIndexes.firstOrNull { it.kind == DocumentIndexKind.TABLE_OF_CONTENT }
        assertNotNull("Sample-4 must expose a TOC index", toc)
        assertEquals("Sample-4 has 21 authored TOC entries", 21, toc!!.entries.size)
        assertFalse("no entry is labelled TOC Heading",
            toc.entries.any { it.text.contains("TOC Heading", ignoreCase = true) })
    }

    private fun parseSync(file: File) = runBlocking {
        OfficeDocumentParser(ApplicationProvider.getApplicationContext<Context>())
            .parseDocument(file, bypassCache = true)
    }

    private fun buildDocx(documentXml: String): File {
        val tmp = Files.createTempFile("docx-table-test", ".docx").toFile()
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
<w:style w:type="paragraph" w:styleId="TOC1"><w:name w:val="toc 1"/></w:style>
<w:style w:type="paragraph" w:styleId="TOC2"><w:name w:val="toc 2"/></w:style>
<w:style w:type="paragraph" w:styleId="TOC3"><w:name w:val="toc 3"/></w:style>
<w:style w:type="paragraph" w:styleId="JudulTOC"><w:name w:val="TOC Heading"/></w:style>
</w:styles>""")
            entry("word/document.xml", documentXml)
        }
        return tmp
    }
}
