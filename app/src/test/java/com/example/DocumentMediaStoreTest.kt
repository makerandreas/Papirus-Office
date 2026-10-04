package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.DocumentMediaStore
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocumentMediaStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun storesMediaDurablyWithManifestAndRecoversDeletedEntry() {
        val root = File(temporaryFolder.root, "files/media")
        val source = packageFile("durable.docx", mapOf("word/media/logo.png" to bytes("image-one")))
        val store = DocumentMediaStore(root)

        val first = store.extractImages(source)
        val firstFile = first.getValue("word/media/logo.png")
        assertTrue(firstFile.isFile)
        assertTrue(firstFile.canonicalPath.startsWith(root.canonicalPath + File.separator))
        val manifest = File(firstFile.parentFile.parentFile, "manifest.properties")
        assertTrue(manifest.isFile)
        val manifestText = manifest.readText()
        assertTrue(manifestText.contains("entry.0.path="))
        assertTrue(manifestText.contains("entry.0.size=9"))
        assertTrue(manifestText.contains("entry.0.sha256="))
        assertArrayEquals(bytes("image-one"), firstFile.readBytes())

        assertTrue("simulate a cache entry removed outside the parser", firstFile.delete())
        val recovered = DocumentMediaStore(root).extractImages(source)
        val recoveredFile = recovered.getValue("word/media/logo.png")
        assertTrue(recoveredFile.isFile)
        assertEquals("stable paths let cached parsed elements resolve after repair", firstFile.absolutePath, recoveredFile.absolutePath)
        assertArrayEquals(bytes("image-one"), recoveredFile.readBytes())
    }

    @Test
    fun changedSourceReplacesOldMediaVersion() {
        val root = File(temporaryFolder.root, "files/media")
        val source = packageFile("edited.odt", mapOf("Pictures/photo.png" to bytes("old")))
        val store = DocumentMediaStore(root)
        val oldFile = store.extractImages(source).getValue("Pictures/photo.png")
        val oldModified = source.lastModified()

        packageFile(source.name, mapOf("Pictures/photo.png" to bytes("new-media")))
        assertTrue(source.setLastModified(oldModified + 10_000L))
        val newFile = store.extractImages(source).getValue("Pictures/photo.png")

        assertNotEquals(oldFile.absolutePath, newFile.absolutePath)
        assertFalse("stale versions of the same source are removed", oldFile.exists())
        assertArrayEquals(bytes("new-media"), newFile.readBytes())
        assertEquals(1, root.listFiles()?.count { it.isDirectory })
    }

    @Test
    fun evictsLeastRecentlyUsedDocumentToEnforceAggregateStoreLimit() {
        val root = File(temporaryFolder.root, "files/media")
        val limits = limits(maxDocumentMediaBytes = 6, maxStoreBytes = 6)
        val store = DocumentMediaStore(root, limits)
        val firstSource = packageFile("first.docx", mapOf("word/media/a.png" to bytes("1234")))
        val secondSource = packageFile("second.docx", mapOf("word/media/b.png" to bytes("5678")))
        val firstFile = store.extractImages(firstSource).getValue("word/media/a.png")
        val firstDirectory = firstFile.parentFile.parentFile
        assertTrue(firstDirectory.setLastModified(1_000L))

        val secondFile = store.extractImages(secondSource).getValue("word/media/b.png")

        assertFalse("the older document is evicted first", firstFile.exists())
        assertTrue(secondFile.isFile)
        assertTrue(store.storedBytes() <= limits.maxStoreBytes)
    }

    @Test
    fun duplicateBasenamesKeepDistinctPackagePathsAndFiles() {
        val root = File(temporaryFolder.root, "files/media")
        val source = packageFile(
            "duplicates.docx",
            linkedMapOf(
                "word/media/first/logo.png" to bytes("first-logo"),
                "word/media/second/logo.png" to bytes("second-logo")
            )
        )
        val images = DocumentMediaStore(root).extractImages(source)
        val first = images.getValue("word/media/first/logo.png")
        val second = images.getValue("word/media/second/logo.png")

        assertNotEquals(first.absolutePath, second.absolutePath)
        assertArrayEquals(bytes("first-logo"), first.readBytes())
        assertArrayEquals(bytes("second-logo"), second.readBytes())
        assertEquals(first, images.getValue("media/first/logo.png"))
        assertEquals(second, images.getValue("media/second/logo.png"))
        assertNull("ambiguous bare names must not point at an arbitrary image", images["logo.png"])
    }

    @Test
    fun enforcesPerImageAndPerDocumentByteCaps() {
        val root = File(temporaryFolder.root, "files/media")
        val limits = limits(maxImageBytes = 3, maxDocumentMediaBytes = 3, maxStoreBytes = 20)
        val source = packageFile(
            "bounded.docx",
            linkedMapOf(
                "word/media/one.png" to bytes("12"),
                "word/media/two.png" to bytes("34"),
                "word/media/oversized.png" to bytes("5678")
            )
        )
        val store = DocumentMediaStore(root, limits)
        val images = store.extractImages(source)

        assertTrue(images.containsKey("word/media/one.png"))
        assertFalse(images.containsKey("word/media/two.png"))
        assertFalse(images.containsKey("word/media/oversized.png"))
        assertEquals(2L, store.storedBytes())
        assertTrue(store.storedBytes() <= limits.maxDocumentMediaBytes)
    }

    @Test
    fun enforcesImageCountCap() {
        val root = File(temporaryFolder.root, "files/media")
        val limits = limits(maxImageCount = 2, maxDocumentMediaBytes = 20, maxStoreBytes = 20)
        val source = packageFile(
            "counted.odt",
            linkedMapOf(
                "Pictures/one.png" to bytes("1"),
                "Pictures/two.png" to bytes("2"),
                "Pictures/three.png" to bytes("3")
            )
        )
        val images = DocumentMediaStore(root, limits).extractImages(source)

        assertTrue(images.containsKey("Pictures/one.png"))
        assertTrue(images.containsKey("Pictures/two.png"))
        assertFalse(images.containsKey("Pictures/three.png"))
    }

    @Test
    fun enforcesZipEntryScanLimit() {
        val root = File(temporaryFolder.root, "files/media")
        val limits = limits(maxImageCount = 10, maxDocumentMediaBytes = 20, maxStoreBytes = 20, maxZipEntries = 2)
        val source = packageFile(
            "entry-limited.docx",
            linkedMapOf(
                "[Content_Types].xml" to bytes("metadata"),
                "word/document.xml" to bytes("document"),
                "word/media/after-limit.png" to bytes("image")
            )
        )

        val images = DocumentMediaStore(root, limits).extractImages(source)

        assertTrue(images.isEmpty())
    }

    @Test
    fun inMemoryParsedDocumentRepairsDeletedMediaOnCacheHit() = runBlocking {
        val fixture = fixture("Sample-6.docx")
        val source = File(temporaryFolder.root, "cache-recovery.docx")
        fixture.copyTo(source, overwrite = true)
        val parser = OfficeDocumentParser(context)
        val initial = parser.parseDocument(source, bypassCache = true)
        assertFalse(initial.isParsingFailed)
        val firstImage = initial.elements.filterIsInstance<OfficeDocumentElement.ImageElement>().first()
        val deletedFile = firstImage.imageFile
        assertNotNull(deletedFile)
        assertTrue(deletedFile!!.isFile)
        assertTrue(deletedFile.delete())

        val fromMemoryCache = parser.parseDocument(source)
        val repaired = fromMemoryCache.elements.filterIsInstance<OfficeDocumentElement.ImageElement>()
        assertEquals(3, repaired.size)
        assertTrue(repaired.all { it.imageFile?.isFile == true })
        assertTrue(fromMemoryCache.extractedImages.values.all { it.isFile })
    }

    private fun limits(
        maxImageBytes: Long = 16,
        maxImageCount: Int = 10,
        maxDocumentMediaBytes: Long = 64,
        maxStoreBytes: Long = 128,
        maxZipEntries: Int = 100
    ) = DocumentMediaStore.Limits(
        maxImageBytes = maxImageBytes,
        maxImageCount = maxImageCount,
        maxDocumentMediaBytes = maxDocumentMediaBytes,
        maxStoreBytes = maxStoreBytes,
        maxZipEntries = maxZipEntries
    )

    private fun packageFile(name: String, entries: Map<String, ByteArray>): File {
        val file = File(temporaryFolder.root, name)
        return packageFile(file, entries)
    }

    private fun packageFile(file: File, entries: Map<String, ByteArray>): File {
        file.parentFile?.mkdirs()
        ZipOutputStream(file.outputStream()).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return file
    }

    private fun fixture(name: String): File {
        var root: File? = File(".").absoluteFile
        while (root != null && !File(root, ".git").exists()) root = root.parentFile
        return File(root ?: File("."), "tests/inky/$name").also {
            assertTrue("Missing fixture ${it.absolutePath}", it.isFile)
        }
    }

    private fun bytes(value: String) = value.toByteArray(Charsets.UTF_8)
}
