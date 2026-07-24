package com.makerandreas.papirusoffice.data.cache

import android.content.Context
import com.makerandreas.papirusoffice.data.OfficeParsedDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Repository layer managing local Room document caching operations.
 * Validates file timestamp and size to prevent stale cache hits.
 */
class DocumentCacheRepository(context: Context) {

    private val cacheDao = DocumentDatabase.getInstance(context).documentCacheDao()

    suspend fun getCachedDocument(file: File): DocumentCacheEntity? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null

        val cache = cacheDao.getCacheByPath(file.absolutePath) ?: return@withContext null

        // Cache validation: check if file size or last modified timestamp changed externally
        if (cache.lastModified != file.lastModified() || cache.fileSize != file.length()) {
            cacheDao.deleteCacheByPath(file.absolutePath)
            return@withContext null
        }

        // Update last opened timestamp
        val updated = cache.copy(lastOpenedTimestamp = System.currentTimeMillis())
        cacheDao.insertOrUpdateCache(updated)
        return@withContext updated
    }

    suspend fun saveCachedDocument(file: File, parsedDoc: OfficeParsedDocument) = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext

        val entity = DocumentCacheEntity(
            filePath = file.absolutePath,
            fileName = file.name,
            fileSize = file.length(),
            lastModified = file.lastModified(),
            lastOpenedTimestamp = System.currentTimeMillis(),
            format = getDocumentFormat(file),
            plainText = parsedDoc.plainText,
            isParsingFailed = parsedDoc.isParsingFailed,
            failureReason = parsedDoc.failureReason
        )
        cacheDao.insertOrUpdateCache(entity)
    }

    suspend fun saveCachedDocument(file: File, text: String) = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext

        val entity = DocumentCacheEntity(
            filePath = file.absolutePath,
            fileName = file.name,
            fileSize = file.length(),
            lastModified = file.lastModified(),
            lastOpenedTimestamp = System.currentTimeMillis(),
            format = getDocumentFormat(file),
            plainText = text,
            isParsingFailed = false,
            failureReason = null
        )
        cacheDao.insertOrUpdateCache(entity)
    }

    suspend fun invalidateCache(file: File) = withContext(Dispatchers.IO) {
        cacheDao.deleteCacheByPath(file.absolutePath)
    }

    fun getRecentDocuments(limit: Int = 20): Flow<List<DocumentCacheEntity>> {
        return cacheDao.getRecentCachedDocuments(limit)
    }

    private fun getDocumentFormat(file: File): String {
        val name = file.name.lowercase()
        return when {
            name.endsWith(".docx") || name.endsWith(".docm") -> "DOCX"
            name.endsWith(".odt") -> "ODT"
            name.endsWith(".ods") -> "ODS"
            name.endsWith(".xlsx") || name.endsWith(".xlsm") -> "XLSX"
            name.endsWith(".pptx") -> "PPTX"
            name.endsWith(".odp") -> "ODP"
            else -> "UNKNOWN"
        }
    }
}
