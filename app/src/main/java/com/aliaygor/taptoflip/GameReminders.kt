package com.aliaygor.taptoflip

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

internal class GameReminders(private val context: Context) {
    private val prefs = context.getSharedPreferences("tap_to_flip", Context.MODE_PRIVATE)
    val enabled: Boolean get() = prefs.getBoolean("reminders_enabled", false)

    fun allowed(): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,
            Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 26 || context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE)

    fun setEnabled(value: Boolean) {
        prefs.edit().putBoolean("reminders_enabled", value).apply()
        if (value) {
            createChannel()
            visit()
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(WORK)
            NotificationManagerCompat.from(context).cancel(NOTIFICATION)
        }
    }

    fun visit() {
        prefs.edit().putLong("last_active", System.currentTimeMillis())
            .putInt("reminders_since_visit", 0).apply()
        NotificationManagerCompat.from(context).cancel(NOTIFICATION)
        if (!enabled) return
        createChannel()
        val request = PeriodicWorkRequestBuilder<ReturnReminderWorker>(6, TimeUnit.HOURS)
            .setInitialDelay(4, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK,
            ExistingPeriodicWorkPolicy.KEEP, request)
    }

    internal fun deliverIfDue(now: Long, hour: Int): Boolean {
        if (!enabled) return false
        createChannel()
        if (!allowed()) return false
        val count = prefs.getInt("reminders_since_visit", 0)
        if (!ReminderPolicy.shouldSend(now, prefs.getLong("last_active", 0),
                prefs.getLong("last_reminder", 0), count, hour)) return false
        val best = prefs.getInt("high_score", 0)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(context, 410, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val text = if (best > 0) "Your best is $best. Got a new record in you?"
            else "One tap, one little challenge. Ready for your first record?"
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(if (count == 0) "A little hop?" else "Your next record is waiting")
            .setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending).setAutoCancel(true).setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW).setSilent(true).build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION, notification)
            prefs.edit().putLong("last_reminder", now)
                .putInt("reminders_since_visit", count + 1).apply()
            return true
        } catch (_: SecurityException) {
            return false
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "Gentle game reminders",
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Occasional invitations after a few days away. Never daily."
                setSound(null, null)
                enableVibration(false)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL = "return_to_game"
        const val WORK = "gentle_return_reminder"
        const val NOTIFICATION = 410
    }
}

class ReturnReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        GameReminders(applicationContext).deliverIfDue(System.currentTimeMillis(),
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
        return Result.success()
    }
}
