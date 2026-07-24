package com.makerandreas.papirusoffice.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Room document caching operations.
 */
@Dao
interface DocumentCacheDao {

    @Query("SELECT * FROM document_cache WHERE filePath = :filePath LIMIT 1")
    suspend fun getCacheByPath(filePath: String): DocumentCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCache(cache: DocumentCacheEntity)

    @Query("SELECT * FROM document_cache ORDER BY lastOpenedTimestamp DESC LIMIT :limit")
    fun getRecentCachedDocuments(limit: Int = 20): Flow<List<DocumentCacheEntity>>

    @Query("SELECT * FROM document_cache ORDER BY lastOpenedTimestamp DESC")
    suspend fun getAllCachedDocuments(): List<DocumentCacheEntity>

    @Query("DELETE FROM document_cache WHERE filePath = :filePath")
    suspend fun deleteCacheByPath(filePath: String)

    @Query("DELETE FROM document_cache WHERE lastOpenedTimestamp < :thresholdTimestamp")
    suspend fun clearOldCache(thresholdTimestamp: Long): Int

    @Query("DELETE FROM document_cache WHERE fileSize > :minSizeBytes AND lastOpenedTimestamp < :thresholdTimestamp")
    suspend fun deleteLargeExpiredCache(minSizeBytes: Long, thresholdTimestamp: Long): Int

    @Query("DELETE FROM document_cache WHERE isParsingFailed = 1 AND lastOpenedTimestamp < :thresholdTimestamp")
    suspend fun deleteFailedParseCache(thresholdTimestamp: Long): Int

    @Query("DELETE FROM document_cache WHERE filePath NOT IN (SELECT filePath FROM document_cache ORDER BY lastOpenedTimestamp DESC LIMIT :keepCount)")
    suspend fun deleteExcessOldCache(keepCount: Int): Int

    @Query("DELETE FROM document_cache")
    suspend fun clearAllCache()
}
