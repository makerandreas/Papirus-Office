package com.makerandreas.papirusoffice.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity for storing Inky ODF/OOXML document metadata.
 * Stores creation date, last modified date, author, word count, character count, and paragraph count.
 */
@Entity(tableName = "inky_document_metadata")
data class InkyDocumentMetadataEntity(
    @PrimaryKey
    val filePath: String,
    val fileName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis(),
    val author: String = "Papirus Office User",
    val wordCount: Int = 0,
    val characterCount: Int = 0,
    val paragraphCount: Int = 0,
    val fileType: String = "ODT"
)
