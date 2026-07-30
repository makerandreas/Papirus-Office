package com.makerandreas.papirusoffice.core.fonts

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleFontsRepository(private val context: Context) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(GoogleFontsResponse::class.java)
    
    private var cachedFonts: List<GoogleFontMetadata> = emptyList()

    suspend fun getFontsList(): List<GoogleFontMetadata> = withContext(Dispatchers.IO) {
        if (cachedFonts.isNotEmpty()) return@withContext cachedFonts

        // Try to load from assets/fonts.json
        try {
            val inputStream = context.assets.open("fonts.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val response = adapter.fromJson(jsonString)
            cachedFonts = response?.items?.sortedBy { it.family } ?: emptyList()
            cachedFonts
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getDownloadableFonts(family: String): List<DownloadableFont> {
        val fonts = getFontsList()
        val font = fonts.find { it.family.equals(family, ignoreCase = true) } ?: return emptyList()
        
        val downloadables = mutableListOf<DownloadableFont>()
        font.files?.forEach { (variant, url) ->
            downloadables.add(DownloadableFont(font.family, variant, url, font.category))
        }
        return downloadables
    }
}
