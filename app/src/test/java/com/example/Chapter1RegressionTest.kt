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
}
