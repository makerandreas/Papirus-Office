package com.makerandreas.papirusoffice.data.bridge

import android.content.Context
import android.util.Log
import com.makerandreas.papirusoffice.data.FontInfo
import com.makerandreas.papirusoffice.data.FontProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class GoogleFontMetadata(
    val family: String,
    val category: String, // sans-serif, serif, display, handwriting, monospace
    val downloadUrl: String,
    val previewUrl: String? = null,
    val variants: List<String> = listOf("regular", "bold", "italic"),
    var isDownloaded: Boolean = false,
    var localPath: String? = null
)

/**
 * Service Layer Bridge for Google Fonts database integration.
 * Provides searching, background downloading, and seamless linking with Papirus FontProvider.
 */
class GoogleFontsBridge private constructor() {

    private val TAG = "GoogleFontsBridge"

    private val _availableFonts = MutableStateFlow<List<GoogleFontMetadata>>(emptyList())
    val availableFonts: StateFlow<List<GoogleFontMetadata>> = _availableFonts.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    init {
        loadCatalog()
    }

    /**
     * Pre-populates catalog with popular open-source Google Fonts
     */
    private fun loadCatalog() {
        val catalog = listOf(
            GoogleFontMetadata(
                family = "Roboto",
                category = "sans-serif",
                downloadUrl = "https://fonts.gstatic.com/s/roboto/v30/KFOmCnqEu92Fr1Mu4mxK.ttf"
            ),
            GoogleFontMetadata(
                family = "Open Sans",
                category = "sans-serif",
                downloadUrl = "https://fonts.gstatic.com/s/opensans/v35/memSYaGs126MiZpBA-UvWbX2vVnXBbObj2OVZyOOSr4dVJWUgsjZ0C4nY1M2xLER.ttf"
            ),
            GoogleFontMetadata(
                family = "Lato",
                category = "sans-serif",
                downloadUrl = "https://fonts.gstatic.com/s/lato/v24/S6uyw4BMUTPHjx4wXg.ttf"
            ),
            GoogleFontMetadata(
                family = "Montserrat",
                category = "sans-serif",
                downloadUrl = "https://fonts.gstatic.com/s/montserrat/v25/JTUHjIg1_i6t8kCHKm453WzAmdDwZB3j.ttf"
            ),
            GoogleFontMetadata(
                family = "Inter",
                category = "sans-serif",
                downloadUrl = "https://fonts.gstatic.com/s/inter/v12/UcCO3FwrK3iLTeHuS_fvQtMwCp50KnMw2boKoduKmMEVuLyfAZ9hiA.ttf"
            ),
            GoogleFontMetadata(
                family = "Merriweather",
                category = "serif",
                downloadUrl = "https://fonts.gstatic.com/s/merriweather/v30/u-440qyriQwlOrhSvowK_l5-fCZM.ttf"
            ),
            GoogleFontMetadata(
                family = "Playfair Display",
                category = "serif",
                downloadUrl = "https://fonts.gstatic.com/s/playfairdisplay/v30/nuFvD-vYSZviVYUb_RJ3ijVRyeA6nJ6b.ttf"
            ),
            GoogleFontMetadata(
                family = "Fira Code",
                category = "monospace",
                downloadUrl = "https://fonts.gstatic.com/s/firacode/v21/uNdpd343fvc2yb_52-C1Xz_C.ttf"
            ),
            GoogleFontMetadata(
                family = "Oswald",
                category = "sans-serif",
                downloadUrl = "https://fonts.gstatic.com/s/oswald/v49/TK3iWkUHHAIjg752GT8G.ttf"
            ),
            GoogleFontMetadata(
                family = "Poppins",
                category = "sans-serif",
                downloadUrl = "https://fonts.gstatic.com/s/poppins/v20/pxiBYP8kv8JHgFVrLDz8Z1xlFQ.ttf"
            ),
            GoogleFontMetadata(
                family = "Roboto Flex",
                category = "sans-serif",
                downloadUrl = "https://fonts.gstatic.com/s/robotoflex/v18/raA9HI23-u1_O3M20N2z1x_4O3A.ttf"
            )
        )
        _availableFonts.value = catalog
    }

    /**
     * Checks existing fonts in /storage/emulated/0/Fonts and updates status
     */
    fun syncWithInstalledFonts(context: Context) {
        val installedFonts = FontProvider.scanAvailableFonts(context)
        val fontFamiliesInstalled = installedFonts.map { it.familyName.lowercase() }.toSet()

        val updatedList = _availableFonts.value.map { font ->
            val isInst = fontFamiliesInstalled.contains(font.family.lowercase())
            font.copy(
                isDownloaded = isInst,
                localPath = if (isInst) "${FontProvider.TARGET_FONTS_DIR_PATH}/${font.family.replace(" ", "")}.ttf" else null
            )
        }
        _availableFonts.value = updatedList
    }

    /**
     * Downloads a font in the background without locking the UI
     */
    suspend fun downloadFont(context: Context, font: GoogleFontMetadata): Boolean = withContext(Dispatchers.IO) {
        _isDownloading.value = true
        try {
            val targetDir = File(FontProvider.TARGET_FONTS_DIR_PATH)
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            val destinationDir = if (targetDir.exists() && targetDir.canWrite()) {
                targetDir
            } else {
                File(context.getExternalFilesDir(null), "Fonts").also { if (!it.exists()) it.mkdirs() }
            }

            val fileName = "${font.family.replace(" ", "")}.ttf"
            val destFile = File(destinationDir, fileName)

            Log.d(TAG, "Starting download for font ${font.family} from ${font.downloadUrl} to ${destFile.absolutePath}")

            val url = URL(font.downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d(TAG, "Successfully downloaded ${font.family} font to ${destFile.absolutePath}")

                // Sync fonts again
                syncWithInstalledFonts(context)
                _isDownloading.value = false
                return@withContext true
            } else {
                Log.e(TAG, "Font download failed with response code ${connection.responseCode}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception downloading font ${font.family}: ${e.message}", e)
        } finally {
            _isDownloading.value = false
        }
        return@withContext false
    }

    /**
     * Search fonts by query name or category
     */
    fun searchFonts(query: String): List<GoogleFontMetadata> {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return _availableFonts.value
        return _availableFonts.value.filter {
            it.family.lowercase().contains(q) || it.category.lowercase().contains(q)
        }
    }

    companion object {
        @Volatile
        private var instance: GoogleFontsBridge? = null

        fun getInstance(): GoogleFontsBridge {
            return instance ?: synchronized(this) {
                instance ?: GoogleFontsBridge().also { instance = it }
            }
        }
    }
}
