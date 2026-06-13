package com.aliaygor.taptoflip

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.random.Random

enum class GameStatus { RUNNING, PAUSED, GAME_OVER }
enum class ObstacleType { GRASS, BIRD, BEE, BAT, FIREFLY }

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
    val height: Float,
    val type: ObstacleType = ObstacleType.GRASS
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
    var roundAge = 0f
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
        roundAge = 0f
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
        roundAge += dt
        jumpFeedback = (jumpFeedback - dt * 4.5f).coerceAtLeast(0f)
        difficulty = 1f + ln(1f + score / 70f) * 0.65f
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

    internal fun setScoreForTest(value: Int) {
        score = value.coerceAtLeast(0)
        elapsedScore = score.toFloat()
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
        val type = chooseObstacleType()
        val minWidth = when (type) {
            ObstacleType.GRASS -> (worldWidth * 0.22f).coerceAtLeast(96f)
            ObstacleType.BIRD -> (worldWidth * 0.14f).coerceAtLeast(72f)
            ObstacleType.BEE -> (worldWidth * 0.12f).coerceAtLeast(64f)
            ObstacleType.BAT -> (worldWidth * 0.15f).coerceAtLeast(76f)
            ObstacleType.FIREFLY -> (worldWidth * 0.11f).coerceAtLeast(58f)
        }
        val maxWidth = when (type) {
            ObstacleType.GRASS -> (worldWidth * 0.39f).coerceAtLeast(minWidth + 24f)
            ObstacleType.BIRD -> (worldWidth * 0.23f).coerceAtLeast(minWidth + 18f)
            ObstacleType.BEE -> (worldWidth * 0.19f).coerceAtLeast(minWidth + 16f)
            ObstacleType.BAT -> (worldWidth * 0.25f).coerceAtLeast(minWidth + 18f)
            ObstacleType.FIREFLY -> (worldWidth * 0.17f).coerceAtLeast(minWidth + 14f)
        }
        val width = randomRange(minWidth, maxWidth)

        val crowding = (score / 1_000f).coerceIn(0f, 1f)
        val expertCrowding = ((score - 1_000) / 1_500f).coerceIn(0f, 1f)
        val minGap = worldWidth * (0.27f - crowding * 0.07f - expertCrowding * 0.04f)
        val maxGap = worldWidth * (0.48f - crowding * 0.13f - expertCrowding * 0.08f)
        val gap = randomRange(minGap, maxGap)

        val height = when (type) {
            ObstacleType.GRASS -> platformHeight()
            ObstacleType.BIRD -> (player.size * 0.62f).coerceIn(38f, 58f)
            ObstacleType.BEE -> (player.size * 0.52f).coerceIn(34f, 50f)
            ObstacleType.BAT -> (player.size * 0.66f).coerceIn(42f, 62f)
            ObstacleType.FIREFLY -> (player.size * 0.48f).coerceIn(32f, 46f)
        }
        val minY = worldHeight * 0.025f
        val maxY = worldHeight - height - worldHeight * 0.025f
        val previousY = previous?.y ?: worldHeight * 0.5f
        val minVerticalChange = worldHeight * 0.115f
        val lanes = floatArrayOf(0.02f, 0.16f, 0.31f, 0.47f, 0.63f, 0.79f, 0.98f)
        var y = minY + (maxY - minY) * lanes[random.nextInt(lanes.size)]
        repeat(4) {
            if (abs(y - previousY) >= minVerticalChange) return@repeat
            y = minY + (maxY - minY) * lanes[random.nextInt(lanes.size)]
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
            height = height,
            type = type
        )
    }

    private fun chooseObstacleType(): ObstacleType {
        val unlocked = mutableListOf(ObstacleType.GRASS)
        if (score >= 300) unlocked += ObstacleType.BIRD
        if (score >= 600) unlocked += ObstacleType.BEE
        if (score >= 900) unlocked += ObstacleType.BAT
        if (score >= 1_200) unlocked += ObstacleType.FIREFLY
        if (unlocked.size == 1 || random.nextFloat() < 0.48f) return ObstacleType.GRASS
        return unlocked[random.nextInt(1, unlocked.size)]
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
