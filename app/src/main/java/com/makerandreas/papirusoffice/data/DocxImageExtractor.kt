package com.makerandreas.papirusoffice.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DocxImageExtractor(context: Context) {
    private val mediaStore = DocumentMediaStore(File(context.filesDir, "media"))

    suspend fun extractImagesFromDocx(docxFile: File): Map<String, File> = withContext(Dispatchers.IO) {
        extract(docxFile)
    }

    suspend fun extractImagesFromOdt(odtFile: File): Map<String, File> = withContext(Dispatchers.IO) {
        extract(odtFile)
    }

    private fun extract(source: File): Map<String, File> {
        return try {
            mediaStore.extractImages(source)
        } catch (e: Exception) {
            Log.e(TAG, "Image extraction failed for ${source.name}", e)
            emptyMap()
        }
    }

    private companion object {
        const val TAG = "DocxImageExtractor"
    }
}
