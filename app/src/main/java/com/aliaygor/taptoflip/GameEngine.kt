package com.aliaygor.taptoflip

import kotlin.math.abs
import kotlin.math.sin
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
    var y: Float,
    var width: Float,
    var height: Float,
    val type: ObstacleType = ObstacleType.GRASS,
    var passed: Boolean = false,
    var anchorY: Float = y
)

class GameEngine(
    private val random: Random = Random.Default,
    private val gravity: Float = GameplayRules.GRAVITY,
    private val jumpVelocity: Float = GameplayRules.JUMP,
    private val baseScrollSpeed: Float = GameplayRules.START_SPEED,
    var mode: GameMode = GameMode.CLASSIC,
    var earlyLosses: Int = 0
) {
    val combo = ComboTracker()
    var bonusFeedback = 0f; private set
    val remainingSeconds get() = (GameplayRules.TIME_ATTACK_SECONDS - roundAge).coerceAtLeast(0f)
    private var bonusScore = 0
    private var accumulator = 0f

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
    var reviveUsed = false
        private set
    var protectionSeconds = 0f
        private set

    fun reviveAfterReward(): Boolean {
        if (state != GameStatus.GAME_OVER || reviveUsed) return false
        reviveUsed = true
        player.y = worldHeight * 0.48f
        player.velocityY = 0f
        platforms.removeAll { it.x < player.x + player.size * 3f && it.x + it.width > player.x - player.size }
        combo.breakCombo()
        protectionSeconds = 3f
        crashFeedback = 0f
        jumpFeedback = 0f
        state = GameStatus.RUNNING
        return true
    }

    val player = PlayerState()
    val platforms = mutableListOf<PlatformState>()

    private var nextPlatformId = 1
    private var initialized = false
    private var elapsedScore = 0f

    fun resize(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        if (initialized && width == worldWidth && height == worldHeight) return
        val firstLayout = !initialized
        if (!firstLayout) {
            val orientationChanged = (width > height) != (worldWidth > worldHeight)
            val scaleX = width / worldWidth
            val scaleY = height / worldHeight
            player.y *= scaleY
            player.velocityY *= scaleY
            platforms.forEach {
                it.x *= scaleX
                it.y *= scaleY
                it.anchorY *= scaleY
                it.width *= scaleX
                it.height *= scaleY
            }
            // Only rotation pauses the run; HUD/inset relayouts must keep playing.
            if (orientationChanged) pause()
        }
        worldWidth = width
        worldHeight = height
        player.size = minOf(width * 0.17f, height * 0.12f, 88f)
        player.x = width * 0.2f
        if (firstLayout) {
            initialized = true
            reset()
        }
    }

    fun jump() {
        if (state != GameStatus.RUNNING) return
        player.velocityY = jumpVelocity * worldHeight / 700f
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
        reviveUsed = false
        protectionSeconds = 0f
        elapsedScore = 0f
        bonusScore = 0
        accumulator = 0f
        bonusFeedback = 0f
        combo.reset()
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
        if (state != GameStatus.RUNNING || !initialized || !deltaSeconds.isFinite()) return
        accumulator += deltaSeconds.coerceIn(0f, 0.25f)
        val stepSize = 1f / 120f
        while (accumulator + 0.000001f >= stepSize && state == GameStatus.RUNNING) {
            accumulator -= stepSize
            step(stepSize)
        }
    }
    private fun step(deltaSeconds: Float) {
        if (state != GameStatus.RUNNING || !initialized) return

        val dt = deltaSeconds.coerceIn(0f, 0.033f)
        roundAge += dt
        jumpFeedback = (jumpFeedback - dt * 4.5f).coerceAtLeast(0f)
        difficulty = GameplayRules.difficulty(roundAge, earlyLosses, mode)
        bonusFeedback = (bonusFeedback - dt).coerceAtLeast(0f)
        val scroll = baseScrollSpeed * (worldWidth / 400f) * difficulty * dt

        val wasInsideWorld = !touchesWorldEdge()
        player.velocityY += gravity * (worldHeight / 700f) * dt
        player.y += player.velocityY * dt
        if (roundAge < GameplayRules.LEARNING_SECONDS && wasInsideWorld && touchesWorldEdge()) {
            player.y = player.y.coerceIn(0f, worldHeight - player.size)
            player.velocityY = 0f
            combo.breakCombo()
        }
        val protected = protectionSeconds > 0f
        protectionSeconds = (protectionSeconds - dt).coerceAtLeast(0f)
        if (protected) player.y = player.y.coerceIn(0f, (worldHeight - player.size).coerceAtLeast(0f))
        platforms.forEach {
            it.x -= scroll
            if (roundAge >= 60f && it.type != ObstacleType.GRASS) {
                it.y = (it.anchorY + sin(roundAge * 1.2f + it.id) * worldHeight * 0.035f * ((roundAge - 60f) / 10f).coerceIn(0f, 1f))
                    .coerceIn(0f, worldHeight - it.height)
            }
        }

        elapsedScore += dt * 10f
        val updatedScore = elapsedScore.toInt() + bonusScore
        if (updatedScore > score) {
            score = updatedScore
            scoreEvent = score / 10
        }

        if (!protected && (touchesWorldEdge() || platforms.any(::collidesWithPlayer))) {
            state = GameStatus.GAME_OVER
            crashFeedback = 1f
            return
        }

        platforms.forEach {
            if (!it.passed && it.x + it.width < player.x) {
                it.passed = true
                bonusScore += combo.passed()
                if (combo.streak % 10 == 0) bonusScore += 20
                score = elapsedScore.toInt() + bonusScore
                bonusFeedback = 1f
            }
        }
        if (mode == GameMode.TIME_ATTACK && remainingSeconds <= 0f) {
            state = GameStatus.GAME_OVER
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
        elapsedScore = (score - bonusScore).toFloat()
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
        val reactionGap = baseScrollSpeed * (worldWidth / 400f) * difficulty * 1.25f
        val gap = randomRange(maxOf(minGap, reactionGap), maxOf(maxGap, reactionGap + worldWidth * 0.12f * (1f - crowding * 0.4f - expertCrowding * 0.15f)))

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
