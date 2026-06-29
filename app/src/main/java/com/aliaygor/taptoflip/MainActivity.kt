package com.aliaygor.taptoflip

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.aliaygor.taptoflip.ui.theme.TapToFlipTheme
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val SkyTop = Color(0xFF70D8FF)
private val SkyBottom = Color(0xFFD7F5FF)
private val DeepGreen = Color(0xFF176B3A)
private val GrassGreen = Color(0xFF65C43B)
private val Lime = Color(0xFFA9E34B)
private val Earth = Color(0xFF8D542E)
private val Cream = Color(0xFFFFF7D6)
private val Ink = Color(0xFF16324A)
private const val ADMOB_BANNER_AD_UNIT_ID = "ca-app-pub-5287725227601079/1395452429"
private const val ADMOB_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-5287725227601079/3135360764"

class MainActivity : ComponentActivity() {
    private var interstitialAd: InterstitialAd? = null
    private var gameOverCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            TapToFlipTheme {
                GameApp()
            }
        }
        lifecycleScope.launch {
            delay(1_600)
            MobileAds.initialize(this@MainActivity) {
                loadInterstitial()
            }
        }
    }

    private fun loadInterstitial() {
        InterstitialAd.load(
            this,
            ADMOB_INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun showInterstitial() {
        gameOverCount++
        if (gameOverCount % 3 != 0) return
        interstitialAd?.apply {
            fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() = loadInterstitial()
                override fun onAdFailedToShowFullScreenContent(error: AdError) = loadInterstitial()
                override fun onAdShowedFullScreenContent() {
                    interstitialAd = null
                }
            }
            show(this@MainActivity)
        }
    }
}

private enum class AppScreen { MENU, HOW_TO_PLAY, GAME }

private class GameAudio {
    private val tones = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 38)
    }.getOrNull()

    fun jump() {
        runCatching { tones?.startTone(ToneGenerator.TONE_PROP_BEEP, 45) }
    }

    fun gameOver() {
        runCatching { tones?.startTone(ToneGenerator.TONE_PROP_NACK, 180) }
    }

    fun release() {
        runCatching { tones?.release() }
    }
}

@Composable
private fun GameApp() {
    val activity = LocalActivity.current
    var screen by remember { mutableStateOf(AppScreen.MENU) }
    var soundEnabled by remember { mutableStateOf(true) }

    Surface(modifier = Modifier.fillMaxSize(), color = SkyBottom) {
        when (screen) {
            AppScreen.MENU -> MenuScreen(
                soundEnabled = soundEnabled,
                onToggleSound = { soundEnabled = it },
                onStart = { screen = AppScreen.GAME },
                onHowToPlay = { screen = AppScreen.HOW_TO_PLAY },
                onExit = { activity?.finish() }
            )

            AppScreen.HOW_TO_PLAY -> HowToPlayScreen(
                onPlay = { screen = AppScreen.GAME },
                onBack = { screen = AppScreen.MENU }
            )

            AppScreen.GAME -> GameScreen(
                soundEnabled = soundEnabled,
                onExitToMenu = { screen = AppScreen.MENU }
            )
        }
    }
}

