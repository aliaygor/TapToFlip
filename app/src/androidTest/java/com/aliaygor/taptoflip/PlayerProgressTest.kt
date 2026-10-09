package com.aliaygor.taptoflip

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class PlayerProgressTest {
    private fun isolatedContext(): Context {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "progress_test_${System.nanoTime()}"
        return object : ContextWrapper(base) {
            override fun getSharedPreferences(ignored: String, mode: Int) =
                base.getSharedPreferences(name, mode)
        }
    }

    @Test fun preservesLegacyBestAndKeepsOnlyFiveRankedScores() {
        val context = isolatedContext()
        context.getSharedPreferences("", 0).edit().putInt("high_score", 80).commit()
        val progress = PlayerProgress(context)
        assertEquals(80, progress.best)
        listOf(10, 100, 30, 90, 120, 110, 100).forEach(progress::record)
        val reopened = PlayerProgress(context)
        assertEquals(120, reopened.best)
        assertEquals(listOf(120, 110, 100, 90, 80), reopened.topScores)
    }

    @Test fun dailyGoalStaysFixedAndYesterdayScoreResets() {
        val context = isolatedContext()
        val progress = PlayerProgress(context)
        assertEquals(50, progress.dailyTarget)
        progress.record(100)
        assertEquals(50, progress.dailyTarget)
        assertEquals(100, progress.todayBest)
        context.getSharedPreferences("", 0).edit()
            .putString("score_day", "2000-01-01")
            .putString("target_day", "2000-01-01").commit()
        assertEquals(0, progress.todayBest)
        assertEquals(100, progress.dailyTarget)
        progress.record(20)
        assertEquals(20, progress.todayBest)
        assertEquals(100, progress.best)
    }

    @Test fun frogColorSurvivesReopeningAndInvalidValuesFallBackToGreen() {
        val context = isolatedContext()
        assertEquals(FrogStyle.GREEN, PlayerProgress(context).frogStyle)
        assertFalse(PlayerProgress(context).frogUnlocked(FrogStyle.BLUE))
        PlayerProgress(context).frogStyle = FrogStyle.BLUE
        assertEquals(FrogStyle.GREEN, PlayerProgress(context).frogStyle)
        PlayerProgress(context).unlockFrog(FrogStyle.BLUE)
        assertTrue(PlayerProgress(context).frogUnlocked(FrogStyle.BLUE))
        PlayerProgress(context).frogStyle = FrogStyle.BLUE
        assertEquals(FrogStyle.BLUE, PlayerProgress(context).frogStyle)
        context.getSharedPreferences("", 0).edit().putString("frog_style", "unknown").commit()
        assertEquals(FrogStyle.GREEN, PlayerProgress(context).frogStyle)
    }

    @Test fun soundPreferenceSurvivesReopening() {
        val context = isolatedContext()
        PlayerProgress(context).soundEnabled = false
        assertFalse(PlayerProgress(context).soundEnabled)
    }

    @Test fun tasksRewardsAndModeRecordsSurviveAndRefresh() {
        val context = isolatedContext()
        val progress = PlayerProgress(context)
        val engine = GameEngine(gravity = 0f, mode = GameMode.SURVIVAL)
        engine.resize(400f, 700f)
        repeat(3) {
            engine.setScoreForTest(60)
            engine.setPlayerForTest(700f)
            engine.update(0.02f)
            repeat(6) { engine.combo.passed() }
            progress.recordRun(engine)
            engine.reset()
        }
        val reopened = PlayerProgress(context)
        assertEquals(listOf(true, true, true), reopened.tasks.completed)
        assertEquals(3, reopened.rewardStars)
        assertEquals(60, reopened.modeBest(GameMode.SURVIVAL))
        assertEquals(0, reopened.best)
        assertEquals(3, reopened.totalGames)
        context.getSharedPreferences("", 0).edit().putString("tasks_day", "2000-01-01").commit()
        assertEquals(DailyTasks(), reopened.tasks)
        assertEquals(3, reopened.rewardStars)
    }

    @Test fun timeAttackBestAndTodayBestStaySeparateFromClassicAndResetByDay() {
        val context = isolatedContext()
        val progress = PlayerProgress(context)
        progress.record(61)
        val engine = GameEngine(mode = GameMode.TIME_ATTACK)
        engine.setScoreForTest(560)
        progress.recordRun(engine)
        val reopened = PlayerProgress(context)
        assertEquals(560, reopened.modeBest(GameMode.TIME_ATTACK))
        assertEquals(560, reopened.modeTodayBest(GameMode.TIME_ATTACK))
        assertEquals(61, reopened.modeBest(GameMode.CLASSIC))
        assertEquals(61, reopened.modeTodayBest(GameMode.CLASSIC))
        engine.setScoreForTest(100)
        reopened.recordRun(engine)
        assertEquals(560, reopened.modeTodayBest(GameMode.TIME_ATTACK))
        context.getSharedPreferences("", 0).edit().putString("daily_day_TIME_ATTACK", "2000-01-01").commit()
        assertEquals(0, reopened.modeTodayBest(GameMode.TIME_ATTACK))
        assertEquals(560, reopened.modeBest(GameMode.TIME_ATTACK))
    }

    @Test fun onlineOutboxIsAccountBoundAndKeepsHigherUnsentResult() {
        val context = isolatedContext()
        val progress = PlayerProgress(context)
        val first = RankedResult("player-a", GameMode.CLASSIC, 120, true, true, false)
        progress.queueRankedScore(first)
        progress.queueRankedScore(first.copy(score = 60))
        assertEquals(120, PlayerProgress(context).pendingRankedScore("player-a", GameMode.CLASSIC))
        assertNull(progress.pendingRankedScore("player-b", GameMode.CLASSIC))
        assertNull(progress.pendingRankedScore("player-a", GameMode.SURVIVAL))
        progress.queueRankedScore(first.copy(score = 200))
        val day = CompetitionPolicy.leaderboardDay(System.currentTimeMillis())
        progress.ackRankedScore("player-a", GameMode.CLASSIC, 120, day)
        assertEquals(200, progress.pendingRankedScore("player-a", GameMode.CLASSIC))
        progress.ackRankedScore("player-a", GameMode.CLASSIC, 200, day - 1)
        assertEquals(200, progress.pendingRankedScore("player-a", GameMode.CLASSIC))
        progress.ackRankedScore("player-a", GameMode.CLASSIC, 200, day)
        assertNull(progress.pendingRankedScore("player-a", GameMode.CLASSIC))
    }
    @Test fun revivedAndGuestScoresNeverEnterTheOnlineOutbox() {
        val progress = PlayerProgress(isolatedContext())
        val result = RankedResult("player-a", GameMode.CLASSIC, 120, true, true, false)
        progress.queueRankedScore(result.copy(revived = true))
        progress.queueRankedScore(result.copy(ranked = false))
        assertNull(progress.pendingRankedScore("player-a", GameMode.CLASSIC))
        progress.competitionEnabled = true
        assertTrue(progress.competitionEnabled)
    }
    @Test fun runStarsAreOnceOnlyAndCanBuyPermanentAppearance() {
        val context = isolatedContext()
        val progress = PlayerProgress(context)
        assertFalse(progress.awardRunStars("round", 5, doubled = true))
        assertTrue(progress.awardRunStars("round", 5))
        assertFalse(progress.awardRunStars("round", 5))
        assertTrue(progress.awardRunStars("round", 5, doubled = true))
        assertFalse(progress.awardRunStars("round", 5, doubled = true))
        assertEquals(10, PlayerProgress(context).rewardStars)
        assertFalse(progress.buyFrog(FrogStyle.SPOTTED))
        assertTrue(progress.awardRunStars("next", 10))
        assertTrue(progress.buyFrog(FrogStyle.SPOTTED))
        assertEquals(0, progress.rewardStars)
        assertTrue(PlayerProgress(context).frogUnlocked(FrogStyle.SPOTTED))
        assertFalse(progress.buyFrog(FrogStyle.SPOTTED))
        assertEquals(0, progress.best)
        assertFalse(progress.awardRunStars("empty", 0))
    }
}
