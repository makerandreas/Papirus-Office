// SPDX-License-Identifier: MPL-2.0
package com.example.core.jni

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Caller side of [OfficeEngineService] (`:office` process).
 *
 * Returns `false` immediately when [LokitEngine.isNativeAvailable] is `false`, so the
 * JVM/Robolectric unit tests and any build without the bundled native library take the
 * pure-Kotlin PDF fallback instead of waiting on a service that cannot do the job.
 * One job per call, with [CONVERT_TIMEOUT_MS] as an upper bound.
 */
object OfficeEngineClient {
    private const val TAG = "OfficeEngineClient"
    private const val CONVERT_TIMEOUT_MS = 90_000L

    suspend fun convertToPdf(
        context: Context,
        inputFile: File,
        outputFile: File,
        password: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!LokitEngine.isNativeAvailable || !inputFile.exists()) {
            return@withContext false
        }
        outputFile.parentFile?.mkdirs()
        val status = withTimeoutOrNull(CONVERT_TIMEOUT_MS) {
            runJob(context.applicationContext, inputFile, outputFile, password)
        } ?: return@withContext false
        status == OfficeEngineService.STATUS_OK && outputFile.exists() && outputFile.length() > 0L
    }

    private suspend fun runJob(
        appContext: Context,
        inputFile: File,
        outputFile: File,
        password: String?
    ): Int? = suspendCancellableCoroutine { cont ->
        val done = AtomicBoolean(false)
        val receiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                if (done.compareAndSet(false, true) && cont.isActive) {
                    cont.resume(resultCode)
                }
            }
        }
        val intent = Intent(appContext, OfficeEngineService::class.java).apply {
            putExtra(OfficeEngineService.KEY_INPUT_PATH, inputFile.absolutePath)
            putExtra(OfficeEngineService.KEY_OUTPUT_PATH, outputFile.absolutePath)
            if (password != null) {
                putExtra(OfficeEngineService.KEY_PASSWORD, password)
            }
            putExtra(OfficeEngineService.KEY_RECEIVER, receiver)
        }
        try {
            appContext.startService(intent)
        } catch (e: Exception) {
            Log.w(TAG, "Could not start the :office conversion service", e)
            if (done.compareAndSet(false, true) && cont.isActive) cont.resume(null)
        }
        cont.invokeOnCancellation { done.set(true) }
    }
}
