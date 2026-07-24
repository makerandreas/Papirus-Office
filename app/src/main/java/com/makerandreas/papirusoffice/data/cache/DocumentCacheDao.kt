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

    @Query("DELETE FROM document_cache WHERE filePath = :filePath")
    suspend fun deleteCacheByPath(filePath: String)

    @Query("DELETE FROM document_cache WHERE lastOpenedTimestamp < :thresholdTimestamp")
    suspend fun clearOldCache(thresholdTimestamp: Long)

    @Query("DELETE FROM document_cache")
    suspend fun clearAllCache()
}