@Composable
private fun MenuScreen(
    soundEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onStart: () -> Unit,
    onHowToPlay: () -> Unit,
    onExit: () -> Unit
) {
    val frog = rememberFrogBitmap()
    ScenicBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                SoundToggle(soundEnabled, onToggleSound)
            }
            Spacer(Modifier.weight(0.25f))
            Text(
                text = "TAP TO",
                color = Color.White,
                fontSize = 38.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "FLIP",
                color = Lime,
                fontSize = 66.sp,
                lineHeight = 62.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "Tap. Dodge. Keep going.",
                color = Ink.copy(alpha = 0.82f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Image(
                bitmap = frog,
                contentDescription = null,
                modifier = Modifier.size(190.dp).padding(top = 6.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.weight(0.45f))
            Column(
                modifier = Modifier.offset(y = (-74).dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PrimaryButton("START", onStart)
                Spacer(Modifier.height(6.dp))
                MenuTextButton("HOW TO PLAY", onHowToPlay)
                MenuTextButton("EXIT", onExit, color = Color(0xFF9A3D4D))
            }
        }
    }
}

@Composable
private fun HowToPlayScreen(onPlay: () -> Unit, onBack: () -> Unit) {
    ScenicBackground {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = Cream.copy(alpha = 0.96f)),
            shape = RoundedCornerShape(30.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "HOW TO PLAY",
                    color = DeepGreen,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(20.dp))
                Instruction("1", "Tap to launch the frog upward.")
                Instruction("2", "Do not touch any grass platform.")
                Instruction("3", "Survive longer as the game speeds up.")
                Spacer(Modifier.height(22.dp))
                PrimaryButton("LET'S HOP", onPlay)
                Spacer(Modifier.height(8.dp))
                SecondaryButton("BACK", onBack)
            }
        }
    }
}

