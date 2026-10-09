package com.aliaygor.taptoflip

/** Separate boards must be configured for this scoring ruleset, not legacy scores. */
internal data class CompetitionConfig(val projectId: String, val boards: Map<GameMode, String>) {
    val hasProject get() = (projectId.trim().toLongOrNull() ?: 0L) > 0L
    fun readyFor(mode: GameMode) = hasProject && !boards[mode].isNullOrBlank()
}
internal data class RankedResult(val owner: String, val mode: GameMode, val score: Int,
    val ranked: Boolean, val finished: Boolean, val revived: Boolean)
internal object CompetitionPolicy {
    const val RULESET = "balanced_bubbles_v1"
    fun eligible(result: RankedResult) = result.owner.isNotBlank() && result.ranked &&
        result.finished && !result.revived && result.score > 0
    // Play Games daily leaderboards reset at 07:00 UTC (UTC-7 midnight).
    fun leaderboardDay(nowMillis: Long) = Math.floorDiv(nowMillis - 7L * 60 * 60 * 1000, 86_400_000L)
    fun retryable(recordedDay: Long, nowMillis: Long) = recordedDay == leaderboardDay(nowMillis)
}
