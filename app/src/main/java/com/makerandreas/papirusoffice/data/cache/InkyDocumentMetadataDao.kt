package com.makerandreas.papirusoffice.data.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Inky document metadata stored in Room database.
 */
@Dao
interface InkyDocumentMetadataDao {

    @Query("SELECT * FROM inky_document_metadata WHERE filePath = :filePath LIMIT 1")
    suspend fun getMetadataByPath(filePath: String): InkyDocumentMetadataEntity?

    @Query("SELECT * FROM inky_document_metadata WHERE filePath = :filePath LIMIT 1")
    fun observeMetadataByPath(filePath: String): Flow<InkyDocumentMetadataEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMetadata(metadata: InkyDocumentMetadataEntity)

    @Query("SELECT * FROM inky_document_metadata ORDER BY lastModifiedAt DESC")
    fun getAllMetadata(): Flow<List<InkyDocumentMetadataEntity>>

    @Query("DELETE FROM inky_document_metadata WHERE filePath = :filePath")
    suspend fun deleteMetadataByPath(filePath: String)
}
