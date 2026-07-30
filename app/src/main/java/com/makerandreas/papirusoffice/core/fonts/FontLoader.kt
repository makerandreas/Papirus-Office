package com.makerandreas.papirusoffice.core.fonts

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle

class FontLoader {
    fun loadFontFamily(fontFiles: List<Pair<File, String>>): FontFamily? {
        if (fontFiles.isEmpty()) return null

        // In a real app we'd load each font weight/style.
        // For simplicity and to avoid compose API version issues,
        // let's just create a Typeface from the "regular" or first file
        // and return a FontFamily from it.
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
