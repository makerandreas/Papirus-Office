package com.makerandreas.papirusoffice.core.fonts

import java.io.File

class FontScanner(private val fontCacheManager: FontCacheManager) {

    fun scanLocalFonts(): List<File> {
        val dir = fontCacheManager.getLocalFontsDir()
        return dir.listFiles()?.filter { it.extension.lowercase() in listOf("ttf", "otf") } ?: emptyList()
    }
    
    fun scanGoogleFonts(): List<File> {
        val dir = fontCacheManager.getGoogleFontsDir()
        return dir.listFiles()?.filter { it.extension.lowercase() in listOf("ttf", "otf") } ?: emptyList()
    }
}
