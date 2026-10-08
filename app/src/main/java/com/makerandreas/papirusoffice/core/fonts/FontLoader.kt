package com.makerandreas.papirusoffice.core.fonts

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import java.io.File

class FontLoader {
    fun loadFontFamily(fontFiles: List<Pair<File, String>>): FontFamily? {
        if (fontFiles.isEmpty()) return null

        // Build FontFamily using regular style or primary font file.
        val regularFile = fontFiles.find { it.second == "regular" }?.first ?: fontFiles.first().first
        
        return try {
            val typeface = Typeface.createFromFile(regularFile)
            FontFamily(typeface)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
