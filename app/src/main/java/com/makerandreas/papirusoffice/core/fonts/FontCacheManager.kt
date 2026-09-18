package com.makerandreas.papirusoffice.core.fonts

import android.content.Context
import android.util.Log
import java.io.File

class FontCacheManager(private val context: Context) {

    companion object {
        private const val TAG = "FontCacheManager"
    }

    /**
     * App-private font directory. Uses app-specific external storage when
     * available (survives longer than cache) and internal storage otherwise.
     * Never touches shared/external storage roots, so no storage permission
     * is required.
     */
    private fun getBaseFontDir(): File {
        val candidates = listOfNotNull(
            context.getExternalFilesDir("fonts"),
            File(context.filesDir, "fonts")
        )
        for (dir in candidates) {
            try {
                if (dir.exists() || dir.mkdirs()) return dir
            } catch (e: Exception) {
                Log.w(TAG, "Cannot use font dir ${dir.absolutePath}: ${e.message}")
            }
        }
        // Last resort: a File object pointing at internal storage even if
        // creation failed; callers handle write failures gracefully.
        return File(context.filesDir, "fonts")
    }

    fun getGoogleFontsDir(): File {
        val dir = File(getBaseFontDir(), "google")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getLocalFontsDir(): File {
        val dir = File(getBaseFontDir(), "local")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Builds a traversal-safe file name from network-supplied font metadata.
     * Family/variant strings come from the Google Fonts API and must never be
     * able to escape the font directory via "/" or "..".
     */
    private fun safeFontFileName(family: String, variant: String): String {
        val safeFamily = family.replace("[^A-Za-z0-9_-]".toRegex(), "").take(80)
            .ifEmpty { "font" }
        val safeVariant = variant.replace("[^A-Za-z0-9_-]".toRegex(), "").take(40)
            .ifEmpty { "Regular" }
        return "$safeFamily-$safeVariant.ttf"
    }

    fun getCachedFontFile(family: String, variant: String): File? {
        val fileName = safeFontFileName(family, variant)

        // Check local first (user provided overrides)
        val localFile = File(getLocalFontsDir(), fileName)
        if (localFile.exists()) return localFile

        // Check google fonts
        val googleFile = File(getGoogleFontsDir(), fileName)
        if (googleFile.exists()) return googleFile

        return null
    }

    fun getDestinationFileForGoogleFont(family: String, variant: String): File {
        return File(getGoogleFontsDir(), safeFontFileName(family, variant))
    }
}
