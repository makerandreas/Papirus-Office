package com.example

import com.makerandreas.papirusoffice.data.DocxNumberingReader
import com.makerandreas.papirusoffice.data.NumberingCounterState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Tests for [DocxNumberingReader], covering the synthetic reader cases plus
 * the real six fixtures in `tests/inky/`.
 */
class DocxNumberingReaderTest {

    @Test
    fun `sample 3 has no numbering xml so result is empty`() {
        val result = DocxNumberingReader.read(fixture("Sample-3.docx"))
        assertTrue("Sample-3 has no word/numbering.xml", result.isEmpty())
    }

    @Test
    fun `sample 6 numId 15 resolves a multi-level spec with BAB prefix on level 1`() {
        val result = DocxNumberingReader.read(fixture("Sample-6.docx"))
        val spec = result.numSpecs[15]
        assertNotNull("numId 15 must resolve", spec)
        val lvl1 = spec!!.level(1)!!
        // The Judul1-bound multi-level list carries the literal prefix "BAB "
        // from lvlText (read verbatim, never hard-coded). Level 1 uses one
        // display level; deeper levels use the typical %1.%2 decimal form.
        assertEquals("BAB ", lvl1.numPrefix)
        assertEquals("", lvl1.numSuffix)
        assertEquals(1, lvl1.displayLevels)

        val lvl2 = spec.level(2)!!
        assertEquals(".", lvl2.numSuffix)
        assertEquals(2, lvl2.displayLevels)

        val counter = NumberingCounterState()
        val first = counter.advance(spec, 1)
        assertTrue("first chapter label starts with BAB prefix", first.startsWith("BAB "))
        // Sub-heading level 2: starts with the digit "1".
        val sub1 = counter.advance(spec, 2)
        assertTrue("first sub-heading starts with '1.'", sub1.startsWith("1."))
        // Sub-sub-heading level 3: "1.1.1"-style.
        val sub2 = counter.advance(spec, 3)
        assertTrue("sub-sub has two dots", sub2.count { it == '.' } == 2)
        // Back to chapter 2: prefix BAB with a higher digit and counter reset.
        val second = counter.advance(spec, 1)
        assertTrue("second chapter label starts with BAB prefix", second.startsWith("BAB "))
    }

    @Test
    fun `negative abstractNumId is ignored per MS-OI29500`() {
        // A numbering.xml that only contains a negative abstractNum should
        // parse without crashing and produce an empty numSpec.
        val tmp = Files.createTempFile("neg-num", ".docx").toFile()
        writeMinimalDocx(tmp, numberingXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:numbering xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:abstractNum w:abstractNumId="-1"><w:lvl w:ilvl="0"><w:start w:val="1"/><w:numFmt w:val="decimal"/><w:lvlText w:val="%1."/></w:lvl></w:abstractNum>
  <w:num w:numId="1"><w:abstractNumId w:val="-1"/></w:num>
</w:numbering>""")
        val result = DocxNumberingReader.read(tmp)
        assertNull("negative abstractNumId must not produce a spec", result.numSpecs[1])
        tmp.delete()
    }

    @Test
    fun `numId zero is the suppression sentinel and has no spec`() {
        val result = DocxNumberingReader.read(fixture("Sample-6.docx"))
        assertNull("numId 0 must never be a spec (it is suppression)", result.numSpecs[0])
    }

    private fun fixture(name: String): File = File("tests/inky", name).let {
        require(it.exists()) { "fixture ${it.absolutePath} not found; run tests from the repo root" }
        it
    }

    /** Write a minimal DOCX-shaped ZIP containing a document.xml stub and the
     * supplied [numberingXml] under `word/numbering.xml`. The reader only
     * inspects the numbering part, so document.xml can be a stub. */
    private fun writeMinimalDocx(file: File, numberingXml: String) {
        java.util.zip.ZipOutputStream(file.outputStream()).use { zos ->
            fun writeEntry(name: String, content: String) {
                val entry = java.util.zip.ZipEntry(name)
                zos.putNextEntry(entry)
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            writeEntry(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/numbering.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.numbering+xml"/>
</Types>"""
            )
            writeEntry(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""
            )
            writeEntry(
                "word/_rels/document.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering" Target="numbering.xml"/>
</Relationships>"""
            )
            writeEntry(
                "word/document.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body><w:p><w:r><w:t>hello</w:t></w:r></w:p></w:body>
</w:document>"""
            )
            writeEntry("word/numbering.xml", numberingXml)
        }
    }
}
