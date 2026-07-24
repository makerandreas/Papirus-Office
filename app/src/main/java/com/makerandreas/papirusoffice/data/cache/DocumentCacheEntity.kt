package com.makerandreas.papirusoffice.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity for caching document metadata and extracted text.
 * Allows faster loading for recently opened ODF/OOXML documents.
 */
@Entity(tableName = "document_cache")
data class DocumentCacheEntity(
    @PrimaryKey
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val lastModified: Long,
    val lastOpenedTimestamp: Long = System.currentTimeMillis(),
    val format: String,
    val plainText: String,
    val isParsingFailed: Boolean = false,
    val failureReason: String? = null
)
