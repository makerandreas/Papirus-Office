package com.makerandreas.papirusoffice.core.fonts

import android.util.Log
import com.makerandreas.papirusoffice.data.util.ZipSafe
import com.makerandreas.papirusoffice.data.util.copyCappedTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class FontDownloadManager {

    companion object {
        private const val TAG = "FontDownloadManager"
    }

    suspend fun downloadFont(url: String, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        if (destinationFile.exists()) return@withContext true

        // Only HTTPS font hosts are accepted; font URLs come from the Google
        // Fonts API, anything else is rejected instead of fetched.
        val downloadUrl = try {
            URL(url).also {
                require(it.protocol.equals("https", ignoreCase = true)) {
                    "Refusing non-HTTPS font URL"
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Invalid font URL rejected: ${e.message}")
            return@withContext false
        }

        var connection: HttpURLConnection? = null
        try {
            connection = downloadUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext false
            }
            val contentLength = connection.contentLengthLong
            if (contentLength > ZipSafe.MAX_FONT_BYTES) {
                Log.w(TAG, "Font download rejected: $contentLength bytes exceeds cap")
                return@withContext false
            }

            destinationFile.parentFile?.mkdirs()

            connection.inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyCappedTo(output, ZipSafe.MAX_FONT_BYTES)
                }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Font download failed: ${e.message}")
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            false
        } finally {
            connection?.disconnect()
        }
    }
}
