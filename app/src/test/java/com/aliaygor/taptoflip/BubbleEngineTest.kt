package com.aliaygor.taptoflip

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class BubbleEngineTest {
    private fun engine() = GameEngine(Random(7), gravity = 0f, baseScrollSpeed = 0f).apply {
        resize(400f, 700f)
        replacePlatformsForTest(emptyList())
        replaceBubblesForTest(emptyList())
    }
    @Test fun collectedBubbleAwardsExactlyOnceWithoutChangingDifficulty() {
        val control = engine()
        val rewarded = engine()
        rewarded.replaceBubblesForTest(listOf(BubbleState(rewarded.player.x + rewarded.player.size / 2f,
            rewarded.player.y + rewarded.player.size / 2f, 16f)))
        repeat(100) { control.update(0.01f); rewarded.update(0.01f) }
        assertEquals(control.score + GameplayRules.BUBBLE_POINTS, rewarded.score)
        assertEquals(1, rewarded.collectedBubbles)
        assertEquals(control.difficulty, rewarded.difficulty, 0f)
        assertEquals(control.combo.streak, rewarded.combo.streak)
    }
    @Test fun fiveConsecutiveBubblesAwardMilestoneOnlyOnce() {
        val e = engine()
        e.replaceBubblesForTest(List(5) { BubbleState(e.player.x + 30f, e.player.y + 30f, 16f) })
        e.update(0.02f)
        assertEquals(5 * GameplayRules.BUBBLE_POINTS + 25, e.score)
        assertEquals(5, e.bubbleStreak)
        assertTrue(e.bubbleMilestoneFeedback > 0f)
        e.update(0.02f)
        assertEquals(5 * GameplayRules.BUBBLE_POINTS + 25, e.score)
        e.reset()
        assertEquals(0, e.bubbleStreak)
        assertEquals(0f, e.bubbleMilestoneFeedback, 0f)
    }
    @Test fun missedBubbleBreaksBonusSeries() {
        val e = engine()
        e.replaceBubblesForTest(List(4) { BubbleState(e.player.x + 30f, e.player.y + 30f, 16f) })
        e.update(0.02f)
        e.replaceBubblesForTest(listOf(BubbleState(-30f, 100f, 10f)))
        e.update(0.02f)
        assertEquals(0, e.bubbleStreak)
        e.replaceBubblesForTest(listOf(BubbleState(e.player.x + 30f, e.player.y + 30f, 16f)))
        e.update(0.02f)
        assertEquals(5 * GameplayRules.BUBBLE_POINTS, e.score)
    }
    @Test fun bubbleOutsideFrogIsNotCollectedAndMissingHasNoPenalty() {
        val engine = engine()
        repeat(3) { engine.combo.passed() }
        engine.replaceBubblesForTest(listOf(BubbleState(engine.player.x + engine.player.size + 30f,
            engine.player.y, 10f), BubbleState(-20f, engine.player.y, 10f)))
        engine.update(0.02f)
        assertEquals(0, engine.collectedBubbles)
        assertEquals(1, engine.bubbles.size)
        assertEquals(3, engine.combo.streak)
        assertEquals(GameStatus.RUNNING, engine.state)
    }
    @Test fun pauseFreezesMovementAndPickup() {
        val engine = engine()
        val bubble = BubbleState(engine.player.x + 30f, engine.player.y + 30f, 16f)
        engine.replaceBubblesForTest(listOf(bubble))
        engine.pause()
        engine.update(0.25f)
        assertEquals(0, engine.collectedBubbles)
        assertEquals(engine.player.x + 30f, bubble.x, 0f)
        assertEquals(0, engine.score)
    }
    @Test fun generatedBubblesHaveClearanceFromObstaclesAndWorldEdges() {
        val engine = GameEngine(Random(7), gravity = 0f).apply { resize(400f, 700f) }
        repeat(80) { engine.generatedPlatformForTest() }
        assertTrue(engine.bubbles.isNotEmpty())
        engine.bubbles.forEach { bubble ->
            assertTrue(bubble.y - bubble.radius > engine.player.size)
            assertTrue(bubble.y + bubble.radius < engine.worldHeight - engine.player.size)
            engine.platforms.forEach { obstacle ->
                assertTrue(bubble.x + bubble.radius + engine.player.size * 0.5f < obstacle.x ||
                    bubble.x - bubble.radius - engine.player.size * 0.5f > obstacle.x + obstacle.width)
            }
        }
    }
    @Test fun rotationScalesExistingBubblesAndResetClearsPickupState() {
        val engine = engine()
        val bubble = BubbleState(300f, 350f, 16f)
        engine.replaceBubblesForTest(listOf(bubble))
        engine.resize(800f, 350f)
        assertEquals(600f, bubble.x, 0f)
        assertEquals(175f, bubble.y, 0f)
        assertEquals(8f, bubble.radius, 0f)
        assertEquals(GameStatus.PAUSED, engine.state)
        engine.resume()
        engine.replaceBubblesForTest(listOf(BubbleState(engine.player.x + 10f, engine.player.y + 10f, 16f)))
        engine.update(0.02f)
        assertEquals(1, engine.collectedBubbles)
        engine.reset()
        assertEquals(0, engine.collectedBubbles)
        assertEquals(0f, engine.bubbleFeedback, 0f)
        assertEquals(0, engine.score)
    }
}
