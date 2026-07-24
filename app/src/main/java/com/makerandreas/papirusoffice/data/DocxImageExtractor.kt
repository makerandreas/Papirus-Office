package com.makerandreas.papirusoffice.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class DocxImageExtractor(private val context: Context) {

    /**
     * Membaca dan mengekstrak gambar dari berkas DOCX/ODT tanpa membebani Main Thread UI
     */
    suspend fun extractImagesFromDocx(docxFile: File): Map<String, File> = withContext(Dispatchers.IO) {
        val extractedImages = mutableMapOf<String, File>()
        val cacheDir = File(context.cacheDir, "docx_media_${docxFile.nameWithoutExtension}").apply { mkdirs() }

        try {
            ZipInputStream(docxFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    // Berkas gambar di Microsoft OpenXML tersimpan di folder word/media/
                    // Berkas gambar di OpenDocument (ODT) tersimpan di folder Pictures/
                    val name = entry.name
                    if (name.startsWith("word/media/") || name.startsWith("Pictures/") || name.startsWith("pictures/")) {
                        val imageName = name.substringAfterLast("/")
                        if (imageName.isNotEmpty()) {
                            val outputFile = File(cacheDir, imageName)

                            if (!outputFile.exists()) {
                                FileOutputStream(outputFile).use { output ->
                                    zip.copyTo(output)
                                }
                            }
                            extractedImages[imageName] = outputFile
                            // Also store with full path key for ODT relative links (e.g. Pictures/image.png)
                            extractedImages[name] = outputFile
                        }
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

    suspend fun extractImagesFromOdt(odtFile: File): Map<String, File> = extractImagesFromDocx(odtFile)
}
