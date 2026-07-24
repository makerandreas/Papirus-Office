package com.makerandreas.papirusoffice.data.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility logging class to record document parsing issues, malformed XML,
 * and unsupported XML tags directly into the application's crash.log.
 */
object DocumentParsingLogger {

    fun logError(
        context: Context,
        tag: String,
        exceptionType: String,
        message: String,
        details: String
    ) {
        try {
            val file = File(context.filesDir, "crash.log")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dateStr = sdf.format(Date())
            val threadName = Thread.currentThread().name

            val content = """
                === CRASH REPORT ===
                Timestamp: $dateStr
                Thread: $threadName
                Exception: $exceptionType
                Message: $message
                
                StackTrace:
                [$tag]
                $details
                === END CRASH REPORT ===
            """.trimIndent()

            file.appendText(content + "\n\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun logUnsupportedTag(
        context: Context,
        fileName: String,
        tagName: String,
        attributes: Map<String, String> = emptyMap()
    ) {
        val attrStr = if (attributes.isNotEmpty()) {
            attributes.entries.joinToString(", ") { "${it.key}='${it.value}'" }
        } else {
            "None"
        }
        logError(
            context = context,
            tag = "DocParser Warning",
            exceptionType = "UnsupportedXmlTagWarning",
            message = "Unsupported XML tag <$tagName> in file $fileName",
            details = "Unsupported Tag encountered: <$tagName>\nAttributes: $attrStr\nFile: $fileName"
        )
    }

    fun logMalformedXml(
        context: Context,
        fileName: String,
        errorMsg: String,
        cause: Throwable? = null
    ) {
        val stackTrace = cause?.let { android.util.Log.getStackTraceString(it) } ?: "No stack trace available"
        logError(
            context = context,
            tag = "DocParser XML Error",
            exceptionType = "MalformedXmlError",
            message = "Malformed XML in $fileName: $errorMsg",
            details = "Parsing failed for file: $fileName\nError message: $errorMsg\n\n$stackTrace"
        )
    }
}
