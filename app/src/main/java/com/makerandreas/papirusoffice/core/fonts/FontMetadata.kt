package com.makerandreas.papirusoffice.core.fonts

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Json

@JsonClass(generateAdapter = true)
data class GoogleFontMetadata(
    @Json(name = "family") val family: String,
    @Json(name = "variants") val variants: List<String>,
    @Json(name = "subsets") val subsets: List<String>,
    @Json(name = "version") val version: String? = null,
    @Json(name = "lastModified") val lastModified: String? = null,
    @Json(name = "files") val files: Map<String, String>? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "kind") val kind: String? = null,
    @Json(name = "popularity") val popularity: Int? = null
)

@JsonClass(generateAdapter = true)
data class GoogleFontsResponse(
    @Json(name = "kind") val kind: String? = null,
    @Json(name = "items") val items: List<GoogleFontMetadata>
)

data class DownloadableFont(
    val family: String,
    val variant: String,
    val url: String,
    val category: String?
)
