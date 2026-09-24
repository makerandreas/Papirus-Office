package com.makerandreas.papirusoffice.data.crash

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.R

/**
 * BroadcastReceiver for handling system-level crash notification actions:
 * - Copy: Copy stacktrace to system clipboard
 * - Save: Save stacktrace to Downloads/Storage
 * - Share: Share stacktrace via Intent chooser
 */
class CrashNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_COPY_STACKTRACE = "com.makerandreas.papirusoffice.ACTION_COPY_STACKTRACE"
        const val ACTION_SAVE_STACKTRACE = "com.makerandreas.papirusoffice.ACTION_SAVE_STACKTRACE"
        const val ACTION_SHARE_STACKTRACE = "com.makerandreas.papirusoffice.ACTION_SHARE_STACKTRACE"
        const val EXTRA_STACKTRACE = "extra_stacktrace"
        const val EXTRA_ERROR_SUMMARY = "extra_error_summary"
        const val CHANNEL_ID = "papirus_crash_reports"
        const val NOTIFICATION_ID = 9991

        fun getSavedStackTrace(context: Context): String {
            return try {
                val file = File(context.filesDir, "crash.log")
                if (file.exists()) file.readText() else "No stacktrace recorded."
            } catch (e: Exception) {
                "Error reading crash log: ${e.message}"
            }
        }
    }

    /**
     * Saves a crash report to Downloads via MediaStore on Android 10+ (no
     * storage permission needed for our own entry), falling back to internal
     * storage on older releases or when MediaStore is unavailable.
     *
     * @return human-readable save location for the confirmation toast.
     */
    private fun saveCrashReport(context: Context, fileName: String, content: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                )
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(content.toByteArray())
                    } ?: throw java.io.IOException("MediaStore refused output stream")
                    return "Downloads/$fileName"
                }
            } catch (e: Exception) {
                Log.w("CrashReceiver", "MediaStore save failed, using internal storage", e)
            }
        }
        val fallback = File(context.filesDir, fileName)
        fallback.writeText(content)
        return fallback.absolutePath
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        val stackTrace = intent.getStringExtra(EXTRA_STACKTRACE) ?: getSavedStackTrace(context)
        val errorSummary = intent.getStringExtra(EXTRA_ERROR_SUMMARY) ?: "Papirus Office error occurred"

        when (action) {
            ACTION_COPY_STACKTRACE -> {
                try {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Papirus Crash Stacktrace", stackTrace)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, R.string.toast_stacktrace_copied_to_clipboard, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.toast_failed_to_copy_stacktrace_e_message, e.message), Toast.LENGTH_SHORT).show()
                }
            }
            ACTION_SAVE_STACKTRACE -> {
                try {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val fileName = "Papirus_Crash_Report_$timeStamp.txt"
                    val savedLocation = saveCrashReport(context, fileName, stackTrace)
                    Toast.makeText(context, context.getString(R.string.toast_saved_crash_report_to_n_savedlocation, savedLocation), Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.toast_failed_to_save_crash_log_e_message, e.message), Toast.LENGTH_SHORT).show()
                }
            }
            ACTION_SHARE_STACKTRACE -> {
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Papirus Office Crash Report")
                        putExtra(Intent.EXTRA_TEXT, "Headline: Papirus Office crashed!\nSummary: $errorSummary\n\n=== StackTrace ===\n$stackTrace")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooserIntent = Intent.createChooser(shareIntent, "Share Crash Report via").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(chooserIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.toast_failed_to_share_crash_report_e_message, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
