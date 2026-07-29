package com.makerandreas.papirusoffice.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing persistent document metadata for Inky, Cellina, Slidia, and Pagella.
 */
@Entity(tableName = "document_metadata")
data class DocumentMetadata(
    @PrimaryKey
    val filePath: String,
    val fileName: String,
    val moduleType: String, // "INKY", "CELLINA", "SLIDIA", "PAGELLA"
    val fileSizeBytes: Long,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val pageOrSheetCount: Int = 1,
    val isFavorite: Boolean = false,
    val author: String? = "Papirus User",
    val summary: String? = null,
    val lastSyncedAt: Long = 0L
)
