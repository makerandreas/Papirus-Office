package com.makerandreas.papirusoffice.core.fonts

import android.content.Context
import android.os.Environment
import java.io.File

class FontCacheManager(private val context: Context) {

    private fun getBaseFontDir(): File {
        val root = File(Environment.getExternalStorageDirectory(), "Papirus Office/fonts")
        if (!root.exists()) {
            try {
                root.mkdirs()
            } catch (e: Exception) {
                // Fallback to app-specific directory if permission denied
                return File(context.getExternalFilesDir(null), "fonts")
            }
        }
        return root
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

    fun getCachedFontFile(family: String, variant: String): File? {
        val fileName = "${family.replace(" ", "")}-$variant.ttf"
        
        // Check local first (user provided overrides)
        val localFile = File(getLocalFontsDir(), fileName)
        if (localFile.exists()) return localFile

        // Check google fonts
        val googleFile = File(getGoogleFontsDir(), fileName)
        if (googleFile.exists()) return googleFile
        
        return null
    }

    fun getDestinationFileForGoogleFont(family: String, variant: String): File {
        val fileName = "${family.replace(" ", "")}-$variant.ttf"
        return File(getGoogleFontsDir(), fileName)
    }
}
