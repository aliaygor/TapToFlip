package com.aliaygor.taptoflip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class GameEngineTest {
    @Test fun phoneResolutionKeepsOriginalFrogSizeLimit() {
        val engine = GameEngine().apply { resize(1080f, 2100f) }
        assertEquals(88f, engine.player.size, 0.01f)
    }

    @Test fun hudHeightChangesDoNotPauseRun() {
        val engine = engine()
        engine.setScoreForTest(123)
        engine.resize(400f, 680f)
        assertEquals(GameStatus.RUNNING, engine.state)
        assertEquals(123, engine.score)
        engine.pause()
        engine.resize(400f, 700f)
        assertEquals(GameStatus.PAUSED, engine.state)
    }

    @Test fun resizingPreservesRunAndPausesBeforeContinuing() {
        val engine = engine()
        engine.setScoreForTest(123)
        engine.jump()
        val playerY = engine.player.y
        val velocity = engine.player.velocityY
        val obstacle = engine.platforms.first().copy()
        engine.resize(800f, 350f)
        assertEquals(GameStatus.PAUSED, engine.state)
        assertEquals(123, engine.score)
        assertEquals(playerY / 2f, engine.player.y, 0.01f)
        assertEquals(velocity / 2f, engine.player.velocityY, 0.01f)
        assertEquals(obstacle.x * 2f, engine.platforms.first().x, 0.01f)
        assertEquals(obstacle.y / 2f, engine.platforms.first().y, 0.01f)
        engine.resume()
        engine.jump()
        val before = engine.player.y
        engine.update(0.03f)
        assertTrue(engine.player.y < before)
        assertEquals(GameStatus.RUNNING, engine.state)
        engine.resize(800f, 350f)
        assertEquals(GameStatus.RUNNING, engine.state)
    }

    @Test fun jumpUsesSameRelativeHeightInLandscapeAndPortrait() {
        fun relativeRise(width: Float, height: Float): Float {
            val engine = GameEngine().apply { resize(width, height); replacePlatformsForTest(emptyList()) }
            val start = engine.player.y
            engine.jump()
            engine.update(0.03f)
            return (start - engine.player.y) / height
        }
        assertEquals(relativeRise(400f, 700f), relativeRise(900f, 300f), 0.0001f)
    }

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
            assertTrue(abs(generated.y - previous.y) >= engine.worldHeight * 0.10f)
            assertTrue(generated.y >= 0f)
            assertTrue(generated.y + generated.height <= engine.worldHeight)
        }
    }

    @Test
    fun obstaclesReachTopAndBottomBands() {
        val engine = engine()
        val generated = List(80) { engine.generatedPlatformForTest() }

        assertTrue(generated.any { it.y < engine.worldHeight * 0.1f })
        assertTrue(generated.any { it.y + it.height > engine.worldHeight * 0.88f })
    }

    @Test
    fun highScoreCanSpawnBirds() {
        val engine = engine()
        engine.setScoreForTest(500)

        val generated = List(100) { engine.generatedPlatformForTest() }

        assertTrue(generated.any { it.type == ObstacleType.BIRD })
    }

    @Test
    fun obstacleCharactersUnlockInScoreStages() {
        val early = engine()
        early.setScoreForTest(299)
        assertTrue(List(40) { early.generatedPlatformForTest() }.all { it.type == ObstacleType.GRASS })

        val advanced = engine()
        advanced.setScoreForTest(1_500)
        val types = List(300) { advanced.generatedPlatformForTest().type }.toSet()

        assertTrue(types.containsAll(ObstacleType.entries))
    }

    @Test
    fun expertScoresCreateDenserObstacleLayouts() {
        fun averageGap(score: Int): Float {
            val engine = engine()
            engine.setScoreForTest(score)
            val gaps = mutableListOf<Float>()
            repeat(80) {
                val previous = engine.platforms.last()
                val next = engine.generatedPlatformForTest()
                gaps += next.x - (previous.x + previous.width)
            }
            return gaps.average().toFloat()
        }

        assertTrue(averageGap(1_500) < averageGap(100))
    }

    @Test
    fun difficultyContinuesPastOldSpeedCap() {
        val engine = engine(gravity = 0f)
        engine.setScoreForTest(500)
        engine.update(0.01f)

        assertTrue(engine.difficulty > 1.78f)
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
