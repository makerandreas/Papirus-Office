package com.makerandreas.papirusoffice

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import android.os.StrictMode
import com.example.BuildConfig
import com.makerandreas.papirusoffice.data.PapirusLogger
import com.makerandreas.papirusoffice.data.crash.CrashHandlerManager
import java.io.File

/**
 * PapirusApplication
 *
 * Implements Android 17 behavior guidelines & Low-End device (e.g. Samsung Galaxy A11, Realme C3)
 * memory management, StrictMode implicit URI grant detection, and ApplicationExitInfo monitoring.
 */
class PapirusApplication : Application(), ComponentCallbacks2 {

    companion object {
        lateinit var instance: PapirusApplication
            private set

        fun isLowRamDevice(context: Context): Boolean {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            return am?.isLowRamDevice ?: false
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. Initialize Papirus Runtime Logger & Crash Handler
        PapirusLogger.initialize(this)
        CrashHandlerManager.init(this)

        // 2. StrictMode VM Policy: Detect implicit URI permission grants (Android 17/18 requirement)
        setupStrictMode()

        // 3. Android 17 MemoryLimiter & Exit Reason Diagnostics (API 30+)
        inspectPreviousExitReasons()

        PapirusLogger.i("PapirusApp", "PapirusApplication initialized. LowRamDevice: ${isLowRamDevice(this)}")
    }

    /**
     * StrictMode detection to catch implicit URI permission grants without FLAG_GRANT_READ_URI_PERMISSION.
     */
    private fun setupStrictMode() {
        if (BuildConfig.DEBUG) {
            try {
                val vmPolicyBuilder = StrictMode.VmPolicy.Builder()
                // Dynamically invoke detectImplicitUriPermissionGrant() for Android 17+ support
                try {
                    val method = StrictMode.VmPolicy.Builder::class.java.getMethod("detectImplicitUriPermissionGrant")
                    method.invoke(vmPolicyBuilder)
                } catch (_: NoSuchMethodException) {
                    // Method available starting on Android 17 (API 37)
                }
                vmPolicyBuilder.penaltyLog()
                StrictMode.setVmPolicy(vmPolicyBuilder.build())
                PapirusLogger.d("PapirusApp", "StrictMode VmPolicy detectImplicitUriPermissionGrant configured")
            } catch (e: Throwable) {
                PapirusLogger.w("PapirusApp", "StrictMode setup exception: ${e.message}")
            }
        }
    }

    /**
     * Inspects ApplicationExitInfo for Android 17 MemoryLimiter (MemoryLimiter:AnonSwap)
     * and Low Memory terminations from the previous app execution.
     */
    private fun inspectPreviousExitReasons() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                val exitReasons = activityManager?.getHistoricalProcessExitReasons(packageName, 0, 5)
                exitReasons?.firstOrNull()?.let { exitInfo ->
                    val desc = exitInfo.description ?: ""
                    val timestamp = exitInfo.timestamp
                    val reason = exitInfo.reason

                    if (reason == ApplicationExitInfo.REASON_OTHER && desc.contains("MemoryLimiter:AnonSwap")) {
                        PapirusLogger.w(
                            "MemoryLimiter",
                            "Previous session terminated by Android 17 MemoryLimiter:AnonSwap (Timestamp: $timestamp, Description: $desc)"
                        )
                    } else if (reason == ApplicationExitInfo.REASON_LOW_MEMORY) {
                        PapirusLogger.w(
                            "MemoryLimiter",
                            "Previous session killed by System Low Memory Killer (LMK) (Timestamp: $timestamp)"
                        )
                    } else if (reason == ApplicationExitInfo.REASON_CRASH || reason == ApplicationExitInfo.REASON_CRASH_NATIVE) {
                        PapirusLogger.e(
                            "CrashMonitor",
                            "Previous session encountered native/Java crash (Reason code: $reason, Description: $desc)"
                        )
                    }
                }
            } catch (e: Throwable) {
                PapirusLogger.w("PapirusApp", "Failed to inspect historical exit reasons: ${e.message}")
            }
        }
    }

    /**
     * Handles system memory trimming events to protect low-RAM devices (Galaxy A11, Realme C3)
     * and prevent Android 17 AnonSwap memory limiter kills.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        PapirusLogger.w("MemoryTrim", "onTrimMemory received level: $level")

        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                // Aggressively free temporary extracted caches
                clearTemporaryMediaCache()
                System.gc()
            }
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                // Moderate cleanup when app is in background or moderate pressure
                clearTemporaryMediaCache()
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        PapirusLogger.e("MemoryTrim", "onLowMemory triggered! Purging memory caches aggressively.")
        clearTemporaryMediaCache()
        System.gc()
    }

    /**
     * Cleans up temporary extracted document media files in cacheDir.
     */
    private fun clearTemporaryMediaCache() {
        try {
            val cache = cacheDir ?: return
            val tempDirs = cache.listFiles { file ->
                file.isDirectory && file.name.startsWith("docx_media_")
            }
            tempDirs?.forEach { dir ->
                dir.deleteRecursively()
            }
        } catch (e: Throwable) {
            PapirusLogger.w("MemoryTrim", "Error clearing temporary media cache: ${e.message}")
        }
    }
}
