package com.makerandreas.papirusoffice.data.crash

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Global Uncaught Exception Handler and System Notification Dispatcher.
 * Captures uncaught exceptions, writes full stacktraces to crash.log,
 * and posts an Android System Notification with headline "Papirus Office crashed!",
 * error summary subtitle, and 3 action options: Copy, Save, and Share.
 */
object CrashHandlerManager {

    private const val TAG = "CrashHandlerManager"
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val dateStr = sdf.format(Date())
                val stackTraceString = Log.getStackTraceString(throwable)
                val summary = "${throwable.javaClass.simpleName}: ${throwable.message ?: "No error message"}"

                val fullReport = """
                    === CRASH REPORT ===
                    Timestamp: $dateStr
                    Thread: ${thread.name}
                    Exception: ${throwable.javaClass.name}
                    Message: ${throwable.message ?: "No message provided"}
                    
                    StackTrace:
                    $stackTraceString
                    === END CRASH REPORT ===
                """.trimIndent()

                // 1. Write crash report to internal crash.log
                try {
                    val file = File(context.filesDir, "crash.log")
                    file.appendText(fullReport + "\n\n")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed writing crash.log", e)
                }

                // 2. Dispatch Android System Notification
                sendCrashNotification(context.applicationContext, summary, fullReport)
            } catch (e: Exception) {
                Log.e(TAG, "Error in uncaught exception handler", e)
            }

            // Chain to default uncaught exception handler
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun sendCrashNotification(context: Context, errorSummary: String, stackTrace: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Ensure Notification Channel exists for Android O+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CrashNotificationReceiver.CHANNEL_ID,
                    "Crash Reports",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Displays notifications when Papirus Office encounters a system crash."
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // PendingIntent for tapping notification body -> opens MainActivity and navigates to Crash Logs Screen
            val contentIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("OPEN_CRASH_LOGS", true)
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                0,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Action 1: Copy stacktrace
            val copyIntent = Intent(context, CrashNotificationReceiver::class.java).apply {
                action = CrashNotificationReceiver.ACTION_COPY_STACKTRACE
                putExtra(CrashNotificationReceiver.EXTRA_STACKTRACE, stackTrace)
                putExtra(CrashNotificationReceiver.EXTRA_ERROR_SUMMARY, errorSummary)
            }
            val copyPendingIntent = PendingIntent.getBroadcast(
                context,
                101,
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Action 2: Save stacktrace
            val saveIntent = Intent(context, CrashNotificationReceiver::class.java).apply {
                action = CrashNotificationReceiver.ACTION_SAVE_STACKTRACE
                putExtra(CrashNotificationReceiver.EXTRA_STACKTRACE, stackTrace)
                putExtra(CrashNotificationReceiver.EXTRA_ERROR_SUMMARY, errorSummary)
            }
            val savePendingIntent = PendingIntent.getBroadcast(
                context,
                102,
                saveIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Action 3: Share stacktrace (Direct Activity PendingIntent to bypass background activity launch restrictions)
            val shareText = "Headline: Papirus Office crashed!\nSummary: $errorSummary\n\n=== StackTrace ===\n$stackTrace"
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Papirus Office Crash Report")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            val chooserIntent = Intent.createChooser(sendIntent, "Share Crash Report via")
            val sharePendingIntent = PendingIntent.getActivity(
                context,
                103,
                chooserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CrashNotificationReceiver.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Papirus Office crashed!")
                .setContentText(errorSummary)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle("Papirus Office crashed!")
                        .bigText("Summary: $errorSummary\n\nTap to inspect full crash logs or choose an action below.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .addAction(android.R.drawable.ic_menu_set_as, "Copy", copyPendingIntent)
                .addAction(android.R.drawable.ic_menu_save, "Save", savePendingIntent)
                .addAction(android.R.drawable.ic_menu_share, "Share", sharePendingIntent)
                .build()

            notificationManager.notify(CrashNotificationReceiver.NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending crash notification", e)
        }
    }
}
