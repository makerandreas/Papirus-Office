package com.makerandreas.papirusoffice.core.fonts

import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FontSyncService(private val context: Context) {
    private val repository = GoogleFontsRepository(context)
    private val downloadManager = FontDownloadManager()
    private val cacheManager = FontCacheManager(context)
    private val loader = FontLoader()

    suspend fun getFontFamily(familyName: String): FontFamily? = withContext(Dispatchers.IO) {
        // Find metadata
        val fonts = repository.getFontsList()
        val meta = fonts.find { it.family.equals(familyName, ignoreCase = true) } ?: return@withContext null

        val availableFiles = mutableListOf<Pair<java.io.File, String>>()
        
        // Ensure "regular" or fallback variant exists
        val variantsToFetch = if (meta.variants.contains("regular")) listOf("regular") else meta.variants.take(1)
        
        // Also fetch 700, italic if we want, but for now let's just get what is requested
        for (variant in meta.variants) {
            val file = cacheManager.getCachedFontFile(meta.family, variant)
            if (file != null) {
                availableFiles.add(file to variant)
            } else {
                // If not cached, let's download it if it's in the files map
                val downloadUrl = meta.files?.get(variant)
                if (downloadUrl != null) {
                    val dest = cacheManager.getDestinationFileForGoogleFont(meta.family, variant)
                    val success = downloadManager.downloadFont(downloadUrl, dest)
                    if (success) {
                        availableFiles.add(dest to variant)
                    }
                }
            }
        }

        if (availableFiles.isEmpty()) return@withContext null

        loader.loadFontFamily(availableFiles)
    }
}
