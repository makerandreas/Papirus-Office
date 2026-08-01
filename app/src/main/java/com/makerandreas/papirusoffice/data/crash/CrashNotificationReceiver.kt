package com.makerandreas.papirusoffice.data.crash

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                    Toast.makeText(context, "Stacktrace copied to clipboard!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to copy stacktrace: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            ACTION_SAVE_STACKTRACE -> {
                try {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val fileName = "Papirus_Crash_Report_$timeStamp.txt"
                    
                    var targetFile: File? = null
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (downloadsDir != null && (downloadsDir.exists() || downloadsDir.mkdirs())) {
                        targetFile = File(downloadsDir, fileName)
                    }
                    if (targetFile == null) {
                        targetFile = File(context.filesDir, fileName)
                    }

                    targetFile.writeText(stackTrace)
                    Toast.makeText(context, "Saved crash report to:\n${targetFile.absolutePath}", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to save crash log: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            ACTION_SHARE_STACKTRACE -> {
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Papirus Office Crash Report")
                        putExtra(Intent.EXTRA_TEXT, "Headline: Papirus Office crashed!\nSummary: $errorSummary\n\n=== StackTrace ===\n$stackTrace")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val chooserIntent = Intent.createChooser(shareIntent, "Share Crash Report via").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooserIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to share crash report: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
