package com.makerandreas.papirusoffice.data

import android.content.Context
import android.util.Log
import com.makerandreas.papirusoffice.data.util.ZipSafe
import com.makerandreas.papirusoffice.data.util.copyCappedTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class DocxImageExtractor(private val context: Context) {

    companion object {
        private const val TAG = "DocxImageExtractor"
    }

    /**
     * Membaca dan mengekstrak gambar dari berkas DOCX/ODT tanpa membebani Main Thread UI.
     *
     * Extraction is bounded ([ZipSafe.MAX_IMAGE_COUNT] files,
     * [ZipSafe.MAX_IMAGE_BYTES] each) so a hostile archive cannot fill the disk.
     */
    /**
     * P0-3: stable image-cache key by file identity (path + length + mtime) instead of
     * bare `nameWithoutExtension`. Prevents SAF `document.odt` vs. Recents `document_1.odt`
     * cache miss and collisions between unrelated same-name files.
     */
    private fun cacheKeyForFile(file: File): String {
        return try {
            val basis = "${file.absolutePath}:${file.length()}:${file.lastModified()}"
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(basis.toByteArray(Charsets.UTF_8))
            // 16 hex chars (= 64 bits) is enough for cache-dir uniqueness and keeps path short
            digest.joinToString("") { "%02x".format(it) }.take(16)
        } catch (e: Exception) {
            // Fallback: sanitized name + hashCode (never empty)
            val sanitized = file.nameWithoutExtension.replace("[^A-Za-z0-9]".toRegex(), "_").take(24)
            "${sanitized}_${file.absolutePath.hashCode().let { if (it < 0) -it else it }}"
        }
    }

    suspend fun extractImagesFromDocx(docxFile: File): Map<String, File> = withContext(Dispatchers.IO) {
        val extractedImages = mutableMapOf<String, File>()
        val cacheDir = File(context.cacheDir, "docx_media_${cacheKeyForFile(docxFile)}").apply { mkdirs() }

        try {
            ZipInputStream(docxFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                var imageCount = 0
                while (entry != null) {
                    // Berkas gambar di Microsoft OpenXML tersimpan di folder word/media/
                    // Berkas gambar di OpenDocument (ODT) tersimpan di folder Pictures/
                    val name = entry.name
                    if (name.startsWith("word/media/") || name.startsWith("Pictures/") || name.startsWith("pictures/")) {
                        val imageName = sanitizeImageName(name.substringAfterLast("/"))
                        if (imageName != null) {
                            if (imageCount >= ZipSafe.MAX_IMAGE_COUNT) {
                                Log.w(TAG, "Image count cap reached, skipping remaining entries")
                                break
                            }
                            val outputFile = File(cacheDir, imageName)

                            if (!outputFile.exists()) {
                                try {
                                    FileOutputStream(outputFile).use { output ->
                                        zip.copyCappedTo(output, ZipSafe.MAX_IMAGE_BYTES)
                                    }
                                } catch (e: Exception) {
                                    Log.w(TAG, "Skipping oversized image $name: ${e.message}")
                                    outputFile.delete()
                                    zip.closeEntry()
                                    entry = zip.nextEntry
                                    continue
                                }
                            }
                            imageCount++
                            extractedImages[imageName] = outputFile
                            // Also store with full path key for ODT relative links (e.g. Pictures/image.png)
                            extractedImages[name] = outputFile
                            // P0-3: lower-cased variant so xlink:href case differences don't miss
                            extractedImages[name.lowercase(java.util.Locale.ROOT)] = outputFile
                            extractedImages[imageName.lowercase(java.util.Locale.ROOT)] = outputFile
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Image extraction failed for ${docxFile.name}", e)
        }
        return@withContext extractedImages
    }

    /**
     * Returns a traversal-safe file name, or null when the entry name is
     * suspicious (empty, parent refs, separators).
     */
    private fun sanitizeImageName(raw: String): String? {
        if (raw.isEmpty() || raw == "." || raw == "..") return null
        if (raw.contains('/') || raw.contains('\\')) return null
        val safe = raw.replace("[^A-Za-z0-9 _.,()-]".toRegex(), "_").take(100)
        return safe.ifEmpty { null }
    }

    suspend fun extractImagesFromOdt(odtFile: File): Map<String, File> = extractImagesFromDocx(odtFile)
}
