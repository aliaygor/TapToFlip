package com.aliaygor.taptoflip

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Offline progress. Existing high_score values are retained across upgrades. */
internal class PlayerProgress(context: Context) {
    private val prefs = context.getSharedPreferences("tap_to_flip", Context.MODE_PRIVATE)
    private fun day() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val best: Int get() = prefs.getInt("high_score", 0)
    val todayBest: Int get() = if (prefs.getString("score_day", "") == day())
        prefs.getInt("daily_best", 0) else 0
    val dailyTarget: Int get() {
        val today = day()
        if (prefs.getString("target_day", "") != today) {
            prefs.edit().putString("target_day", today)
                .putInt("daily_target", maxOf(50, best)).apply()
        }
        return prefs.getInt("daily_target", 50)
    }
    val topScores: List<Int> get() = prefs.getString("top_scores", "")
        .orEmpty().split(",").mapNotNull { it.toIntOrNull() }
        .filter { it > 0 }.sortedDescending().take(5)
    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) { prefs.edit().putBoolean("sound_enabled", value).apply() }

    fun record(score: Int) {
        val scores = (topScores + score + best).filter { it > 0 }
            .distinct().sortedDescending().take(5)
        prefs.edit().putInt("high_score", maxOf(best, score))
            .putString("score_day", day()).putInt("daily_best", maxOf(todayBest, score))
            .putString("top_scores", scores.joinToString(",")).apply()
    }
}
