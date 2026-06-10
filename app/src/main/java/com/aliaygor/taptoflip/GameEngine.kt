package com.aliaygor.taptoflip

import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

enum class GameStatus { RUNNING, PAUSED, GAME_OVER }

data class PlayerState(
    var x: Float = 0f,
    var y: Float = 0f,
    var velocityY: Float = 0f,
    var size: Float = 72f
)

data class PlatformState(
    val id: Int,
    var x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

class GameEngine(
    private val random: Random = Random.Default,
    private val gravity: Float = 1750f,
    private val jumpVelocity: Float = -690f,
    private val baseScrollSpeed: Float = 245f
) {
    var worldWidth = 0f
        private set
    var worldHeight = 0f
        private set
    var state = GameStatus.RUNNING
        private set
    var score = 0
        private set
    var difficulty = 1f
        private set
    var jumpFeedback = 0f
        private set
    var crashFeedback = 0f
        private set
    var scoreEvent = 0
        private set

    val player = PlayerState()
    val platforms = mutableListOf<PlatformState>()

    private var nextPlatformId = 1
    private var initialized = false
    private var elapsedScore = 0f

    fun resize(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        val firstLayout = !initialized
        worldWidth = width
        worldHeight = height
        player.size = (width * 0.17f).coerceIn(62f, 88f)
        player.x = width * 0.2f
        if (firstLayout) {
            initialized = true
            reset()
        }
    }

    fun jump() {
        if (state != GameStatus.RUNNING) return
        player.velocityY = jumpVelocity
        jumpFeedback = 1f
    }

    fun pause() {
        if (state == GameStatus.RUNNING) state = GameStatus.PAUSED
    }

    fun resume() {
        if (state == GameStatus.PAUSED) state = GameStatus.RUNNING
    }

    fun reset() {
        if (!initialized) return
        state = GameStatus.RUNNING
        score = 0
        difficulty = 1f
        jumpFeedback = 0f
        crashFeedback = 0f
        scoreEvent = 0
        elapsedScore = 0f
        nextPlatformId = 1
        platforms.clear()
        player.y = worldHeight * 0.48f
        player.velocityY = 0f

        platforms += PlatformState(
            id = nextPlatformId++,
            x = worldWidth * 0.78f,
            y = worldHeight * 0.69f,
            width = worldWidth * 0.34f,
            height = platformHeight()
        )
        while (rightmostEdge() < worldWidth * 1.75f) spawnPlatform()
    }

    fun update(deltaSeconds: Float) {
        if (state != GameStatus.RUNNING || !initialized) return

        val dt = deltaSeconds.coerceIn(0f, 0.033f)
        jumpFeedback = (jumpFeedback - dt * 4.5f).coerceAtLeast(0f)
        difficulty = 1f + (score.coerceAtMost(120) * 0.0065f)
        val scroll = baseScrollSpeed * difficulty * dt

        player.velocityY += gravity * dt
        player.y += player.velocityY * dt
        platforms.forEach { it.x -= scroll }

        elapsedScore += dt * 10f
        val updatedScore = elapsedScore.toInt()
        if (updatedScore > score) {
            score = updatedScore
            scoreEvent = score / 10
        }

        if (touchesWorldEdge() || platforms.any(::collidesWithPlayer)) {
            state = GameStatus.GAME_OVER
            crashFeedback = 1f
            return
        }

        platforms.removeAll { it.x + it.width < -24f }
        while (rightmostEdge() < worldWidth * 1.55f) spawnPlatform()
    }

    internal fun replacePlatformsForTest(items: List<PlatformState>) {
        platforms.clear()
        platforms.addAll(items)
        nextPlatformId = (items.maxOfOrNull { it.id } ?: 0) + 1
    }

    internal fun setPlayerForTest(y: Float, velocityY: Float = 0f) {
        player.y = y
        player.velocityY = velocityY
    }

    internal fun generatedPlatformForTest(): PlatformState {
        spawnPlatform()
        return platforms.last()
    }

    private fun collidesWithPlayer(platform: PlatformState): Boolean {
        val insetX = player.size * 0.2f
        val insetY = player.size * 0.16f
        val playerLeft = player.x + insetX
        val playerRight = player.x + player.size - insetX
        val playerTop = player.y + insetY
        val playerBottom = player.y + player.size - insetY
        return playerLeft < platform.x + platform.width &&
            playerRight > platform.x &&
            playerTop < platform.y + platform.height &&
            playerBottom > platform.y
    }

    private fun touchesWorldEdge(): Boolean =
        player.y + player.size * 0.15f <= 0f ||
            player.y + player.size * 0.85f >= worldHeight

    private fun spawnPlatform() {
        val previous = platforms.maxByOrNull { it.x + it.width }
        val minWidth = (worldWidth * 0.24f).coerceAtLeast(104f)
        val maxWidth = (worldWidth * 0.42f).coerceAtLeast(minWidth + 24f)
        val width = randomRange(minWidth, maxWidth)

        val minGap = worldWidth * 0.28f
        val maxGap = (worldWidth * 0.48f + score.coerceAtMost(100) * 0.45f)
            .coerceAtMost(worldWidth * 0.58f)
        val gap = randomRange(minGap, maxGap)

        val minY = worldHeight * 0.18f
        val maxY = worldHeight * 0.78f
        val previousY = previous?.y ?: worldHeight * 0.5f
        val minVerticalChange = worldHeight * 0.14f
        var y = randomRange(minY, maxY)
        repeat(4) {
            if (abs(y - previousY) >= minVerticalChange) return@repeat
            y = randomRange(minY, maxY)
        }
        if (abs(y - previousY) < minVerticalChange) {
            y = if (previousY < worldHeight * 0.5f) {
                (previousY + minVerticalChange).coerceAtMost(maxY)
            } else {
                (previousY - minVerticalChange).coerceAtLeast(minY)
            }
        }

        platforms += PlatformState(
            id = nextPlatformId++,
            x = (previous?.let { it.x + it.width } ?: worldWidth) + gap,
            y = y,
            width = width,
            height = platformHeight()
        )
    }

    private fun rightmostEdge(): Float =
        platforms.maxOfOrNull { it.x + it.width } ?: 0f

    private fun platformHeight(): Float =
        (worldHeight * 0.052f).coerceIn(34f, 54f)

    private fun randomRange(min: Float, max: Float): Float {
        if (abs(max - min) < 0.001f) return min
        return min + random.nextFloat() * max(max - min, 0f)
    }
}
