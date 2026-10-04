package com.aliaygor.taptoflip

import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class ReminderDeliveryTest {
    @Test fun requiresConsentAndPermissionAndDoesNotSendDuplicates() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = instrumentation.targetContext
        val name = "reminder_test_${System.nanoTime()}"
        var denyNotificationPermission = true
        val context = object : ContextWrapper(base) {
            override fun checkPermission(permission: String, pid: Int, uid: Int): Int {
                if (permission == android.Manifest.permission.POST_NOTIFICATIONS && denyNotificationPermission)
                    return PackageManager.PERMISSION_DENIED
                return super.checkPermission(permission, pid, uid)
            }
            override fun getSharedPreferences(ignored: String, mode: Int) = base.getSharedPreferences(name, mode)
        }
        val prefs = context.getSharedPreferences("", 0)
        val reminders = GameReminders(context)
        val now = System.currentTimeMillis()
        val manager = base.getSystemService(NotificationManager::class.java)
        try {
            prefs.edit().putLong("last_active", now - 4 * ReminderPolicy.DAY)
                .putInt("high_score", 123).commit()
            assertFalse(reminders.deliverIfDue(now, 12)) // No user opt-in.
            prefs.edit().putBoolean("reminders_enabled", true).commit()
            if (Build.VERSION.SDK_INT >= 33) assertFalse(reminders.deliverIfDue(now, 12))
            denyNotificationPermission = false
            assertTrue(reminders.deliverIfDue(now, 12))
            assertTrue(manager.activeNotifications.any { it.id == GameReminders.NOTIFICATION &&
                it.notification.extras.getCharSequence("android.text").toString().contains("123") &&
                it.notification.contentIntent != null })
            assertEquals(1, prefs.getInt("reminders_since_visit", 0))
            assertFalse(reminders.deliverIfDue(now, 12))
            prefs.edit().putLong("last_active", now).commit()
            assertFalse(reminders.deliverIfDue(now, 12))
        } finally {
            manager.cancel(GameReminders.NOTIFICATION)
            // The harness grants before instrumentation and restores after it exits.
            // Revoking a real permission during instrumentation kills the runner.
            prefs.edit().clear().commit()
        }
    }
}
