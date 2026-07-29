package com.makerandreas.papirusoffice.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Periodic WorkManager worker to clean temporary cache files and clear stale rendering cache
 * for Papirus Engine to optimize memory and disk usage on low-end target devices.
 */
class PapirusEngineCacheWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "PapirusEngineCacheWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting periodic Papirus Engine cache cleanup task...")

            var deletedFilesCount = 0
            var deletedBytes = 0L

            val maxAgeMillis = 24 * 60 * 60 * 1000L // 24 hours
            val now = System.currentTimeMillis()

            // 1. Clean cacheDir (.tmp, .docx, .xlsx, .pdf, .tmp_render files)
            val cacheDir = appContext.cacheDir
            if (cacheDir.exists() && cacheDir.isDirectory) {
                cacheDir.listFiles()?.forEach { file ->
                    if (isDisposableCacheFile(file, now, maxAgeMillis)) {
                        val size = file.length()
                        if (file.deleteRecursively()) {
                            deletedFilesCount++
                            deletedBytes += size
                        }
                    }
                }
            }

            // 2. Clean externalCacheDir if accessible
            val extCacheDir = appContext.externalCacheDir
            if (extCacheDir != null && extCacheDir.exists() && extCacheDir.isDirectory) {
                extCacheDir.listFiles()?.forEach { file ->
                    if (isDisposableCacheFile(file, now, maxAgeMillis)) {
                        val size = file.length()
                        if (file.deleteRecursively()) {
                            deletedFilesCount++
                            deletedBytes += size
                        }
                    }
                }
            }

            Log.i(TAG, "Papirus Engine cache cleanup complete. Removed $deletedFilesCount files (${deletedBytes / 1024} KB).")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error performing Papirus Engine cache cleanup: ${e.message}", e)
            Result.retry()
        }
    }

    private fun isDisposableCacheFile(file: File, now: Long, maxAgeMillis: Long): Boolean {
        if (!file.exists()) return false
        val isOld = (now - file.lastModified()) > maxAgeMillis
        val name = file.name.lowercase()
        val isTmpOrRender = name.startsWith("temp_") || name.startsWith("render_") || 
                            name.endsWith(".tmp") || name.endsWith(".bak") || name.contains("_cache")
        return isOld || isTmpOrRender
    }
}
