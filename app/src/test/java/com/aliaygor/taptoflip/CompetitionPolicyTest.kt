package com.aliaygor.taptoflip

import org.junit.Assert.*
import org.junit.Test

class CompetitionPolicyTest {
    private val valid = RankedResult("player-a", GameMode.CLASSIC, 120, true, true, false)
    @Test fun onlyCompletedRankedRunsWithoutRewardedReviveAreEligible() {
        assertTrue(CompetitionPolicy.eligible(valid))
        assertFalse(CompetitionPolicy.eligible(valid.copy(ranked = false)))
        assertFalse(CompetitionPolicy.eligible(valid.copy(finished = false)))
        assertFalse(CompetitionPolicy.eligible(valid.copy(revived = true)))
        assertFalse(CompetitionPolicy.eligible(valid.copy(owner = "")))
        assertFalse(CompetitionPolicy.eligible(valid.copy(score = 0)))
        assertFalse(CompetitionPolicy.eligible(valid.copy(score = -1)))
    }
    @Test fun boardsRequireRealProjectAndPerModeIds() {
        val empty = CompetitionConfig("", emptyMap())
        assertFalse(empty.hasProject)
        GameMode.entries.forEach { assertFalse(empty.readyFor(it)) }
        assertFalse(CompetitionConfig("0", mapOf(GameMode.CLASSIC to "board")).readyFor(GameMode.CLASSIC))
        val configured = CompetitionConfig("123456789", mapOf(GameMode.CLASSIC to "classic-board"))
        assertTrue(configured.readyFor(GameMode.CLASSIC))
        assertFalse(configured.readyFor(GameMode.TIME_ATTACK))
    }
    @Test fun oldPendingScoresCannotEnterANewDailyCompetition() {
        val midnight = 7L * 60 * 60 * 1000
        assertEquals(-1L, CompetitionPolicy.leaderboardDay(midnight - 1))
        assertEquals(0L, CompetitionPolicy.leaderboardDay(midnight))
        assertTrue(CompetitionPolicy.retryable(0, midnight + 100))
        assertFalse(CompetitionPolicy.retryable(0, midnight + 86_400_000L))
    }
    @Test fun rankedDifficultyIgnoresAdaptiveAssistance() {
        fun engine(ranked: Boolean, losses: Int) = GameEngine(gravity = 0f, baseScrollSpeed = 0f,
            earlyLosses = losses, ranked = ranked).apply { resize(400f, 700f); update(0.1f) }
        assertEquals(engine(true, 0).difficulty, engine(true, 3).difficulty, 0f)
        assertTrue(engine(false, 3).difficulty < engine(true, 3).difficulty)
    }
}
