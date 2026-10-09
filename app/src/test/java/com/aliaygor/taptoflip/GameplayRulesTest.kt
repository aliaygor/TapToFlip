package com.aliaygor.taptoflip
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameplayRulesTest {
    @Test fun learningAndRampAreGentleAndBounded() {
        assertEquals(1f, GameplayRules.difficulty(25f, 0, GameMode.CLASSIC), 0f)
        assertTrue(GameplayRules.difficulty(60f, 0, GameMode.CLASSIC) < 1.3f)
        var previous = 1f
        for (second in 26..600) {
            val value = GameplayRules.difficulty(second.toFloat(), 0, GameMode.CLASSIC)
            assertTrue(value >= previous && value - previous < 0.01f)
            previous = value
        }
        assertTrue(GameplayRules.difficulty(600f, 0, GameMode.SURVIVAL) > previous)
        assertEquals(0.82f, GameplayRules.difficulty(0f, 99, GameMode.CLASSIC), 0.001f)
    }
    @Test fun comboRewardsAndBreakPreserveEarnedEvents() {
        val combo = ComboTracker()
        assertEquals(5, combo.passed()); assertEquals(5, combo.passed())
        assertEquals(10, combo.passed())
        repeat(3) { combo.passed() }
        assertEquals(3, combo.multiplier); assertEquals(2, combo.events)
        combo.breakCombo()
        assertEquals(1, combo.multiplier); assertEquals(2, combo.events)
        combo.reset(); assertEquals(0, combo.events)
    }
    @Test fun dailyTasksAccumulateAndCapWithoutCountingAbandonedGames() {
        val first = DailyTasks().advance(60, 3, false)
        assertEquals(0, first.games)
        val done = first.advance(60, 3, true).advance(0, 0, true).advance(0, 0, true)
        assertEquals(listOf(true, true, true), done.completed)
        assertEquals(done, done.advance(999, 999, true))
    }
    @Test fun refreshRatesProduceSamePhysicsAndScore() {
        fun run(hz: Int): GameEngine {
            val engine = GameEngine(Random(7), gravity = 0f, baseScrollSpeed = 0f)
            engine.resize(400f, 700f)
            engine.replacePlatformsForTest(emptyList())
            repeat(hz * 10) { engine.update(1f / hz) }
            return engine
        }
        val baseline = run(30)
        for (hz in listOf(60, 90, 120)) {
            val engine = run(hz)
            assertEquals(baseline.roundAge, engine.roundAge, 0.01f)
            assertEquals(baseline.score, engine.score)
            assertEquals(baseline.player.y, engine.player.y, 0.01f)
        }
    }
    @Test fun passedObstacleAwardsOnceAndTimeAttackEnds() {
        val engine = GameEngine(gravity = 0f, baseScrollSpeed = 0f, mode = GameMode.TIME_ATTACK)
        engine.resize(400f, 700f)
        // Keep a far obstacle so automatic spawning cannot introduce a random
        // collision while this fixture checks a passed obstacle and the timer.
        engine.replacePlatformsForTest(listOf(
            PlatformState(99, -10f, 0f, 10f, 10f),
            PlatformState(100, 2000f, 0f, 10f, 10f)))
        engine.update(0.02f)
        assertEquals(1, engine.combo.streak)
        repeat(300) { engine.update(0.25f) }
        assertEquals(GameStatus.GAME_OVER, engine.state)
        assertEquals(1, engine.combo.streak)
        assertEquals(0f, engine.remainingSeconds, 0.01f)
    }

    @Test fun gravityAndScrollingAreIndependentOfDisplayRefreshRate() {
        fun run(hz: Int): GameEngine {
            val engine = GameEngine(Random(7), gravity = 100f)
            engine.resize(400f, 700f)
            engine.jump()
            repeat(hz) { engine.update(1f / hz) }
            return engine
        }
        val baseline = run(30)
        for (hz in listOf(60, 90, 120)) {
            val engine = run(hz)
            assertEquals(baseline.player.y, engine.player.y, 0.01f)
            assertEquals(baseline.player.velocityY, engine.player.velocityY, 0.01f)
            assertEquals(baseline.platforms.first().x, engine.platforms.first().x, 0.01f)
        }
    }
    @Test fun edgeCoachingExpiresAfterLearningWindow() {
        val engine = GameEngine(gravity = 0f, baseScrollSpeed = 0f)
        engine.resize(400f, 700f)
        engine.replacePlatformsForTest(emptyList())
        engine.setPlayerForTest(700f - engine.player.size * 0.85f - 1f, 300f)
        engine.update(0.02f)
        assertEquals(GameStatus.RUNNING, engine.state)
        engine.setPlayerForTest(350f)
        repeat(101) { engine.update(0.25f) }
        engine.setPlayerForTest(700f - engine.player.size * 0.85f - 1f, 300f)
        engine.update(0.02f)
        assertEquals(GameStatus.GAME_OVER, engine.state)
    }
}
