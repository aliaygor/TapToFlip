package com.aliaygor.taptoflip

internal object ReminderPolicy {
    const val DAY = 86_400_000L

    // Two gentle invitations at most until the player returns; never daily.
    fun shouldSend(now: Long, lastActive: Long, lastSent: Long, sentSinceVisit: Int, hour: Int): Boolean {
        if (lastActive <= 0 || sentSinceVisit >= 2 || hour !in 10..19) return false
        val interval = if (sentSinceVisit == 0) 4 * DAY else 7 * DAY
        return now - lastActive >= 4 * DAY && (lastSent == 0L || now - lastSent >= interval)
    }
}
