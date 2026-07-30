package com.makerandreas.papirusoffice.data

import android.content.Context
import android.util.Log
import com.makerandreas.papirusoffice.data.framework.PathSettings
import java.io.File
import java.io.FileOutputStream

/**
 * PapirusAssetEngine
 * Connects Android assets (/app/src/main/assets/) to Papirus Office Engine subsystems:
 * 1. Fonts (/app/src/main/assets/fonts)
 * 2. Hunspell Dictionary (/app/src/main/assets/hunspell)
 * 3. Hyphenation (/app/src/main/assets/hyphenation)
 */
object PapirusAssetEngine {

    private const val TAG = "PapirusAssetEngine"

    const val ASSETS_FONTS_DIR = "fonts"
    const val ASSETS_HUNSPELL_DIR = "hunspell"
    const val ASSETS_HYPHENATION_DIR = "hyphenation"
    const val ASSETS_SHARE_DIR = "share"
    const val ASSETS_PROGRAM_DIR = "program"
    const val ASSETS_UNPACK_DIR = "unpack"
    const val ASSETS_DEXOPT_DIR = "dexopt"

    data class FontItem(
        val fileName: String,
        val displayName: String,
        val familyName: String,
        val assetPath: String,
        val localFilePath: String?
    )

    private var isConnected = false
    private var pathSettings: PathSettings? = null

    /**
     * Connects all assets from app/src/main/assets/ to Papirus Engine
     */
    @Synchronized
    fun initialize(context: Context) {
        if (isConnected) return

        try {
            val internalAssetsDir = File(context.filesDir, "papirus_assets")
            if (!internalAssetsDir.exists()) {
                internalAssetsDir.mkdirs()
            }

            // Copy assets to internal storage if needed for native file-path engine access
            syncAssetFolderToStorage(context, ASSETS_FONTS_DIR, File(internalAssetsDir, "fonts"))
            syncAssetFolderToStorage(context, ASSETS_HUNSPELL_DIR, File(internalAssetsDir, "hunspell"))
            syncAssetFolderToStorage(context, ASSETS_HYPHENATION_DIR, File(internalAssetsDir, "hyphenation"))
            syncAssetFolderToStorage(context, ASSETS_SHARE_DIR, File(internalAssetsDir, "share"))
            syncAssetFolderToStorage(context, ASSETS_PROGRAM_DIR, File(internalAssetsDir, "program"))
            syncAssetFolderToStorage(context, ASSETS_UNPACK_DIR, File(internalAssetsDir, "unpack"))
            syncAssetFolderToStorage(context, ASSETS_DEXOPT_DIR, File(internalAssetsDir, "dexopt"))

            // Initialize and configure PathSettings
            pathSettings = PathSettings(context).apply {
                setPropertyValue("Font", File(internalAssetsDir, "fonts").absolutePath)
                setPropertyValue("Linguistic", File(internalAssetsDir, "hunspell").absolutePath)
                setPropertyValue("Dictionary", File(internalAssetsDir, "hunspell/en-US").absolutePath)
                setPropertyValue("Hyphenation", File(internalAssetsDir, "hyphenation").absolutePath)
                setPropertyValue("Share", File(internalAssetsDir, "share").absolutePath)
                setPropertyValue("Program", File(internalAssetsDir, "program").absolutePath)
                setPropertyValue("Unpack", File(internalAssetsDir, "unpack").absolutePath)
            }

            isConnected = true
            Log.d(TAG, "PapirusAssetEngine connected successfully. Assets synced to ${internalAssetsDir.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing PapirusAssetEngine", e)
        }
    }

    /**
     * Recursively copies asset files to local file storage if not present or changed.
     */
    private fun syncAssetFolderToStorage(context: Context, assetPath: String, targetDir: File) {
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val assetManager = context.assets
        try {
            val list = assetManager.list(assetPath) ?: return
            for (fileOrDir in list) {
                val subAssetPath = if (assetPath.isEmpty()) fileOrDir else "$assetPath/$fileOrDir"
                val subList = assetManager.list(subAssetPath)
                val destFile = File(targetDir, fileOrDir)

                if (!subList.isNullOrEmpty()) {
                    // It's a directory
                    syncAssetFolderToStorage(context, subAssetPath, destFile)
                } else {
                    // It's a file
                    if (!destFile.exists() || destFile.length() == 0L) {
                        try {
                            assetManager.open(subAssetPath).use { input ->
                                FileOutputStream(destFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            Log.d(TAG, "Copied asset $subAssetPath to ${destFile.absolutePath}")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to copy asset $subAssetPath", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing asset folder $assetPath", e)
        }
    }

    /**
     * Dynamically reads all available font files from app/src/main/assets/fonts
     */
    fun getAvailableFonts(context: Context): List<FontItem> {
        val fontsList = mutableListOf<FontItem>()
        val internalFontsDir = File(context.filesDir, "papirus_assets/fonts")

        try {
            val assetFiles = context.assets.list(ASSETS_FONTS_DIR) ?: emptyArray()
            val fontFiles = assetFiles.filter { it.endsWith(".ttf", ignoreCase = true) || it.endsWith(".otf", ignoreCase = true) }

            fontFiles.forEach { fileName ->
                val cleanName = fileName.substringBeforeLast(".")
                    .replace("-", " ")
                    .replace("_", " ")

                val familyName = cleanName
                    .replace(" Regular", "", ignoreCase = true)
                    .replace(" Bold", "", ignoreCase = true)
                    .replace(" Italic", "", ignoreCase = true)
                    .replace(" BoldItalic", "", ignoreCase = true)
                    .trim()

                val localFile = File(internalFontsDir, fileName)
                val localPath = if (localFile.exists()) localFile.absolutePath else null

                fontsList.add(
                    FontItem(
                        fileName = fileName,
                        displayName = cleanName,
                        familyName = if (familyName.isNotEmpty()) familyName else cleanName,
                        assetPath = "$ASSETS_FONTS_DIR/$fileName",
                        localFilePath = localPath
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available fonts from assets", e)
        }

        return fontsList
    }

    fun getHunspellDirectory(context: Context): File {
        initialize(context)
        return File(context.filesDir, "papirus_assets/hunspell")
    }

    fun getHyphenationDirectory(context: Context): File {
        initialize(context)
        return File(context.filesDir, "papirus_assets/hyphenation")
    }
}
