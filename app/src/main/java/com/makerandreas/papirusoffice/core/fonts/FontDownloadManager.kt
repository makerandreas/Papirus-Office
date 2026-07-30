package com.makerandreas.papirusoffice.core.fonts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class FontDownloadManager {

    suspend fun downloadFont(url: String, destinationFile: File): Boolean = withContext(Dispatchers.IO) {
        if (destinationFile.exists()) return@withContext true

        var connection: HttpURLConnection? = null
        try {
            val downloadUrl = URL(url)
            connection = downloadUrl.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext false
            }

            destinationFile.parentFile?.mkdirs()
            
            connection.inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            if (destinationFile.exists()) {
                destinationFile.delete()
            }
            false
        } finally {
            connection?.disconnect()
        }
    }
}
