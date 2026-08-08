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

        // Cache validation: check if file size or last modified timestamp changed externally, or if parsing previously failed
        if (cache.lastModified != file.lastModified() || cache.fileSize != file.length() || cache.isParsingFailed) {
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

    /**
     * Performs cleanup of expired, duplicate, or orphaned metadata entries in Room
     * for ODT/DOCX, ODS/XLSX, and ODP/PPTX files.
     */
    suspend fun performAutomatedCleanup(): CleanupResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        var purgedCount = 0

        // 1. Purge entries older than 7 days
        val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000L)
        purgedCount += cacheDao.clearOldCache(sevenDaysAgo)

        // 2. Purge large file cache (> 5 MB) older than 3 days
        val threeDaysAgo = now - (3 * 24 * 60 * 60 * 1000L)
        val minLargeSizeBytes = 5 * 1024 * 1024L
        purgedCount += cacheDao.deleteLargeExpiredCache(minLargeSizeBytes, threeDaysAgo)

        // 3. Purge failed parsing cache older than 24 hours
        val oneDayAgo = now - (24 * 60 * 60 * 1000L)
        purgedCount += cacheDao.deleteFailedParseCache(oneDayAgo)

        // 4. Purge orphaned entries where the underlying file no longer exists
        val allEntries = cacheDao.getAllCachedDocuments()
        for (entry in allEntries) {
            val file = File(entry.filePath)
            if (!file.exists()) {
                cacheDao.deleteCacheByPath(entry.filePath)
                purgedCount++
            }
        }

        // 5. Purge duplicate entries (same file name and file size, retain the most recently opened)
        val remaining = cacheDao.getAllCachedDocuments()
        val seenSignatures = mutableSetOf<String>()
        for (entry in remaining) {
            val signature = "${entry.fileName}_${entry.fileSize}"
            if (seenSignatures.contains(signature)) {
                cacheDao.deleteCacheByPath(entry.filePath)
                purgedCount++
            } else {
                seenSignatures.add(signature)
            }
        }

        // 6. Enforce maximum capacity limit (keep top 100 entries)
        purgedCount += cacheDao.deleteExcessOldCache(keepCount = 100)

        return@withContext CleanupResult(purgedCount = purgedCount)
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
