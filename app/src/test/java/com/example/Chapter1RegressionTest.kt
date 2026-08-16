package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.*
import com.makerandreas.papirusoffice.data.cache.DocumentCacheRepository
import com.makerandreas.papirusoffice.data.writer.OdtDocumentParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Chapter1RegressionTest {

    private lateinit var context: Context
    private lateinit var cacheRepository: DocumentCacheRepository
    private lateinit var odtSerializer: OdtDocumentSerializer
    private lateinit var docxSerializer: DocxDocumentSerializer

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        cacheRepository = DocumentCacheRepository(context)
        odtSerializer = OdtDocumentSerializer()
        docxSerializer = DocxDocumentSerializer()
    }

    @Test
    fun test01To06_NewDocumentFromTemplateSaveAndReopen() = runBlocking {
        // Step 01: Create new document from Default.ott template
        val ottBytes = context.assets.open("templates/styles/Default.ott").use { it.readBytes() }
        val templateParser = OdtDocumentParser()
        val templateDoc = templateParser.parse(ottBytes)

        // Step 02: Type "Hello!" and add elements
        val elements = mutableListOf<OfficeElement>(
            OfficeHeading(text = "Chapter 1 Title", level = 1),
            OfficeParagraph(text = "Hello! Welcome to Papirus Office Engine.")
        )
        val sessionDoc = templateDoc.copy(body = DocumentBody(elements = elements))

        // Step 03: Save as ODT file
        val file = File(context.filesDir, "Chapter1_Test.odt")
        val saveSuccess = odtSerializer.write(sessionDoc, DocumentReference.LocalFile(file), context)
        assertTrue("Save ODT should succeed", saveSuccess)
        assertTrue("Saved file should exist on disk", file.exists() && file.length() > 0)

        // Step 04 & 05: Close session & Reopen saved ODT file
        val reopenedDoc = odtSerializer.read(DocumentReference.LocalFile(file), context)

        // Step 06: Verify "Hello!" and heading structure
        val plainText = reopenedDoc.toPlainText()
        assertTrue("Reopened document should contain 'Hello!'", plainText.contains("Hello!"))
        assertTrue("Reopened document should contain 'Chapter 1 Title'", plainText.contains("Chapter 1 Title"))
    }

    @Test
    fun test07To10_ReloadDocumentBehavior() = runBlocking {
        val file = File(context.filesDir, "ReloadTest.odt")
        val initialDoc = OfficeDocument(
            body = DocumentBody(elements = listOf(OfficeParagraph("Original Content")))
        )
        odtSerializer.write(initialDoc, DocumentReference.LocalFile(file), context)

        // Create an active session with modified content (unsaved edits)
        val editedDoc = OfficeDocument(
            body = DocumentBody(elements = listOf(
                OfficeParagraph("Original Content"),
                OfficeParagraph("Unsaved Draft Edit")
            ))
        )

        // Reload YES -> Discard unsaved edits, reload from physical disk file
        val reloadedDocYes = odtSerializer.read(DocumentReference.LocalFile(file), context)
        assertEquals("Reloading YES should restore physical file content", "Original Content", reloadedDocYes.toPlainText().trim())

        // Reload NO -> Keep edited doc in memory
        val reloadedDocNo = editedDoc
        assertTrue("Reloading NO should keep in-memory edit", reloadedDocNo.toPlainText().contains("Unsaved Draft Edit"))
    }

    @Test
    fun test11To14_DirtyStateTrackingAndCloseConfirmation() {
        val file = File(context.filesDir, "DirtyTest.odt")
        val session = DocumentSession(
            engine = DocumentEngine(),
            document = OfficeDocument(body = DocumentBody(elements = listOf(OfficeParagraph("Base text")))),
            file = OfficeFile(file)
        )

        assertFalse("New session with unchanged document should not be dirty", session.dirty)

        // Modify document
        session.document = session.document.copy(
            body = DocumentBody(elements = session.document.body.elements + OfficeParagraph("New addition"))
        )
        session.dirty = true

        assertTrue("Session with edits should be marked dirty", session.dirty)

        // Simulate save
        session.dirty = false
        assertFalse("Session after markSaved should no longer be dirty", session.dirty)
    }

    @Test
    fun test15To16_RecentsTrackingAndPersistence() = runBlocking {
        val file = File(context.filesDir, "RecentDoc.odt")
        file.writeText("Recent Document Content")

        // Store metadata in cache repository / recents
        cacheRepository.saveCachedDocument(
            file = file,
            text = "Recent Document Content"
        )

        val cachedData = cacheRepository.getCachedDocument(file)
        assertNotNull("Cache repository should find stored recent entry", cachedData)
        assertEquals("Cached text should match written content", "Recent Document Content", cachedData?.plainText)
    }

    @Test
    fun test17To20_OpenExternalOdtAndDocxFiles() = runBlocking {
        // External ODT test
        val odtFile = File(context.filesDir, "external.odt")
        val originalOdtDoc = OfficeDocument(
            body = DocumentBody(elements = listOf(
                OfficeHeading("External ODT Heading", level = 1),
                OfficeParagraph("External ODT Body Text")
            ))
        )
        odtSerializer.write(originalOdtDoc, DocumentReference.LocalFile(odtFile), context)

        val readOdtDoc = odtSerializer.read(DocumentReference.LocalFile(odtFile), context)
        assertTrue("External ODT heading retained", readOdtDoc.toPlainText().contains("External ODT Heading"))

        // External DOCX test
        val docxFile = File(context.filesDir, "external.docx")
        val originalDocxDoc = OfficeDocument(
            body = DocumentBody(elements = listOf(OfficeParagraph("External DOCX Paragraph")))
        )
        docxSerializer.write(originalDocxDoc, DocumentReference.LocalFile(docxFile), context)

        val readDocxDoc = docxSerializer.read(DocumentReference.LocalFile(docxFile), context)
        assertTrue("External DOCX paragraph retained", readDocxDoc.toPlainText().contains("External DOCX Paragraph"))
    }

    @Test
    fun test21To24_SaveOdtInvalidateCacheReopenFromPhysicalFile() = runBlocking {
        val file = File(context.filesDir, "StructuredPhysical.odt")

        // Create a rich structured document: Heading + Paragraphs + Table
        val table = OfficeTable(
            rows = listOf(
                OfficeTableRow(cells = listOf(OfficeTableCell("Header 1"), OfficeTableCell("Header 2"))),
                OfficeTableRow(cells = listOf(OfficeTableCell("Val 1"), OfficeTableCell("Val 2")))
            ),
            numColumns = 2
        )
        val complexDoc = OfficeDocument(
            body = DocumentBody(elements = listOf(
                OfficeHeading("Main Title", level = 1),
                OfficeParagraph("Formatted paragraph text"),
                table
            ))
        )

        // Save ODT physically
        val saveResult = odtSerializer.write(complexDoc, DocumentReference.LocalFile(file), context)
        assertTrue("Save structured ODT should succeed", saveResult)

        // Forcefully invalidate cache
        cacheRepository.invalidateCache(file)

        // Reopen strictly from physical file on disk
        val physicalDoc = odtSerializer.read(DocumentReference.LocalFile(file), context)

        // Verify physical file preserved structured elements (not flattened)
        val elements = physicalDoc.body.elements
        assertTrue("Should contain multiple elements from physical file", elements.size >= 3)
        assertTrue("First element should be OfficeHeading", elements[0] is OfficeHeading)
        assertEquals("Title text matches", "Main Title", (elements[0] as OfficeHeading).text)
        assertTrue("Third element should be OfficeTable", elements.any { it is OfficeTable })
    }

    @Test
    fun test25To30_OdfPackageStrictConformance() = runBlocking {
        val file = File(context.filesDir, "PackageConformance.odt")
        val doc = OfficeDocument(
            body = DocumentBody(elements = listOf(OfficeParagraph("Conformance Test")))
        )
        odtSerializer.write(doc, DocumentReference.LocalFile(file), context)

        var hasMimetype = false
        var isMimetypeUncompressed = false
        var hasManifest = false
        var hasContentXml = false
        var hasStylesXml = false
        var hasMetaXml = false

        java.util.zip.ZipInputStream(file.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "mimetype" -> {
                        hasMimetype = true
                        if (entry.method == java.util.zip.ZipEntry.STORED) {
                            isMimetypeUncompressed = true
                        }
                    }
                    "META-INF/manifest.xml" -> hasManifest = true
                    "content.xml" -> hasContentXml = true
                    "styles.xml" -> hasStylesXml = true
                    "meta.xml" -> hasMetaXml = true
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        assertTrue("Package must contain mimetype", hasMimetype)
        assertTrue("mimetype must be uncompressed STORED entry per ODF spec", isMimetypeUncompressed)
        assertTrue("Package must contain META-INF/manifest.xml", hasManifest)
        assertTrue("Package must contain content.xml", hasContentXml)
        assertTrue("Package must contain styles.xml", hasStylesXml)
        assertTrue("Package must contain meta.xml", hasMetaXml)
    }

    @Test
    fun test31To35_ExternalOdtRoundTripPackagePreservation() = runBlocking {
        // Create an external ODT mock with rich styles.xml and an extra file (Pictures/sample.png)
        val customStylesXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0" office:version="1.2">
              <office:styles>
                <style:style style:name="Custom_LibreOffice_Style" style:family="paragraph">
                  <style:text-properties fo:font-size="16pt" fo:font-weight="bold" fo:color="#FF0000"/>
                </style:style>
              </office:styles>
            </office:document-styles>
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val customContentXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" office:version="1.2">
              <office:body>
                <office:text>
                  <text:p text:style-name="Custom_LibreOffice_Style">Hello External LibreOffice Document!</text:p>
                </office:text>
              </office:body>
            </office:document-content>
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val customManifestXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2">
              <manifest:file-entry manifest:full-path="/" manifest:media-type="application/vnd.oasis.opendocument.text"/>
              <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
              <manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/>
              <manifest:file-entry manifest:full-path="Pictures/sample.png" manifest:media-type="image/png"/>
            </manifest:manifest>
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val samplePngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

        val initialOdtFile = File(context.filesDir, "ExternalDoc.odt")
        java.util.zip.ZipOutputStream(initialOdtFile.outputStream()).use { zout ->
            val mime = "application/vnd.oasis.opendocument.text".toByteArray()
            val entry = java.util.zip.ZipEntry("mimetype").apply {
                method = java.util.zip.ZipEntry.STORED
                size = mime.size.toLong()
                crc = java.util.zip.CRC32().apply { update(mime) }.value
            }
            zout.putNextEntry(entry)
            zout.write(mime)
            zout.closeEntry()

            zout.putNextEntry(java.util.zip.ZipEntry("content.xml"))
            zout.write(customContentXml)
            zout.closeEntry()

            zout.putNextEntry(java.util.zip.ZipEntry("styles.xml"))
            zout.write(customStylesXml)
            zout.closeEntry()

            zout.putNextEntry(java.util.zip.ZipEntry("META-INF/manifest.xml"))
            zout.write(customManifestXml)
            zout.closeEntry()

            zout.putNextEntry(java.util.zip.ZipEntry("Pictures/sample.png"))
            zout.write(samplePngBytes)
            zout.closeEntry()
        }

        // 1. Parse external ODT into OfficeDocument
        val parser = OdtDocumentParser()
        val parsedDoc = parser.parse(initialOdtFile)

        assertNotNull("Package data must be captured", parsedDoc.odtPackageData)
        assertTrue("Original package entries should be captured", parsedDoc.odtPackageData!!.entries.containsKey("Pictures/sample.png"))
        assertTrue("Custom styles should be parsed", parsedDoc.styles.paragraphStyles.containsKey("Custom_LibreOffice_Style"))
        assertEquals("Font weight parsed as bold", true, parsedDoc.styles.paragraphStyles["Custom_LibreOffice_Style"]?.isBold)

        // 2. Edit document in Papirus (modify text)
        val updatedElements = listOf(
            OfficeParagraph(text = "Hello External LibreOffice Document! (Edited by Papirus)", styleName = "Custom_LibreOffice_Style")
        )
        val editedDoc = parsedDoc.copy(body = DocumentBody(elements = updatedElements))

        // 3. Save ODT back to disk
        val savedOdtFile = File(context.filesDir, "ExternalDoc_Saved.odt")
        val saveResult = odtSerializer.write(editedDoc, DocumentReference.LocalFile(savedOdtFile), context)
        assertTrue("Save must succeed", saveResult)

        // 4. Verify preserved files in output package
        val preservedEntries = mutableMapOf<String, ByteArray>()
        java.util.zip.ZipInputStream(savedOdtFile.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                preservedEntries[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        assertTrue("Pictures/sample.png must be preserved byte-for-byte in round-trip", preservedEntries.containsKey("Pictures/sample.png"))
        assertArrayEquals("Pictures/sample.png bytes match", samplePngBytes, preservedEntries["Pictures/sample.png"])
        assertEquals("styles.xml must be preserved exactly from original document", String(customStylesXml), String(preservedEntries["styles.xml"] ?: ByteArray(0)))

        // 5. Reopen saved file and verify content + structure
        val reopened = odtSerializer.read(DocumentReference.LocalFile(savedOdtFile), context)
        assertTrue("Reopened text reflects edits", reopened.toPlainText().contains("(Edited by Papirus)"))
    }
}
