package com.aliaygor.taptoflip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class GameEngineTest {
    @Test
    fun resetCreatesObstaclesAwayFromFrog() {
        val engine = engine()

        assertEquals(GameStatus.RUNNING, engine.state)
        assertTrue(engine.platforms.isNotEmpty())
        assertTrue(engine.platforms.first().x > engine.player.x + engine.player.size)
    }

    @Test
    fun jumpMovesFrogUp() {
        val engine = engine()
        val startY = engine.player.y

        engine.jump()
        engine.update(0.03f)

        assertTrue(engine.player.y < startY)
        assertTrue(engine.jumpFeedback > 0f)
    }

    @Test
    fun scoreIncreasesContinuouslyWhileAlive() {
        val engine = engine(gravity = 0f, scrollSpeed = 0f)
        engine.replacePlatformsForTest(emptyList())

        repeat(40) { engine.update(0.03f) }

        assertTrue(engine.score >= 11)
        assertEquals(GameStatus.RUNNING, engine.state)
    }

    @Test
    fun touchingGrassEndsGame() {
        val engine = engine(gravity = 0f, scrollSpeed = 0f)
        engine.replacePlatformsForTest(
            listOf(
                PlatformState(
                    id = 9,
                    x = engine.player.x,
                    y = engine.player.y + engine.player.size * 0.4f,
                    width = 180f,
                    height = 40f
                )
            )
        )

        engine.update(0.01f)

        assertEquals(GameStatus.GAME_OVER, engine.state)
        assertTrue(engine.crashFeedback > 0f)
    }

    @Test
    fun nearMissDoesNotEndGame() {
        val engine = engine(gravity = 0f, scrollSpeed = 0f)
        engine.replacePlatformsForTest(
            listOf(
                PlatformState(
                    id = 8,
                    x = engine.player.x + engine.player.size + 2f,
                    y = engine.player.y,
                    width = 140f,
                    height = 40f
                )
            )
        )

        engine.update(0.03f)

        assertEquals(GameStatus.RUNNING, engine.state)
    }

    @Test
    fun generatedObstaclesUseDifferentVerticalBands() {
        val engine = engine()
        repeat(20) {
            val previous = engine.platforms.last()
            val generated = engine.generatedPlatformForTest()
            assertTrue(abs(generated.y - previous.y) >= engine.worldHeight * 0.13f)
            assertTrue(generated.y >= engine.worldHeight * 0.18f)
            assertTrue(generated.y <= engine.worldHeight * 0.78f)
        }
    }

    @Test
    fun pauseFreezesScoreAndMovement() {
        val engine = engine()
        engine.pause()
        val y = engine.player.y
        val score = engine.score
        val x = engine.platforms.first().x

        engine.update(0.03f)

        assertEquals(y, engine.player.y)
        assertEquals(score, engine.score)
        assertEquals(x, engine.platforms.first().x)
    }

    @Test
    fun screenEdgeEndsGameAndResetRecovers() {
        val engine = engine(gravity = 0f)
        engine.replacePlatformsForTest(emptyList())
        engine.setPlayerForTest(engine.worldHeight)

        engine.update(0.01f)
        assertEquals(GameStatus.GAME_OVER, engine.state)

        engine.reset()
        assertEquals(GameStatus.RUNNING, engine.state)
        assertEquals(0, engine.score)
    }

    private fun engine(
        gravity: Float = 100f,
        scrollSpeed: Float = 235f
    ) = GameEngine(
        random = Random(7),
        gravity = gravity,
        baseScrollSpeed = scrollSpeed
    ).apply {
        resize(400f, 700f)
    }
}
