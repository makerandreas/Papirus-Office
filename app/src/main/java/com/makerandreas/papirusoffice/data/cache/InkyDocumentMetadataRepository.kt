package com.makerandreas.papirusoffice.data.cache

import kotlinx.coroutines.flow.Flow

/**
 * Repository pattern implementation for managing Inky document metadata operations in Room DB.
 */
class InkyDocumentMetadataRepository(private val dao: InkyDocumentMetadataDao) {

    suspend fun getMetadata(filePath: String): InkyDocumentMetadataEntity? {
        return dao.getMetadataByPath(filePath)
    }

    fun observeMetadata(filePath: String): Flow<InkyDocumentMetadataEntity?> {
        return dao.observeMetadataByPath(filePath)
    }

    suspend fun saveOrUpdateMetadata(metadata: InkyDocumentMetadataEntity) {
        dao.insertOrUpdateMetadata(metadata)
    }

    suspend fun deleteMetadata(filePath: String) {
        dao.deleteMetadataByPath(filePath)
    }

    fun getAllMetadata(): Flow<List<InkyDocumentMetadataEntity>> {
        return dao.getAllMetadata()
    }
}
