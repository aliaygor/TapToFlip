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
}
