package com.makerandreas.papirusoffice.data.db

import kotlinx.coroutines.flow.Flow

/**
 * Repository pattern implementation for DocumentMetadata.
 */
class DocumentMetadataRepository(private val dao: DocumentMetadataDao) {

    val allMetadata: Flow<List<DocumentMetadata>> = dao.getAllMetadata()
    val favorites: Flow<List<DocumentMetadata>> = dao.getFavorites()

    fun getMetadataByModule(moduleType: String): Flow<List<DocumentMetadata>> {
        return dao.getMetadataByModule(moduleType)
    }

    suspend fun getMetadataByPath(filePath: String): DocumentMetadata? {
        return dao.getMetadataByPath(filePath)
    }

    suspend fun insertOrUpdate(metadata: DocumentMetadata) {
        dao.insert(metadata)
    }

    suspend fun insertAll(list: List<DocumentMetadata>) {
        dao.insertAll(list)
    }

    suspend fun delete(metadata: DocumentMetadata) {
        dao.delete(metadata)
    }

    suspend fun deleteByPath(filePath: String) {
        dao.deleteByPath(filePath)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