@Composable
private fun Instruction(number: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).background(GrassGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = Color.White, fontWeight = FontWeight.Black)
        }
        Text(
            text,
            modifier = Modifier.padding(start = 14.dp),
            color = Ink,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GameScreen(soundEnabled: Boolean, onExitToMenu: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current as? MainActivity
    val preferences = remember {
        context.getSharedPreferences("tap_to_flip", Context.MODE_PRIVATE)
    }
    val engine = remember { GameEngine() }
    val audio = remember { GameAudio() }
    val haptic = LocalHapticFeedback.current
    val frog = rememberFrogBitmap()
    var frameVersion by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(preferences.getInt("high_score", 0)) }
    var adShownForRound by remember { mutableStateOf(false) }

    DisposableEffect(activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                engine.pause()
                frameVersion++
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            activity?.lifecycle?.removeObserver(observer)
            audio.release()
        }
    }

    LaunchedEffect(Unit) {
        var lastFrame = 0L
        while (true) {
            withFrameNanos { now ->
                if (lastFrame == 0L) lastFrame = now
                val dt = ((now - lastFrame) / 1_000_000_000f).coerceAtMost(0.033f)
                lastFrame = now
                val previousState = engine.state
                engine.update(dt)

                if (engine.score > highScore) {
                    highScore = engine.score
                    preferences.edit().putInt("high_score", highScore).apply()
                }
                if (previousState != GameStatus.GAME_OVER && engine.state == GameStatus.GAME_OVER) {
                    if (soundEnabled) audio.gameOver()
                    if (!adShownForRound) {
                        activity?.showInterstitial()
                        adShownForRound = true
                    }
                }
                frameVersion++
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Ink)) {
        ScoreBar(engine.score, highScore, engine.difficulty, onExitToMenu) {
            engine.pause()
            frameVersion++
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { engine.resize(it.width.toFloat(), it.height.toFloat()) }
                .pointerInput(engine.state, soundEnabled) {
                    detectTapGestures {
                        if (engine.state == GameStatus.RUNNING) {
                            engine.jump()
                            if (soundEnabled) {
                                audio.jump()
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            frameVersion++
                        }
                    }
                }
        ) {
            frameVersion
            GameplayCanvas(engine)
            FrogSprite(engine, frog)
            if (engine.state == GameStatus.RUNNING && engine.roundAge < 2.2f) {
                TapHint(engine)
            }

            when (engine.state) {
                GameStatus.PAUSED -> PauseOverlay(
                    onResume = {
                        engine.resume()
                        frameVersion++
                    },
                    onMenu = onExitToMenu
                )

                GameStatus.GAME_OVER -> GameOverOverlay(
                    score = engine.score,
                    highScore = highScore,
                    onRestart = {
                        engine.reset()
                        adShownForRound = false
                        frameVersion++
                    },
                    onMenu = onExitToMenu
                )

                GameStatus.RUNNING -> Unit
            }
        }
        BannerPanel(activity)
    }
}

@Composable
private fun ScoreBar(
    score: Int,
    highScore: Int,
    difficulty: Float,
    onMenu: () -> Unit,
    onPause: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Ink).padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMenu,
            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Text("⌂", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        ScoreChip("SCORE", score.toString(), Color.White)
        ScoreChip("BEST", highScore.toString(), Lime)
        ScoreChip("SPEED", "${(difficulty * 100).roundToInt()}%", Color(0xFFFFD166))
        IconButton(
            onClick = onPause,
            modifier = Modifier.background(Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Text("Ⅱ", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ScoreChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.58f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 19.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun BoxScope.FrogSprite(engine: GameEngine, frog: ImageBitmap) {
    val density = LocalDensity.current
    val frogSize = with(density) { engine.player.size.toDp() }
    val stretch = 1f + engine.jumpFeedback * 0.12f
    val rotation = (engine.player.velocityY / 38f).coerceIn(-16f, 18f)
    Image(
        bitmap = frog,
        contentDescription = "Frog",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .offset {
                IntOffset(engine.player.x.roundToInt(), engine.player.y.roundToInt())
            }
            .size(frogSize)
            .graphicsLayer {
                alpha = if (engine.state == GameStatus.GAME_OVER) 0f else 1f
                scaleX = 1f / stretch
                scaleY = stretch
                rotationZ = rotation
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.82f)
            }
    )
}

@Composable
private fun BoxScope.TapHint(engine: GameEngine) {
    val density = LocalDensity.current
    val widthPx = with(density) { 112.dp.toPx() }
    val heightPx = with(density) { 54.dp.toPx() }
    val pulse = (0.66f + sin(engine.roundAge * 9f) * 0.28f).coerceIn(0.35f, 1f)
    Column(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (engine.player.x + engine.player.size / 2f - widthPx / 2f).roundToInt(),
                    y = max(8f, engine.player.y - heightPx - 12f).roundToInt()
                )
            }
            .graphicsLayer {
                alpha = pulse
                scaleX = 0.96f + pulse * 0.04f
                scaleY = 0.96f + pulse * 0.04f
                shadowElevation = 10f
            }
            .background(Color.White.copy(alpha = 0.94f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("TAP!", color = DeepGreen, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("TO JUMP", color = Ink.copy(alpha = 0.68f), fontWeight = FontWeight.Bold, fontSize = 9.sp)
    }
}

@Composable
private fun GameplayCanvas(engine: GameEngine) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawSky()
        drawHills()
        drawClouds(engine.difficulty)
        engine.platforms.forEach { obstacle ->
            when (obstacle.type) {
                ObstacleType.GRASS -> drawGrassPlatform(obstacle, engine.roundAge)
                ObstacleType.BIRD -> drawBirdObstacle(obstacle, engine.roundAge)
                ObstacleType.BEE -> drawBeeObstacle(obstacle, engine.roundAge)
                ObstacleType.BAT -> drawBatObstacle(obstacle, engine.roundAge)
                ObstacleType.FIREFLY -> drawFireflyObstacle(obstacle, engine.roundAge)
            }
        }
        drawFrogLimbs(engine)

        if (engine.jumpFeedback > 0f) {
            val alpha = engine.jumpFeedback * 0.28f
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = engine.player.size * (1.1f - engine.jumpFeedback * 0.25f),
                center = Offset(
                    engine.player.x + engine.player.size / 2f,
                    engine.player.y + engine.player.size / 2f
                ),
                style = Stroke(width = 5f)
            )
        }
        if (engine.crashFeedback > 0f) {
            val center = Offset(
                engine.player.x + engine.player.size / 2f,
                engine.player.y + engine.player.size / 2f
            )
            repeat(8) { index ->
                val direction = index * (Math.PI * 2.0 / 8.0)
                val distance = engine.player.size * (1f - engine.crashFeedback) * 1.25f
                drawCircle(
                    color = if (index % 2 == 0) Color(0xFFFFD166) else Color(0xFFEF476F),
                    radius = 4f + engine.crashFeedback * 5f,
                    center = center + Offset(
                        x = (kotlin.math.cos(direction) * distance).toFloat(),
                        y = (kotlin.math.sin(direction) * distance).toFloat()
                    )
                )
            }
        }
    }
}

private fun DrawScope.drawFrogLimbs(engine: GameEngine) {
    if (engine.state == GameStatus.GAME_OVER) return
    val extension = (
        (-engine.player.velocityY / 690f).coerceIn(0f, 1f) * 0.7f +
            engine.jumpFeedback * 0.3f
        ).coerceIn(0f, 1f)
    if (extension < 0.04f) return

    val size = engine.player.size
    val left = engine.player.x
    val top = engine.player.y
    val limbColor = Color(0xFF69C934)
    val footColor = Color(0xFFD7F04B)
    val stroke = size * 0.12f
    val armSpread = size * (0.12f + extension * 0.32f)
    val legSpread = size * (0.12f + extension * 0.38f)

    val leftHand = Offset(left - armSpread, top + size * 0.48f)
    val rightHand = Offset(left + size + armSpread, top + size * 0.48f)
    val leftFoot = Offset(left + size * 0.23f - legSpread, top + size * 0.92f)
    val rightFoot = Offset(left + size * 0.77f + legSpread, top + size * 0.92f)
    drawLine(
        limbColor,
        Offset(left + size * 0.26f, top + size * 0.54f),
        leftHand,
        stroke,
        StrokeCap.Round
    )
    drawLine(
        limbColor,
        Offset(left + size * 0.74f, top + size * 0.54f),
        rightHand,
        stroke,
        StrokeCap.Round
    )
    drawLine(
        limbColor,
        Offset(left + size * 0.35f, top + size * 0.78f),
        leftFoot,
        stroke * 1.08f,
        StrokeCap.Round
    )
    drawLine(
        limbColor,
        Offset(left + size * 0.65f, top + size * 0.78f),
        rightFoot,
        stroke * 1.08f,
        StrokeCap.Round
    )
    listOf(leftHand, rightHand, leftFoot, rightFoot).forEach {
        drawCircle(footColor, size * 0.075f, it)
    }
}

private fun DrawScope.drawSky() {
    drawRect(Brush.verticalGradient(listOf(SkyTop, SkyBottom)))
}

private fun DrawScope.drawHills() {
    val far = Path().apply {
        moveTo(0f, size.height * 0.72f)
        quadraticTo(size.width * 0.2f, size.height * 0.5f, size.width * 0.43f, size.height * 0.72f)
        quadraticTo(size.width * 0.72f, size.height * 0.42f, size.width, size.height * 0.7f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(far, Color(0xFF8ED66C).copy(alpha = 0.55f))

    val near = Path().apply {
        moveTo(0f, size.height * 0.86f)
        quadraticTo(size.width * 0.28f, size.height * 0.63f, size.width * 0.58f, size.height * 0.86f)
        quadraticTo(size.width * 0.82f, size.height * 0.68f, size.width, size.height * 0.82f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(near, Color(0xFF4DAF62).copy(alpha = 0.45f))
}

private fun DrawScope.drawClouds(difficulty: Float) {
    val drift = (difficulty - 1f) * 18f
    drawCloud(Offset(size.width * 0.14f - drift, size.height * 0.16f), 34f)
    drawCloud(Offset(size.width * 0.72f - drift * 0.6f, size.height * 0.28f), 28f)
    drawCloud(Offset(size.width * 0.43f - drift * 0.35f, size.height * 0.08f), 20f, 0.48f)
    drawCloud(Offset(size.width * 0.91f - drift * 0.8f, size.height * 0.48f), 24f, 0.55f)
    drawCloud(Offset(size.width * 0.27f - drift * 0.5f, size.height * 0.58f), 17f, 0.38f)
}

private fun DrawScope.drawCloud(center: Offset, radius: Float, alpha: Float = 0.7f) {
    val color = Color.White.copy(alpha = alpha)
    drawCircle(color, radius, center)
    drawCircle(color, radius * 0.75f, center + Offset(radius, radius * 0.15f))
    drawCircle(color, radius * 0.62f, center - Offset(radius * 0.85f, -radius * 0.2f))
    drawRoundRect(
        color,
        topLeft = center - Offset(radius * 1.35f, 0f),
        size = Size(radius * 2.8f, radius * 0.85f),
        cornerRadius = CornerRadius(radius)
    )
}

private fun DrawScope.drawGrassPlatform(platform: PlatformState, roundAge: Float = 0f) {
    val visibleWidth = min(platform.width, size.width - platform.x)
    if (visibleWidth <= 0f || platform.x >= size.width) return
    val topLeft = Offset(platform.x, platform.y)
    drawRoundRect(
        color = Earth,
        topLeft = topLeft,
        size = Size(platform.width, platform.height),
        cornerRadius = CornerRadius(12f)
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Lime, GrassGreen)),
        topLeft = Offset(platform.x, platform.y - 6f),
        size = Size(platform.width, 16f),
        cornerRadius = CornerRadius(10f)
    )
    var bladeX = platform.x + 8f
    var bladeIndex = 0
    while (bladeX < platform.x + platform.width - 5f) {
        val sway = sin(roundAge * 3.5f + bladeIndex * 0.8f) * 2.2f
        drawLine(
            color = DeepGreen.copy(alpha = 0.7f),
            start = Offset(bladeX, platform.y + 2f),
            end = Offset(bladeX + 4f + sway, platform.y - 7f),
            strokeWidth = 2f
        )
        bladeX += 15f
        bladeIndex++
    }
    drawLine(
        color = Color(0xFFB97843).copy(alpha = 0.8f),
        start = Offset(platform.x + 12f, platform.y + 17f),
        end = Offset(platform.x + platform.width - 12f, platform.y + 17f),
        strokeWidth = 3f
    )
}

private fun DrawScope.drawBirdObstacle(bird: PlatformState, roundAge: Float) {
    if (bird.x + bird.width < 0f || bird.x > size.width) return
    val flap = sin(roundAge * 12f + bird.id) * bird.height * 0.23f
    val center = Offset(bird.x + bird.width / 2f, bird.y + bird.height / 2f)
    val bodyColor = Color(0xFF5B4BC4)
    val wingColor = Color(0xFF8B7DE5)
    drawOval(
        color = bodyColor,
        topLeft = Offset(bird.x + bird.width * 0.18f, bird.y + bird.height * 0.18f),
        size = Size(bird.width * 0.66f, bird.height * 0.64f)
    )
    val leftWing = Path().apply {
        moveTo(center.x - bird.width * 0.08f, center.y)
        quadraticTo(
            bird.x + bird.width * 0.08f,
            bird.y - flap,
            bird.x + bird.width * 0.04f,
            bird.y + bird.height * 0.12f
        )
        quadraticTo(
            bird.x + bird.width * 0.25f,
            bird.y + bird.height * 0.35f,
            center.x - bird.width * 0.08f,
            center.y
        )
        close()
    }
    val rightWing = Path().apply {
        moveTo(center.x + bird.width * 0.08f, center.y)
        quadraticTo(
            bird.x + bird.width * 0.92f,
            bird.y + flap,
            bird.x + bird.width * 0.96f,
            bird.y + bird.height * 0.12f
        )
        quadraticTo(
            bird.x + bird.width * 0.75f,
            bird.y + bird.height * 0.35f,
            center.x + bird.width * 0.08f,
            center.y
        )
        close()
    }
    drawPath(leftWing, wingColor)
    drawPath(rightWing, wingColor)
    drawCircle(Color.White, bird.height * 0.12f, center + Offset(bird.width * 0.18f, -bird.height * 0.12f))
    drawCircle(Ink, bird.height * 0.055f, center + Offset(bird.width * 0.2f, -bird.height * 0.12f))
    val beak = Path().apply {
        moveTo(bird.x + bird.width * 0.84f, center.y)
        lineTo(bird.x + bird.width, center.y + bird.height * 0.1f)
        lineTo(bird.x + bird.width * 0.84f, center.y + bird.height * 0.18f)
        close()
    }
    drawPath(beak, Color(0xFFFFC857))
}

private fun DrawScope.drawBeeObstacle(bee: PlatformState, roundAge: Float) {
    if (bee.x + bee.width < 0f || bee.x > size.width) return
    val bob = sin(roundAge * 9f + bee.id) * bee.height * 0.08f
    val center = Offset(bee.x + bee.width / 2f, bee.y + bee.height / 2f + bob)
    val wingLift = sin(roundAge * 22f + bee.id) * bee.height * 0.12f
    drawOval(
        Color.White.copy(alpha = 0.78f),
        center - Offset(bee.width * 0.28f, bee.height * 0.48f + wingLift),
        Size(bee.width * 0.32f, bee.height * 0.46f)
    )
    drawOval(
        Color.White.copy(alpha = 0.78f),
        center + Offset(bee.width * 0.02f, -bee.height * 0.48f - wingLift),
        Size(bee.width * 0.32f, bee.height * 0.46f)
    )
    drawOval(
        Color(0xFFFFC928),
        center - Offset(bee.width * 0.34f, bee.height * 0.24f),
        Size(bee.width * 0.68f, bee.height * 0.48f)
    )
    repeat(3) { stripe ->
        drawLine(
            Ink,
            Offset(center.x - bee.width * 0.14f + stripe * bee.width * 0.14f, center.y - bee.height * 0.2f),
            Offset(center.x - bee.width * 0.14f + stripe * bee.width * 0.14f, center.y + bee.height * 0.2f),
            bee.width * 0.06f,
            StrokeCap.Round
        )
    }
    drawCircle(Ink, bee.height * 0.055f, center + Offset(bee.width * 0.22f, -bee.height * 0.07f))
}

private fun DrawScope.drawBatObstacle(bat: PlatformState, roundAge: Float) {
    if (bat.x + bat.width < 0f || bat.x > size.width) return
    val center = Offset(bat.x + bat.width / 2f, bat.y + bat.height / 2f)
    val flap = sin(roundAge * 11f + bat.id) * bat.height * 0.28f
    val wingColor = Color(0xFF513A79)
    val leftWing = Path().apply {
        moveTo(center.x, center.y)
        cubicTo(
            bat.x + bat.width * 0.3f, bat.y + flap,
            bat.x + bat.width * 0.08f, bat.y + bat.height * 0.12f,
            bat.x, bat.y + bat.height * 0.55f
        )
        quadraticTo(bat.x + bat.width * 0.24f, bat.y + bat.height * 0.42f, center.x, center.y)
        close()
    }
    val rightWing = Path().apply {
        moveTo(center.x, center.y)
        cubicTo(
            bat.x + bat.width * 0.7f, bat.y - flap,
            bat.x + bat.width * 0.92f, bat.y + bat.height * 0.12f,
            bat.x + bat.width, bat.y + bat.height * 0.55f
        )
        quadraticTo(bat.x + bat.width * 0.76f, bat.y + bat.height * 0.42f, center.x, center.y)
        close()
    }
    drawPath(leftWing, wingColor)
    drawPath(rightWing, wingColor)
    drawOval(
        Color(0xFF34244F),
        center - Offset(bat.width * 0.14f, bat.height * 0.28f),
        Size(bat.width * 0.28f, bat.height * 0.56f)
    )
    drawCircle(Color(0xFFFFD166), bat.height * 0.045f, center + Offset(-bat.width * 0.05f, -bat.height * 0.08f))
    drawCircle(Color(0xFFFFD166), bat.height * 0.045f, center + Offset(bat.width * 0.05f, -bat.height * 0.08f))
}

private fun DrawScope.drawFireflyObstacle(firefly: PlatformState, roundAge: Float) {
    if (firefly.x + firefly.width < 0f || firefly.x > size.width) return
    val pulse = 0.65f + sin(roundAge * 7f + firefly.id) * 0.25f
    val center = Offset(
        firefly.x + firefly.width / 2f,
        firefly.y + firefly.height / 2f + sin(roundAge * 5f + firefly.id) * firefly.height * 0.12f
    )
    drawCircle(Color(0xFFFFF176).copy(alpha = 0.12f * pulse), firefly.width * 0.7f, center)
    drawCircle(Color(0xFFFFE04B).copy(alpha = 0.3f * pulse), firefly.width * 0.45f, center)
    drawOval(
        Color(0xFF213A3A),
        center - Offset(firefly.width * 0.22f, firefly.height * 0.24f),
        Size(firefly.width * 0.44f, firefly.height * 0.48f)
    )
    drawOval(
        Color(0xFFFFF176),
        center + Offset(-firefly.width * 0.16f, firefly.height * 0.03f),
        Size(firefly.width * 0.32f, firefly.height * 0.23f)
    )
    val wing = Color(0xFFD8F8FF).copy(alpha = 0.72f)
    drawOval(wing, center + Offset(-firefly.width * 0.38f, -firefly.height * 0.28f), Size(firefly.width * 0.32f, firefly.height * 0.34f))
    drawOval(wing, center + Offset(firefly.width * 0.06f, -firefly.height * 0.28f), Size(firefly.width * 0.32f, firefly.height * 0.34f))
}

@Composable
private fun PauseOverlay(onResume: () -> Unit, onMenu: () -> Unit) {
    CenterOverlay("PAUSED", "Your hop is waiting.") {
        PrimaryButton("RESUME", onResume)
        Spacer(Modifier.height(8.dp))
        SecondaryButton("MAIN MENU", onMenu)
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    highScore: Int,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    CenterOverlay("GAME OVER", "Score  $score     Best  $highScore") {
        PrimaryButton("TRY AGAIN", onRestart)
        Spacer(Modifier.height(8.dp))
        SecondaryButton("MAIN MENU", onMenu)
    }
}

@Composable
private fun CenterOverlay(
    title: String,
    subtitle: String,
    actions: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Ink.copy(alpha = 0.66f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            colors = CardDefaults.cardColors(containerColor = Cream),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    color = DeepGreen,
                    fontSize = 31.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Text(subtitle, color = Ink.copy(alpha = 0.78f), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(22.dp))
                actions()
            }
        }
    }
}

@Composable
private fun ScenicBackground(content: @Composable BoxScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawSky()
            drawHills()
            drawClouds(1f)
            drawGrassPlatform(
                PlatformState(
                    id = 0,
                    x = -20f,
                    y = size.height * 0.91f,
                    width = size.width + 40f,
                    height = size.height * 0.12f
                ),
                roundAge = 0f
            )
        }
        content()
    }
}

@Composable
private fun rememberFrogBitmap(): ImageBitmap {
    val resources = LocalResources.current
    return remember {
        val options = BitmapFactory.Options().apply {
            inScaled = false
        }
        (BitmapFactory.decodeResource(resources, R.drawable.player, options)
            ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)).asImageBitmap()
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DeepGreen, contentColor = Color.White),
        shape = RoundedCornerShape(22.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 2.dp,
            focusedElevation = 8.dp,
            hoveredElevation = 10.dp
        )
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun MenuTextButton(text: String, onClick: () -> Unit, color: Color = Ink) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(text, color = Ink, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SoundToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.78f))
            .padding(start = 13.dp, end = 7.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text("SOUND", color = Ink.copy(alpha = 0.62f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(
                if (checked) "ON" else "OFF",
                color = if (checked) DeepGreen else Color(0xFFC43C4D),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2DAA61),
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFD64B5F),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun BannerPanel(activity: MainActivity?) {
    if (activity == null) return
    Box(
        modifier = Modifier.fillMaxWidth().height(58.dp).background(Ink),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = ADMOB_BANNER_AD_UNIT_ID
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            onRelease = { adView -> adView.destroy() }
        )
    }
}
