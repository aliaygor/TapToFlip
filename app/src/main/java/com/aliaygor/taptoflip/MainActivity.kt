package com.aliaygor.taptoflip

import android.content.Context
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import kotlin.math.min
import kotlin.math.roundToInt

private val SkyTop = Color(0xFF70D8FF)
private val SkyBottom = Color(0xFFD7F5FF)
private val DeepGreen = Color(0xFF176B3A)
private val GrassGreen = Color(0xFF65C43B)
private val Lime = Color(0xFFA9E34B)
private val Earth = Color(0xFF8D542E)
private val Cream = Color(0xFFFFF7D6)
private val Ink = Color(0xFF16324A)

class MainActivity : ComponentActivity() {
    private var interstitialAd: InterstitialAd? = null
    private var gameOverCount = 0

    val bannerAdUnitId = "ca-app-pub-5287725227601079/1395452429"
    private val interstitialAdUnitId = "ca-app-pub-5287725227601079/3135360764"

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {
            loadInterstitial()
        }

        setContent {
            TapToFlipTheme {
                GameApp()
            }
        }
    }

    private fun loadInterstitial() {
        InterstitialAd.load(
            this,
            interstitialAdUnitId,
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

    fun score() {
        runCatching { tones?.startTone(ToneGenerator.TONE_PROP_ACK, 70) }
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
                onToggleSound = { soundEnabled = !soundEnabled },
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
    onToggleSound: () -> Unit,
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
                PillButton(
                    text = if (soundEnabled) "SOUND ON" else "SOUND OFF",
                    onClick = onToggleSound
                )
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
                Spacer(Modifier.height(10.dp))
                SecondaryButton("HOW TO PLAY", onHowToPlay)
                Spacer(Modifier.height(8.dp))
                SecondaryButton("EXIT", onExit)
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
    val frog = rememberFrogBitmap()
    var frameVersion by remember { mutableIntStateOf(0) }
    var highScore by remember { mutableIntStateOf(preferences.getInt("high_score", 0)) }
    var adShownForRound by remember { mutableStateOf(false) }
    var lastScoreEvent by remember { mutableIntStateOf(0) }

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

                if (engine.scoreEvent != lastScoreEvent) {
                    lastScoreEvent = engine.scoreEvent
                    if (soundEnabled) audio.score()
                }
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
                            if (soundEnabled) audio.jump()
                            frameVersion++
                        }
                    }
                }
        ) {
            frameVersion
            GameplayCanvas(engine)
            FrogSprite(engine, frog)

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
                        lastScoreEvent = 0
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
private fun GameplayCanvas(engine: GameEngine) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawSky()
        drawHills()
        drawClouds(engine.difficulty)
        engine.platforms.forEach(::drawGrassPlatform)

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
}

private fun DrawScope.drawCloud(center: Offset, radius: Float) {
    val color = Color.White.copy(alpha = 0.7f)
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

private fun DrawScope.drawGrassPlatform(platform: PlatformState) {
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
    while (bladeX < platform.x + platform.width - 5f) {
        drawLine(
            color = DeepGreen.copy(alpha = 0.7f),
            start = Offset(bladeX, platform.y + 2f),
            end = Offset(bladeX + 4f, platform.y - 7f),
            strokeWidth = 2f
        )
        bladeX += 15f
    }
    drawLine(
        color = Color(0xFFB97843).copy(alpha = 0.8f),
        start = Offset(platform.x + 12f, platform.y + 17f),
        end = Offset(platform.x + platform.width - 12f, platform.y + 17f),
        strokeWidth = 3f
    )
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
        PrimaryButton("HOP AGAIN", onRestart)
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
                )
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
        BitmapFactory.decodeResource(resources, R.drawable.player, options).asImageBitmap()
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DeepGreen, contentColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Black)
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
private fun PillButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.82f),
            contentColor = Ink
        ),
        shape = RoundedCornerShape(50)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    adUnitId = activity.bannerAdUnitId
                    adListener = object : com.google.android.gms.ads.AdListener() {
                        override fun onAdLoaded() {
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                        }
                    }
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        )
    }
}
