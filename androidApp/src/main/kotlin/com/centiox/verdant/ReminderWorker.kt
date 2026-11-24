package com.centiox.verdant

import android.app.*
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as Application
        return try {
            val raw = AndroidBudgetStorage(app).read() ?: return Result.success()
            val data = decodeBackup(raw)
            if (!data.reminders) return Result.success()
            val alerts = data.alerts()
            if (alerts.isEmpty() || !NotificationManagerCompat.from(app).areNotificationsEnabled())
                return Result.success()
            val prefs = app.getSharedPreferences("verdant-native", Context.MODE_PRIVATE)
            val key = "${today()}|${alerts.joinToString().hashCode()}"
            if (prefs.getString("lastReminder", "") == key) return Result.success()
            val manager = app.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    "verdant-reminders",
                    "Budget and payment reminders",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
            val private = data.appLock || data.hideBalances
            val intent =
                Intent(app, MainActivity::class.java)
                    .putExtra("openTools", "Alerts")
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pending =
                PendingIntent.getActivity(
                    app,
                    90,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            val notification =
                NotificationCompat.Builder(app, "verdant-reminders")
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(if (private) "Verdant reminder" else alerts.first().first)
                    .setContentText(
                        if (private) "Unlock Verdant to review your reminders."
                        else alerts.first().second
                    )
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(
                                if (private) "You have ${alerts.size} items to review."
                                else
                                    alerts.take(4).joinToString("\n") {
                                        "${it.first}: ${it.second}"
                                    }
                            )
                    )
                    .setContentIntent(pending)
                    .setAutoCancel(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .build()
            if (
                android.os.Build.VERSION.SDK_INT < 33 ||
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        app,
                        android.Manifest.permission.POST_NOTIFICATIONS,
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                manager.notify(900, notification)
                prefs.edit().putString("lastReminder", key).apply()
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context, data: BudgetData) {
            val manager = WorkManager.getInstance(context)
            if (data.reminders)
                manager.enqueueUniquePeriodicWork(
                    "verdant-reminders",
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS).build(),
                )
            else manager.cancelUniqueWork("verdant-reminders")
        }
    }
}
