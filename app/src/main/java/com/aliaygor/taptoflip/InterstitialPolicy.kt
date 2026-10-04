package com.aliaygor.taptoflip

internal class InterstitialPolicy {
    private var completedRounds = 0
    private var lastShownAt: Long? = null

    fun roundCompleted() { completedRounds++ }
    fun canShow(now: Long): Boolean = completedRounds >= 3 &&
        (lastShownAt?.let { now - it >= 45_000L } ?: true)
    fun shown(now: Long) { completedRounds = 0; lastShownAt = now }
}
