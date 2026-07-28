package com.makerandreas.papirusoffice.data

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream

data class FontInfo(
    val fileName: String,
    val displayName: String,
    val familyName: String,
    val filePath: String,
    val extension: String,
    val isCompatible: Boolean = true
)

/**
 * FontProvider
 * Dynamically extracts fonts from 'app/src/main/assets/fonts' to '/storage/emulated/0/Fonts',
 * scans them at runtime, identifies compatible font files, and provides them to ViewModel/UI.
 */
object FontProvider {

    private const val TAG = "FontProvider"
    private const val ASSET_FONTS_DIR = "fonts"
    const val TARGET_FONTS_DIR_PATH = "/storage/emulated/0/Fonts"

    private val COMPATIBLE_EXTENSIONS = setOf("ttf", "otf", "woff", "woff2", "ttc")

    /**
     * Dynamically extracts fonts from app/src/main/assets/fonts to /storage/emulated/0/Fonts
     */
    fun extractFontsFromAssets(context: Context): File {
        val targetDir = File(TARGET_FONTS_DIR_PATH)
        if (!targetDir.exists()) {
            val created = targetDir.mkdirs()
            Log.d(TAG, "Directory $TARGET_FONTS_DIR_PATH created: $created")
        }

        // Primary destination is /storage/emulated/0/Fonts
        val destinationDir = if (targetDir.exists() && (targetDir.canWrite() || targetDir.exists())) {
            targetDir
        } else {
            // Internal/external fallback if strict security prevents writing to root storage
            val fallback = File(context.getExternalFilesDir(null), "Fonts")
            if (!fallback.exists()) fallback.mkdirs()
            fallback
        }

        try {
            val assetManager = context.assets
            val fontAssets = assetManager.list(ASSET_FONTS_DIR) ?: emptyArray()

            for (fileName in fontAssets) {
                if (isCompatibleFontFile(fileName)) {
                    val destFile = File(destinationDir, fileName)
                    if (!destFile.exists() || destFile.length() == 0L) {
                        try {
                            assetManager.open("$ASSET_FONTS_DIR/$fileName").use { input ->
                                FileOutputStream(destFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            Log.d(TAG, "Successfully extracted $fileName to ${destFile.absolutePath}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to copy $fileName to ${destFile.absolutePath}", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting asset fonts", e)
        }

        return destinationDir
    }

    /**
     * Identifies compatible font file extensions (.ttf, .otf, .woff, .woff2, .ttc)
     */
    fun isCompatibleFontFile(fileName: String): Boolean {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return ext in COMPATIBLE_EXTENSIONS
    }

    /**
     * Scans /storage/emulated/0/Fonts at runtime and identifies compatible font files
     */
    fun scanAvailableFonts(context: Context): List<FontInfo> {
        // Extract assets first to ensure /storage/emulated/0/Fonts is populated
        val activeFolder = extractFontsFromAssets(context)
        val fontMap = mutableMapOf<String, FontInfo>()

        // Helper closure to scan a directory
        fun scanDirectory(dir: File) {
            if (!dir.exists() || !dir.isDirectory) return
            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.isFile && isCompatibleFontFile(file.name)) {
                    val cleanName = file.nameWithoutExtension
                        .replace("-", " ")
                        .replace("_", " ")

                    val familyName = cleanName
                        .replace(" Regular", "", ignoreCase = true)
                        .replace(" Bold", "", ignoreCase = true)
                        .replace(" Italic", "", ignoreCase = true)
                        .replace(" BoldItalic", "", ignoreCase = true)
                        .trim()

                    val fontInfo = FontInfo(
                        fileName = file.name,
                        displayName = cleanName,
                        familyName = if (familyName.isNotEmpty()) familyName else cleanName,
                        filePath = file.absolutePath,
                        extension = file.extension.uppercase(),
                        isCompatible = true
                    )
                    fontMap[file.name] = fontInfo
                }
            }
        }

        // 1. Scan target directory (/storage/emulated/0/Fonts)
        scanDirectory(File(TARGET_FONTS_DIR_PATH))

        // 2. Scan active extracted folder if different
        if (activeFolder.absolutePath != TARGET_FONTS_DIR_PATH) {
            scanDirectory(activeFolder)
        }

        return fontMap.values.sortedBy { it.displayName }
    }
}
