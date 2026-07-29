package com.makerandreas.papirusoffice.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for Papirus Office document metadata persistence.
 */
@Dao
interface DocumentMetadataDao {

    @Query("SELECT * FROM document_metadata ORDER BY lastModifiedTimestamp DESC")
    fun getAllMetadata(): Flow<List<DocumentMetadata>>

    @Query("SELECT * FROM document_metadata WHERE moduleType = :moduleType ORDER BY lastModifiedTimestamp DESC")
    fun getMetadataByModule(moduleType: String): Flow<List<DocumentMetadata>>

    @Query("SELECT * FROM document_metadata WHERE filePath = :filePath LIMIT 1")
    suspend fun getMetadataByPath(filePath: String): DocumentMetadata?

    @Query("SELECT * FROM document_metadata WHERE isFavorite = 1 ORDER BY lastModifiedTimestamp DESC")
    fun getFavorites(): Flow<List<DocumentMetadata>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: DocumentMetadata)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(metadataList: List<DocumentMetadata>)

    @Update
    suspend fun update(metadata: DocumentMetadata)

    @Delete
    suspend fun delete(metadata: DocumentMetadata)

    @Query("DELETE FROM document_metadata WHERE filePath = :filePath")
    suspend fun deleteByPath(filePath: String)

    @Query("DELETE FROM document_metadata")
    suspend fun clearAll()
}
