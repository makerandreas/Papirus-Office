package com.makerandreas.papirusoffice.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Data models for Papirus Office Cloud API & Template fetching.
 */
@JsonClass(generateAdapter = true)
data class DocumentTemplate(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "category") val category: String,
    @Json(name = "moduleType") val moduleType: String, // "INKY", "CELLINA", "SLIDIA", "PAGELLA"
    @Json(name = "description") val description: String,
    @Json(name = "previewUrl") val previewUrl: String? = null,
    @Json(name = "downloadUrl") val downloadUrl: String? = null,
    @Json(name = "tags") val tags: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TemplateListResponse(
    @Json(name = "status") val status: String = "success",
    @Json(name = "total") val total: Int = 0,
    @Json(name = "templates") val templates: List<DocumentTemplate> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DocumentMetadataDto(
    @Json(name = "filePath") val filePath: String,
    @Json(name = "fileName") val fileName: String,
    @Json(name = "moduleType") val moduleType: String,
    @Json(name = "fileSizeBytes") val fileSizeBytes: Long,
    @Json(name = "lastModifiedTimestamp") val lastModifiedTimestamp: Long,
    @Json(name = "author") val author: String? = null
)

@JsonClass(generateAdapter = true)
data class SyncRequest(
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "deviceModel") val deviceModel: String,
    @Json(name = "documents") val documents: List<DocumentMetadataDto>
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    @Json(name = "status") val status: String,
    @Json(name = "syncedCount") val syncedCount: Int,
    @Json(name = "serverTimestamp") val serverTimestamp: Long,
    @Json(name = "message") val message: String
)
