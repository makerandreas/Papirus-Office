package com.example

import com.makerandreas.papirusoffice.data.cache.DocumentCacheDao
import com.makerandreas.papirusoffice.data.cache.DocumentCacheEntity
import com.makerandreas.papirusoffice.data.cache.DocumentCacheRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class DocumentCacheRepositoryTest {

    private lateinit var dao: DocumentCacheDao
    private lateinit var repository: DocumentCacheRepository

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = DocumentCacheRepository(dao)
    }

    @Test
    fun invalidateCacheDeletesByPath() = runTest {
        val dummyFile = File("/tmp/test_doc.docx")
        repository.invalidateCache(dummyFile)

        coVerify(exactly = 1) { dao.deleteCacheByPath(dummyFile.absolutePath) }
    }

    @Test
    fun getRecentDocumentsDelegatesToDao() = runTest {
        val dummyList = listOf(
            DocumentCacheEntity(
                filePath = "/tmp/a.odt",
                fileName = "a.odt",
                fileSize = 1024L,
                lastModified = 1000L,
                format = "ODT",
                plainText = "Hello ODT"
            )
        )
        every { dao.getRecentCachedDocuments(10) } returns flowOf(dummyList)

        val result = repository.getRecentDocuments(10).first()

        assertEquals(1, result.size)
        assertEquals("/tmp/a.odt", result[0].filePath)
        assertEquals("ODT", result[0].format)
    }

    @Test
    fun performAutomatedCleanupPurgesExpiredAndCalculatesTotal() = runTest {
        // Setup mock return values for DAO purge queries
        coEvery { dao.clearOldCache(any()) } returns 3
        coEvery { dao.deleteLargeExpiredCache(any(), any()) } returns 2
        coEvery { dao.deleteFailedParseCache(any()) } returns 1
        coEvery { dao.deleteExcessOldCache(any()) } returns 4

        // Setup mock for orphaned / duplicate checks: empty list for this test
        coEvery { dao.getAllCachedDocuments() } returns emptyList()

        val cleanupResult = repository.performAutomatedCleanup()

        // Total should be 3 + 2 + 1 + 4 = 10
        assertEquals(10, cleanupResult.purgedCount)
        coVerify(exactly = 1) { dao.clearOldCache(any()) }
        coVerify(exactly = 1) { dao.deleteLargeExpiredCache(5 * 1024 * 1024L, any()) }
        coVerify(exactly = 1) { dao.deleteFailedParseCache(any()) }
        coVerify(exactly = 1) { dao.deleteExcessOldCache(100) }
    }

    @Test
    fun performAutomatedCleanupRemovesOrphanedEntries() = runTest {
        val nonExistentPath = "/path/to/non_existent_doc_12345.odt"
        val orphaned = DocumentCacheEntity(
            filePath = nonExistentPath,
            fileName = "non_existent.odt",
            fileSize = 2048L,
            lastModified = 1000L,
            format = "ODT",
            plainText = "Orphan"
        )

        coEvery { dao.clearOldCache(any()) } returns 0
        coEvery { dao.deleteLargeExpiredCache(any(), any()) } returns 0
        coEvery { dao.deleteFailedParseCache(any()) } returns 0
        coEvery { dao.deleteExcessOldCache(any()) } returns 0
        coEvery { dao.getAllCachedDocuments() } returns listOf(orphaned)

        val cleanupResult = repository.performAutomatedCleanup()

        assertTrue(cleanupResult.purgedCount >= 1)
        coVerify(atLeast = 1) { dao.deleteCacheByPath(nonExistentPath) }
    }

    @Test
    fun performAutomatedCleanupDetectsDuplicates() = runTest {
        // Create an existing temporary file so it is not treated as orphaned
        val tempFile1 = File.createTempFile("dup1", ".docx")
        val tempFile2 = File.createTempFile("dup2", ".docx")
        tempFile1.deleteOnExit()
        tempFile2.deleteOnExit()

        val entry1 = DocumentCacheEntity(
            filePath = tempFile1.absolutePath,
            fileName = "common_report.docx",
            fileSize = 4096L,
            lastModified = 2000L,
            lastOpenedTimestamp = 5000L,
            format = "DOCX",
            plainText = "Copy 1"
        )
        val entry2 = DocumentCacheEntity(
            filePath = tempFile2.absolutePath,
            fileName = "common_report.docx",
            fileSize = 4096L, // same size and name -> duplicate signature!
            lastModified = 1000L,
            lastOpenedTimestamp = 4000L,
            format = "DOCX",
            plainText = "Copy 2"
        )

        coEvery { dao.clearOldCache(any()) } returns 0
        coEvery { dao.deleteLargeExpiredCache(any(), any()) } returns 0
        coEvery { dao.deleteFailedParseCache(any()) } returns 0
        coEvery { dao.deleteExcessOldCache(any()) } returns 0
        coEvery { dao.getAllCachedDocuments() } returns listOf(entry1, entry2)

        val cleanupResult = repository.performAutomatedCleanup()

        // The duplicate entry2 should be deleted
        assertEquals(1, cleanupResult.purgedCount)
        coVerify(exactly = 1) { dao.deleteCacheByPath(tempFile2.absolutePath) }
    }
}
