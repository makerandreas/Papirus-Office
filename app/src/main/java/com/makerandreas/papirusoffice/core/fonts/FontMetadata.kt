package com.makerandreas.papirusoffice.core.fonts

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

@JsonClass(generateAdapter = true)
data class GoogleFontMetadata(
    @field:Json(name = "family") val family: String,
    @field:Json(name = "variants") val variants: List<String>,
    @field:Json(name = "subsets") val subsets: List<String>,
    @field:Json(name = "version") val version: String? = null,
    @field:Json(name = "lastModified") val lastModified: String? = null,
    @field:Json(name = "files") val files: Map<String, String>? = null,
    @field:Json(name = "category") val category: String? = null,
    @field:Json(name = "kind") val kind: String? = null,
    @field:Json(name = "popularity") val popularity: Int? = null
)

@JsonClass(generateAdapter = true)
data class GoogleFontsResponse(
    @field:Json(name = "kind") val kind: String? = null,
    @field:Json(name = "items") val items: List<GoogleFontMetadata>
)

data class DownloadableFont(
    val family: String,
    val variant: String,
    val url: String,
    val category: String?
)
