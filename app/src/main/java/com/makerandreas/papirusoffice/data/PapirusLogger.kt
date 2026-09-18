package com.makerandreas.papirusoffice.data

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
)

object PapirusLogger {
    private const val TAG_PREFIX = "PapirusOffice"

    /**
     * Log-file rotation bounds. runtime.log is append-only, so without a cap a
     * long-lived install (or a logging hotspot) can fill internal storage.
     */
    const val MAX_LOG_FILE_BYTES = 512L * 1024
    private const val TRUNCATED_KEEP_BYTES = 256L * 1024

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    private var logFile: File? = null

    fun initialize(context: Context) {
        try {
            logFile = File(context.filesDir, "runtime.log")
            if (logFile?.exists() == false) {
                logFile?.createNewFile()
            }
            rotateIfNeeded(logFile)
            i("System", "PapirusLogger initialized. Writing logs to ${logFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e("PapirusLogger", "Initialization failed", e)
        }
    }

    /**
     * Truncates [file] to its last [TRUNCATED_KEEP_BYTES] when it grows past
     * [MAX_LOG_FILE_BYTES]. Shared by the crash log, which has the same
     * append-only growth pattern.
     */
    fun rotateIfNeeded(file: File?) {
        try {
            if (file == null || !file.exists() || file.length() <= MAX_LOG_FILE_BYTES) return
            val keep = TRUNCATED_KEEP_BYTES.coerceAtMost(file.length())
            val tail = ByteArray(keep.toInt())
            java.io.RandomAccessFile(file, "r").use { raf ->
                raf.seek(file.length() - keep)
                raf.readFully(tail)
            }
            // Drop a possible partial first line so the file stays parseable.
            val newline = tail.indexOf('\n'.code.toByte())
            val start = if (newline >= 0) newline + 1 else 0
            file.writeBytes(tail.copyOfRange(start, tail.size))
        } catch (e: Exception) {
            Log.w("PapirusLogger", "Log rotation skipped: ${e.message}")
        }
    }

    private fun addLog(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val entry = LogEntry(timestamp, level, tag, message, throwable)

        // UI Sink (Memory buffer)
        val currentList = _logs.value.toMutableList()
        currentList.add(entry)
        if (currentList.size > 500) { // Keep last 500 lines for the UI
            currentList.removeAt(0)
        }
        _logs.value = currentList

        // Logcat Sink
        val fullTag = "$TAG_PREFIX:$tag"
        try {
            when (level) {
                LogLevel.DEBUG -> Log.d(fullTag, message, throwable)
                LogLevel.INFO -> Log.i(fullTag, message, throwable)
                LogLevel.WARN -> Log.w(fullTag, message, throwable)
                LogLevel.ERROR -> Log.e(fullTag, message, throwable)
            }
        } catch (e: Throwable) {
            println("[$fullTag] $message")
        }

        // File Sink
        logFile?.let { file ->
            try {
                val stackTrace = throwable?.let { "\n" + it.stackTraceToString() } ?: ""
                val logLine = "[$timestamp] [${level.name}] [$tag] $message" + stackTrace + "\n"
                file.appendText(logLine)
            } catch (e: Exception) {
                // Ignore file write errors in unit test environment
            }
        }
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) = addLog(LogLevel.DEBUG, tag, message, throwable)
    fun i(tag: String, message: String, throwable: Throwable? = null) = addLog(LogLevel.INFO, tag, message, throwable)
    fun w(tag: String, message: String, throwable: Throwable? = null) = addLog(LogLevel.WARN, tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = addLog(LogLevel.ERROR, tag, message, throwable)
}
