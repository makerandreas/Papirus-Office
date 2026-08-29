package com.makerandreas.papirusoffice.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data models for Papirus Office Cloud API & Template fetching.
 */
@JsonClass(generateAdapter = true)
data class DocumentTemplate(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "title") val title: String,
    @field:Json(name = "category") val category: String,
    @field:Json(name = "moduleType") val moduleType: String, // "INKY", "CELLINA", "SLIDIA", "PAGELLA"
    @field:Json(name = "description") val description: String,
    @field:Json(name = "previewUrl") val previewUrl: String? = null,
    @field:Json(name = "downloadUrl") val downloadUrl: String? = null,
    @field:Json(name = "tags") val tags: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TemplateListResponse(
    @field:Json(name = "status") val status: String = "success",
    @field:Json(name = "total") val total: Int = 0,
    @field:Json(name = "templates") val templates: List<DocumentTemplate> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DocumentMetadataDto(
    @field:Json(name = "filePath") val filePath: String,
    @field:Json(name = "fileName") val fileName: String,
    @field:Json(name = "moduleType") val moduleType: String,
    @field:Json(name = "fileSizeBytes") val fileSizeBytes: Long,
    @field:Json(name = "lastModifiedTimestamp") val lastModifiedTimestamp: Long,
    @field:Json(name = "author") val author: String? = null
)

@JsonClass(generateAdapter = true)
data class SyncRequest(
    @field:Json(name = "deviceId") val deviceId: String,
    @field:Json(name = "deviceModel") val deviceModel: String,
    @field:Json(name = "documents") val documents: List<DocumentMetadataDto>
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    @field:Json(name = "status") val status: String,
    @field:Json(name = "syncedCount") val syncedCount: Int,
    @field:Json(name = "serverTimestamp") val serverTimestamp: Long,
    @field:Json(name = "message") val message: String
)
