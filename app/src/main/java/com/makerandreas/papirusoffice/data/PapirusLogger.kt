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
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs = _logs.asStateFlow()

    private var logFile: File? = null

    fun initialize(context: Context) {
        try {
            logFile = File(context.filesDir, "runtime.log")
            if (logFile?.exists() == false) {
                logFile?.createNewFile()
            }
            i("System", "PapirusLogger initialized. Writing logs to ${logFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e("PapirusLogger", "Initialization failed", e)
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
        when (level) {
            LogLevel.DEBUG -> Log.d(fullTag, message, throwable)
            LogLevel.INFO -> Log.i(fullTag, message, throwable)
            LogLevel.WARN -> Log.w(fullTag, message, throwable)
            LogLevel.ERROR -> Log.e(fullTag, message, throwable)
        }

        // File Sink
        logFile?.let { file ->
            try {
                val logLine = "[$timestamp] [${level.name}] [$tag] $message" +
                        (throwable?.let { "\n" + Log.getStackTraceString(it) } ?: "") + "\n"
                file.appendText(logLine)
            } catch (e: Exception) {
                Log.e("PapirusLogger", "Failed to append log to runtime.log", e)
            }
        }
    }

    fun d(tag: String, message: String, throwable: Throwable? = null) = addLog(LogLevel.DEBUG, tag, message, throwable)
    fun i(tag: String, message: String, throwable: Throwable? = null) = addLog(LogLevel.INFO, tag, message, throwable)
    fun w(tag: String, message: String, throwable: Throwable? = null) = addLog(LogLevel.WARN, tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = addLog(LogLevel.ERROR, tag, message, throwable)
}
