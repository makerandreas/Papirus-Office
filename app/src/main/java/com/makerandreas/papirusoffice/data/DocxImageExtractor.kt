package com.makerandreas.papirusoffice.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class DocxImageExtractor(private val context: Context) {

    /**
     * Membaca dan mengekstrak gambar dari berkas DOCX tanpa membebani Main Thread UI
     */
    suspend fun extractImagesFromDocx(docxFile: File): Map<String, File> = withContext(Dispatchers.IO) {
        val extractedImages = mutableMapOf<String, File>()
        val cacheDir = File(context.cacheDir, "docx_media_${docxFile.nameWithoutExtension}").apply { mkdirs() }

        try {
            ZipInputStream(docxFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    // Berkas gambar di Microsoft OpenXML tersimpan di folder word/media/
                    if (entry.name.startsWith("word/media/")) {
                        val imageName = entry.name.substringAfterLast("/")
                        val outputFile = File(cacheDir, imageName)

                        if (!outputFile.exists()) {
                            FileOutputStream(outputFile).use { output ->
                                zip.copyTo(output)
                            }
                        }
                        extractedImages[imageName] = outputFile
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext extractedImages
    }
}
