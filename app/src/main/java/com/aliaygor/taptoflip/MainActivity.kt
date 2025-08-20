package com.aliaygor.taptoflip

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private var mInterstitialAd: InterstitialAd? = null
    val bannerAdUnitId = "ca-app-pub-5287725227601079/1395452429"
    private val interstitialAdId = "ca-app-pub-5287725227601079/3135360764"
    private var gameOverCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        loadInterstitial()
        setContent { GameApp() }
    }

    private fun loadInterstitial() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this, interstitialAdId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { mInterstitialAd = ad }
            override fun onAdFailedToLoad(adError: LoadAdError) { mInterstitialAd = null }
        })
    }

    fun showInterstitial() {
        if (gameOverCount % 3 == 0 && mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { loadInterstitial() }
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {}
                override fun onAdShowedFullScreenContent() { mInterstitialAd = null }
            }
            mInterstitialAd?.show(this)
        }
        gameOverCount++
    }
}

@Composable
fun GameApp() {
    var screen by remember { mutableStateOf("menu") }
    MaterialTheme {
        when (screen) {
            "menu" -> MenuScreen(onStart = { screen = "game" })
            "game" -> GameScreen(onExitToMenu = { screen = "menu" })
        }
    }
}

@Composable
fun MenuScreen(onStart: () -> Unit) {
    val context = LocalContext.current
    var widthPx by remember { mutableStateOf(0f) }
    var heightPx by remember { mutableStateOf(0f) }
    var backgroundBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                widthPx = it.size.width.toFloat()
                heightPx = it.size.height.toFloat()
                if (widthPx > 0 && heightPx > 0 && backgroundBitmap == null) {
                    val options = BitmapFactory.Options().apply { inScaled = false }
                    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.background, options)
                    val scaled = Bitmap.createScaledBitmap(bitmap, widthPx.toInt(), heightPx.toInt(), true)
                    backgroundBitmap = scaled.asImageBitmap()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        backgroundBitmap?.let {
            Canvas(Modifier.fillMaxSize()) {
                drawImage(it, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "TAP-TO-FLIP",
                style = MaterialTheme.typography.headlineLarge,
                color = androidx.compose.ui.graphics.Color(0xFF003366)
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onStart) { Text("START") }
        }
    }
}

private data class Player(var x: Float, var y: Float, var vy: Float, val size: Float)
private data class Obstacle(var x: Float, var y: Float, val w: Float, val h: Float)
private enum class GameState { RUNNING, PAUSED, GAME_OVER }

