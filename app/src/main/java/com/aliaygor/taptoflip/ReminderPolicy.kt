package com.aliaygor.taptoflip

internal object ReminderPolicy {
    const val DAY = 86_400_000L

    const val INTERVAL = 2 * DAY
    fun shouldSend(now: Long, scheduleStarted: Long, lastSent: Long, hour: Int): Boolean {
        if (scheduleStarted <= 0 || hour !in 10..19) return false
        return now - (if (lastSent == 0L) scheduleStarted else lastSent) >= INTERVAL
    }
}
