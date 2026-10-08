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

    var selectedMode: GameMode
        get() = runCatching { GameMode.valueOf(prefs.getString("mode", "CLASSIC")!!) }.getOrDefault(GameMode.CLASSIC)
        set(value) { prefs.edit().putString("mode", value.name).apply() }
    var tutorialSeen: Boolean
        get() = prefs.getBoolean("tutorial_seen", false)
        set(value) { prefs.edit().putBoolean("tutorial_seen", value).apply() }
    val earlyLosses get() = prefs.getInt("early_losses", 0)
    fun modeBest(mode: GameMode) = if (mode == GameMode.CLASSIC) best else prefs.getInt("best_${mode.name}", 0)
    val totalGames get() = prefs.getInt("total_games", 0)
    val rewardStars get() = prefs.getInt("reward_stars", 0)
    val tasks: DailyTasks get() = if (prefs.getString("tasks_day", "") == day())
        DailyTasks(prefs.getInt("task_points", 0), prefs.getInt("task_games", 0), prefs.getInt("task_combos", 0)) else DailyTasks()
    fun saveBest(mode: GameMode, score: Int) {
        if (score <= modeBest(mode)) return
        if (mode == GameMode.CLASSIC) record(score)
        else prefs.edit().putInt("best_${mode.name}", score).apply()
    }
    fun recordRun(engine: GameEngine) {
        val completedGame = engine.state == GameStatus.GAME_OVER
        val before = tasks
        val after = before.advance(engine.score, engine.combo.events, completedGame)
        val newlyCompleted = after.completed.zip(before.completed).count { it.first && !it.second }
        prefs.edit().putString("tasks_day", day()).putInt("task_points", after.points)
            .putInt("task_games", after.games).putInt("task_combos", after.combos)
            .putInt("reward_stars", rewardStars + newlyCompleted)
            .putInt("total_games", totalGames + if (completedGame) 1 else 0)
            .putInt("early_losses", if (!completedGame) earlyLosses else if (engine.roundAge < 10f) (earlyLosses + 1).coerceAtMost(3) else 0)
            .putInt("best_${engine.mode.name}", maxOf(modeBest(engine.mode), engine.score)).apply()
        if (engine.mode == GameMode.CLASSIC) record(engine.score)
        if (newlyCompleted > 0) Telemetry.emit("daily_task_completed", "count" to newlyCompleted)
        Telemetry.emit("session_duration", "seconds" to engine.roundAge, "mode" to engine.mode)
    }

    fun record(score: Int) {
        val scores = (topScores + score + best).filter { it > 0 }
            .distinct().sortedDescending().take(5)
        prefs.edit().putInt("high_score", maxOf(best, score))
            .putString("score_day", day()).putInt("daily_best", maxOf(todayBest, score))
            .putString("top_scores", scores.joinToString(",")).apply()
    }
}