@Composable
fun GameScreen(onExitToMenu: () -> Unit) {
    val activity = LocalActivity.current as? MainActivity
    val context = LocalContext.current

    var widthPx by remember { mutableStateOf(0f) }
    var heightPx by remember { mutableStateOf(0f) }
    var backgroundBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    val playerBitmap = remember {
        val options = BitmapFactory.Options().apply { inScaled = false }
        BitmapFactory.decodeResource(context.resources, R.drawable.player, options)
            .let { Bitmap.createScaledBitmap(it, 128, 128, true) }
            .asImageBitmap()
    }

    val obstacleBitmap = remember {
        val options = BitmapFactory.Options().apply { inScaled = false }
        BitmapFactory.decodeResource(context.resources, R.drawable.obstacle, options)
            .let { Bitmap.createScaledBitmap(it, 128, 128, true) }
            .asImageBitmap()
    }

    var state by remember { mutableStateOf(GameState.RUNNING) }
    var score by remember { mutableFloatStateOf(0f) }
    var high by remember { mutableFloatStateOf(0f) }

    val player = remember { Player(100f, 400f, 0f, 64f) }
    val obstacles = remember { mutableStateListOf<Obstacle>() }
    var gravity by remember { mutableFloatStateOf(2200f) }
    var jumpVelocity by remember { mutableFloatStateOf(-900f) }
    var lastSpawn by remember { mutableLongStateOf(0L) }

    val tapModifier = Modifier.pointerInput(Unit) {
        detectTapGestures {
            if (state == GameState.RUNNING) player.vy = jumpVelocity
            if (state == GameState.GAME_OVER) {
                obstacles.clear()
                player.apply { y = heightPx / 2f; vy = 0f }
                score = 0f
                state = GameState.RUNNING
            }
        }
    }

    val minGap = 220f
    val maxGap = 360f
    val minBlockWidth = 180f
    val maxBlockWidth = 300f
    val blockHeight = 80f
    val spawnIntervalMs = 1600L

    LaunchedEffect(Unit) {
        var lastNanos = System.nanoTime()
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.033f)
                lastNanos = now

                if (state == GameState.RUNNING) {
                    player.vy += gravity * dt
                    player.y += player.vy * dt
                    if (player.y < 0f) { player.y = 0f; player.vy = 0f }
                    if (player.y + player.size > heightPx) { player.y = heightPx - player.size; player.vy = 0f }

                    val nowMs = System.currentTimeMillis()
                    if (nowMs - lastSpawn > spawnIntervalMs) {
                        lastSpawn = nowMs
                        val blockW = Random.nextFloat() * (maxBlockWidth - minBlockWidth) + minBlockWidth
                        val gapY = Random.nextFloat() * (heightPx - blockHeight * 2) + blockHeight
                        val gapH = Random.nextFloat() * (maxGap - minGap) + minGap
                        val topY = (gapY - gapH).coerceAtLeast(0f)
                        val bottomY = (gapY + gapH).coerceAtMost(heightPx - blockHeight)

                        if (obstacles.none { o -> rectsOverlap(o.x, o.y, o.w, o.h, widthPx, topY, blockW, blockHeight) }) {
                            obstacles.add(Obstacle(widthPx, topY, blockW, blockHeight))
                        }
                        if (obstacles.none { o -> rectsOverlap(o.x, o.y, o.w, o.h, widthPx, bottomY, blockW, blockHeight) }) {
                            obstacles.add(Obstacle(widthPx, bottomY, blockW, blockHeight))
                        }
                    }

                    val it = obstacles.iterator()
                    while (it.hasNext()) {
                        val o = it.next()
                        o.x -= 320f * dt
                        if (o.x + o.w < -10f) it.remove()
                    }

                    if (obstacles.any { o -> rectsOverlap(player.x, player.y, player.size, player.size, o.x, o.y, o.w, o.h) }) {
                        state = GameState.GAME_OVER
                        activity?.showInterstitial()
                    }

                    score += dt * 10f
                    if (score > high) high = score
                }
            }
        }
    }

    Column {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Score: ${score.toInt()}")
            Text("High Score: ${high.toInt()}")
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(tapModifier)
                .onGloballyPositioned {
                    widthPx = it.size.width.toFloat()
                    heightPx = it.size.height.toFloat()
                    if (widthPx > 0 && heightPx > 0 && backgroundBitmap == null) {
                        val options = BitmapFactory.Options().apply { inScaled = false }
                        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.background, options)
                        val scaled = Bitmap.createScaledBitmap(bitmap, widthPx.toInt(), heightPx.toInt(), true)
                        backgroundBitmap = scaled.asImageBitmap()
                    }
                }
        ) {
            backgroundBitmap?.let {
                Canvas(Modifier.fillMaxSize()) {
                    drawImage(it, dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                }
            }

            Canvas(Modifier.fillMaxSize()) {
                obstacles.forEach {
                    drawImage(
                        image = obstacleBitmap,
                        dstOffset = androidx.compose.ui.unit.IntOffset(it.x.toInt(), it.y.toInt()),
                        dstSize = IntSize(it.w.toInt(), it.h.toInt())
                    )
                }
                drawImage(
                    image = playerBitmap,
                    dstOffset = androidx.compose.ui.unit.IntOffset(player.x.toInt(), player.y.toInt()),
                    dstSize = IntSize(player.size.toInt(), player.size.toInt())
                )
                if (state == GameState.GAME_OVER) {
                    drawContext.canvas.nativeCanvas.apply {
                        val p = android.graphics.Paint().apply {
                            textSize = 64f
                            color = android.graphics.Color.rgb(0, 51, 102)
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        drawText("GAME OVER - TAP to Restart", size.width / 2, size.height / 2, p)
                    }
                }
            }
        }

        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = activity?.bannerAdUnitId ?: ""
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(top = 16.dp)
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (state == GameState.RUNNING) {
                TextButton(onClick = { state = GameState.PAUSED }) { Text("Pause") }
            }
            TextButton(onClick = onExitToMenu) { Text("Menu") }
        }
    }
}

private fun rectsOverlap(x1: Float, y1: Float, w1: Float, h1: Float,
                         x2: Float, y2: Float, w2: Float, h2: Float): Boolean {
    return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2
}
