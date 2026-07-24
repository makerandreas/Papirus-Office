package com.makerandreas.papirusoffice.data.cache

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.makerandreas.papirusoffice.data.util.DocumentParsingLogger
import java.util.concurrent.TimeUnit

/**
 * WorkManager CoroutineWorker for periodically purging expired, duplicate, or orphaned
 * metadata entries in Room for large ODT/DOCX, ODS/XLSX, and ODP/PPTX files.
 */
class DocumentCacheCleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = DocumentCacheRepository(applicationContext)
            val cleanupResult = repository.performAutomatedCleanup()

            DocumentParsingLogger.logError(
                context = applicationContext,
                tag = "DocumentCacheCleanupWorker",
                exceptionType = "CacheCleanupSuccess",
                message = "Automated cache cleanup completed successfully. Purged ${cleanupResult.purgedCount} entries.",
                details = "Timestamp: ${cleanupResult.timestamp}"
            )

            Result.success()
        } catch (e: Exception) {
            DocumentParsingLogger.logError(
                context = applicationContext,
                tag = "DocumentCacheCleanupWorker",
                exceptionType = "CacheCleanupException",
                message = "Failed to run automated cache cleanup: ${e.localizedMessage}",
                details = android.util.Log.getStackTraceString(e)
            )
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "PapirusDocumentCacheCleanupWork"

        /**
         * Schedules periodic background cleanup using WorkManager (e.g. runs every 24 hours).
         */
        fun schedulePeriodicCleanup(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()

                val cleanupRequest = PeriodicWorkRequestBuilder<DocumentCacheCleanupWorker>(
                    24, TimeUnit.HOURS
                )
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    cleanupRequest
                )
            } catch (e: Exception) {
                DocumentParsingLogger.logError(
                    context = context,
                    tag = "DocumentCacheCleanupWorker",
                    exceptionType = "ScheduleWorkException",
                    message = "Could not enqueue periodic cleanup work: ${e.localizedMessage}",
                    details = android.util.Log.getStackTraceString(e)
                )
            }
        }
    }
}
