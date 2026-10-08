package com.aliaygor.taptoflip
import kotlin.math.exp

enum class GameMode { CLASSIC, TIME_ATTACK, SURVIVAL }
object GameplayRules {
    const val START_SPEED = 95f
    const val GRAVITY = 420f
    const val JUMP = -300f
    const val LEARNING_SECONDS = 25f
    const val TIME_ATTACK_SECONDS = 60f
    fun difficulty(seconds: Float, earlyLosses: Int, mode: GameMode): Float {
        val assistance = 1f - earlyLosses.coerceIn(0, 3) * 0.06f
        val ramp = (seconds - LEARNING_SECONDS).coerceAtLeast(0f) / 150f
        val limit = if (mode == GameMode.SURVIVAL) 1.8f else 1.3f
        return assistance * (1f + limit * (1f - exp(-ramp)))
    }
}
class ComboTracker {
    var streak = 0; private set
    val multiplier get() = when { streak >= 6 -> 3; streak >= 3 -> 2; else -> 1 }
    var events = 0; private set
    fun passed(): Int { streak++; if (streak == 3 || streak == 6) events++; return 5 * multiplier }
    fun breakCombo() { streak = 0 }
    fun reset() { streak = 0; events = 0 }
}
data class DailyTasks(val points: Int = 0, val games: Int = 0, val combos: Int = 0) {
    val completed get() = listOf(points >= 100, games >= 3, combos >= 5)
    fun advance(score: Int, comboEvents: Int, completedGame: Boolean) = DailyTasks(
        (points + score.coerceAtLeast(0)).coerceAtMost(100),
        (games + if (completedGame) 1 else 0).coerceAtMost(3),
        (combos + comboEvents.coerceAtLeast(0)).coerceAtMost(5))
}
fun interface GameTelemetry { fun event(name: String, values: Map<String, String>) }
object Telemetry {
    var sink: GameTelemetry = GameTelemetry { _, _ -> }
    fun emit(name: String, vararg values: Pair<String, Any>) = sink.event(name, values.associate { it.first to it.second.toString() })
}
