package com.example

import com.makerandreas.papirusoffice.data.cache.InkyDocumentMetadataDao
import com.makerandreas.papirusoffice.data.cache.InkyDocumentMetadataEntity
import com.makerandreas.papirusoffice.data.cache.InkyDocumentMetadataRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class InkyDocumentMetadataRepositoryTest {

    private lateinit var dao: InkyDocumentMetadataDao
    private lateinit var repository: InkyDocumentMetadataRepository

    @Before
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = InkyDocumentMetadataRepository(dao)
    }

    @Test
    fun getMetadataReturnsMatchingEntity() = runTest {
        val sample = InkyDocumentMetadataEntity(
            filePath = "/sdcard/test.odt",
            fileName = "test.odt",
            wordCount = 150,
            characterCount = 850
        )
        coEvery { dao.getMetadataByPath("/sdcard/test.odt") } returns sample

        val result = repository.getMetadata("/sdcard/test.odt")

        assertEquals(sample, result)
        assertEquals(150, result?.wordCount)
        coVerify(exactly = 1) { dao.getMetadataByPath("/sdcard/test.odt") }
    }

    @Test
    fun getMetadataReturnsNullWhenNotFound() = runTest {
        coEvery { dao.getMetadataByPath(any()) } returns null

        val result = repository.getMetadata("/non/existent.odt")

        assertNull(result)
        coVerify(exactly = 1) { dao.getMetadataByPath("/non/existent.odt") }
    }

    @Test
    fun saveOrUpdateMetadataDelegatesToDao() = runTest {
        val entity = InkyDocumentMetadataEntity(
            filePath = "/docs/notes.docx",
            fileName = "notes.docx",
            wordCount = 42,
            characterCount = 200
        )

        repository.saveOrUpdateMetadata(entity)

        coVerify(exactly = 1) { dao.insertOrUpdateMetadata(entity) }
    }

    @Test
    fun deleteMetadataDelegatesToDao() = runTest {
        repository.deleteMetadata("/docs/obsolete.odt")

        coVerify(exactly = 1) { dao.deleteMetadataByPath("/docs/obsolete.odt") }
    }

    @Test
    fun observeMetadataEmitsFromDaoFlow() = runTest {
        val entity = InkyDocumentMetadataEntity(
            filePath = "/flow/doc.odt",
            fileName = "doc.odt",
            wordCount = 500,
            characterCount = 3000
        )
        every { dao.observeMetadataByPath("/flow/doc.odt") } returns flowOf(entity)

        val emitted = repository.observeMetadata("/flow/doc.odt").first()

        assertEquals(entity, emitted)
        assertEquals(500, emitted?.wordCount)
    }

    @Test
    fun getAllMetadataReturnsFlowFromDao() = runTest {
        val list = listOf(
            InkyDocumentMetadataEntity(filePath = "/a.odt", fileName = "a.odt", wordCount = 10),
            InkyDocumentMetadataEntity(filePath = "/b.odt", fileName = "b.odt", wordCount = 20)
        )
        every { dao.getAllMetadata() } returns flowOf(list)

        val result = repository.getAllMetadata().first()

        assertEquals(2, result.size)
        assertEquals("/a.odt", result[0].filePath)
        assertEquals("/b.odt", result[1].filePath)
    }
}
