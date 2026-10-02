// SPDX-License-Identifier: MPL-2.0
package com.example.core.jni

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.ResultReceiver
import android.util.Log
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Runs [LokitRuntime] inside the isolated `:office` process (`android:process=":office"`).
 *
 * The caller sends one job per `startService` (paths plus a [ResultReceiver]) and
 * gets the status back through that receiver. The service unbinds nothing, keeps a
 * single worker thread, and self-terminates after [IDLE_KILL_MS] with zero active
 * jobs so the native C++ heap inside `liblo-native-code.so` returns to the OS.
 * `ResultReceiver` and `startService` are API 1, so the transport works on minSdk 24
 * without the API 26 `Messenger(IBinder)` constructor.
 */
class OfficeEngineService : Service() {

    private val worker = Executors.newSingleThreadExecutor { r ->
        Thread(r, "papirus-lokit-worker").apply { isDaemon = true }
    }
    private val activeJobs = AtomicInteger(0)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val idleKill = Runnable {
        if (activeJobs.get() == 0) {
            Log.i(TAG, "Idle timeout reached in :office process; self-terminating to release native heap")
            stopSelf()
            Process.killProcess(Process.myPid())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        val inPath = intent.getStringExtra(KEY_INPUT_PATH)
        val outPath = intent.getStringExtra(KEY_OUTPUT_PATH)
        val password = intent.getStringExtra(KEY_PASSWORD)
        @Suppress("DEPRECATION")
        val receiver = intent.getParcelableExtra(KEY_RECEIVER) as? ResultReceiver
        if (inPath.isNullOrEmpty() || outPath.isNullOrEmpty()) {
            receiver?.send(STATUS_FAILED, Bundle().apply { putString(KEY_ERROR, "Missing input or output path") })
            return START_NOT_STICKY
        }

        mainHandler.removeCallbacks(idleKill)
        activeJobs.incrementAndGet()
        worker.execute {
            val result = try {
                runConvert(inPath, outPath, password)
            } catch (t: Throwable) {
                Bundle().apply {
                    putInt(KEY_STATUS, STATUS_FAILED)
                    putString(KEY_ERROR, "${t.javaClass.simpleName}: ${t.message}")
                }
            }
            try {
                receiver?.send(result.getInt(KEY_STATUS, STATUS_FAILED), result)
            } catch (e: Exception) {
                Log.w(TAG, "Caller disconnected before :office conversion finished", e)
            } finally {
                if (activeJobs.decrementAndGet() == 0) {
                    mainHandler.postDelayed(idleKill, IDLE_KILL_MS)
                    stopSelfResult(startId)
                }
            }
        }
        return START_NOT_STICKY
    }

    /** Not a bound service: the job arrives through [onStartCommand]. */
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mainHandler.removeCallbacks(idleKill)
        worker.shutdown()
        super.onDestroy()
    }

    private fun runConvert(inPath: String, outPath: String, password: String?): Bundle {
        val result = LokitRuntime.convertToPdf(
            context = applicationContext,
            inputFile = File(inPath),
            outputFile = File(outPath),
            password = password
        )
        return Bundle().apply {
            when (result) {
                is LokitConversionResult.Success -> putInt(KEY_STATUS, STATUS_OK)
                is LokitConversionResult.Unavailable -> putInt(KEY_STATUS, STATUS_UNAVAILABLE)
                is LokitConversionResult.PasswordRequired -> putInt(KEY_STATUS, STATUS_PASSWORD_REQUIRED)
                is LokitConversionResult.Failed -> {
                    putInt(KEY_STATUS, STATUS_FAILED)
                    putString(KEY_ERROR, result.reason)
                }
            }
        }
    }

    companion object {
        private const val TAG = "OfficeEngineService"
        private const val IDLE_KILL_MS = 60_000L

        const val KEY_INPUT_PATH = "in"
        const val KEY_OUTPUT_PATH = "out"
        const val KEY_PASSWORD = "pw"
        const val KEY_RECEIVER = "receiver"
        const val KEY_STATUS = "status"
        const val KEY_ERROR = "error"

        const val STATUS_OK = 0
        const val STATUS_UNAVAILABLE = 1
        const val STATUS_PASSWORD_REQUIRED = 2
        const val STATUS_FAILED = 3
    }
}
