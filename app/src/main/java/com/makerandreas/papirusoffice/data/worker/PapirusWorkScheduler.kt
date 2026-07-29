package com.makerandreas.papirusoffice.data.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Scheduler utility for managing Papirus Engine background WorkManager tasks.
 */
object PapirusWorkScheduler {

    private const val TAG = "PapirusWorkScheduler"
    private const val PERIODIC_CACHE_WORK_NAME = "papirus_engine_periodic_cache_cleanup"
    private const val ONE_TIME_CACHE_WORK_NAME = "papirus_engine_one_time_cache_cleanup"

    /**
     * Schedules a periodic cache cleanup work task running every 24 hours.
     */
    fun schedulePeriodicCacheCleanup(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<PapirusEngineCacheWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_CACHE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
            Log.d(TAG, "Scheduled 24-hour periodic cache cleanup task via WorkManager.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule periodic cache cleanup: ${e.message}", e)
        }
    }

    /**
     * Enqueues an immediate one-time cache cleanup request.
     */
    fun runImmediateCacheCleanup(context: Context) {
        try {
            val oneTimeRequest = OneTimeWorkRequestBuilder<PapirusEngineCacheWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_CACHE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
            Log.d(TAG, "Enqueued immediate cache cleanup request.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to run immediate cache cleanup: ${e.message}", e)
        }
    }
}
