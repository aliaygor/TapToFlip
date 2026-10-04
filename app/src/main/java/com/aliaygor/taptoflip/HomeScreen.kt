package com.aliaygor.taptoflip

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF0B1721)
private val Panel = Color(0xFF162B35)
private val Accent = Color(0xFFCBF578)
private val Muted = Color(0xFFB2C6CC)

@Composable
internal fun HomeScreen(
    frog: ImageBitmap,
    progress: PlayerProgress,
    soundEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onStart: () -> Unit,
    onHowToPlay: () -> Unit,
    onShare: () -> Unit
) {
    var showScores by remember { mutableStateOf(false) }
    val best = progress.best
    val dailyTarget = progress.dailyTarget
    val today = progress.todayBest
    val float = rememberInfiniteTransition(label = "frog float")
    val lift by float.animateFloat(0f, -10f,
        infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "lift")

    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Night, Color(0xFF12302E))))) {
        val compact = maxHeight < 500.dp
        Column(
            Modifier.align(Alignment.TopCenter).widthIn(max = 480.dp).fillMaxSize()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("TAP TO FLIP", color = Color.White, fontWeight = FontWeight.Black,
                        fontSize = 15.sp, letterSpacing = 2.sp)
                    Text("THE ONE-TAP CHALLENGE", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
                }
                FilledTonalButton(onClick = { onToggleSound(!soundEnabled) },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Panel, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.semantics { contentDescription = if (soundEnabled) "Turn sound off" else "Turn sound on" }) {
                    Text(if (soundEnabled) "SOUND ON" else "SOUND OFF", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(if (compact) "One more little hop." else "One more\nlittle hop.", color = Color.White,
                fontSize = if (compact) 28.sp else 44.sp, lineHeight = if (compact) 32.sp else 46.sp,
                fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp, textAlign = TextAlign.Center)
            Text("Easy to start. A new best to chase.", color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
            Box(Modifier.size(if (compact) 76.dp else 168.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize().background(Brush.radialGradient(
                    listOf(Accent.copy(alpha = 0.2f), Color.Transparent)), CircleShape))
                Box(Modifier.fillMaxSize(0.86f).border(1.dp, Accent.copy(alpha = 0.15f), CircleShape))
                Image(frog, "Tap to Flip frog", Modifier.fillMaxSize(0.9f).graphicsLayer { translationY = lift },
                    contentScale = ContentScale.Fit)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeStat("PERSONAL BEST", best, Modifier.weight(1f), Accent)
                HomeStat("TODAY'S BEST", today, Modifier.weight(1f), Color.White)
            }
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Panel)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TODAY'S CHALLENGE", color = Accent, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(if (today >= dailyTarget) "COMPLETE" else "$today / $dailyTarget", color = Muted, fontSize = 11.sp)
                    }
                    Text(if (today >= dailyTarget) "You did it. Go for a new best."
                        else if (best > 0) "Think you can beat $best?" else "Your first record starts here.",
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    LinearProgressIndicator(progress = { (today.toFloat() / dailyTarget).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(5.dp), color = Accent,
                        trackColor = Color.White.copy(alpha = 0.08f))
                }
            }
            Surface(onClick = onStart, shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(64.dp), color = Accent) {
                Row(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Accent, Color(0xFF91EAAF))))
                    .padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("LET'S PLAY", color = Night, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text(if (best > 0) "Next record: ${best + 1}" else "Tap. Dodge. Find your rhythm.",
                            color = Night.copy(alpha = 0.72f), fontSize = 11.sp)
                    }
                    Text("→", color = Night, fontSize = 28.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeSecondary("HOW TO PLAY", Modifier.weight(1f), onHowToPlay)
                HomeSecondary("MY RECORDS", Modifier.weight(1f)) { showScores = true }
            }
            HomeSecondary("CHALLENGE A FRIEND", Modifier.fillMaxWidth(), onShare)
            Text("NO ACCOUNT NEEDED  •  v1.3", color = Muted.copy(alpha = 0.65f), fontSize = 9.sp, letterSpacing = 1.sp)
        }
    }
    if (showScores) {
        AlertDialog(onDismissRequest = { showScores = false }, containerColor = Panel,
            title = { Text("Your best hops", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Personal top 5 • saved on this device", color = Muted, fontSize = 12.sp)
                    if (progress.topScores.isEmpty()) Text("No finished runs yet. Your next hop could be #1.", color = Color.White)
                    progress.topScores.forEachIndexed { index, score ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("#${index + 1}", color = Accent, fontWeight = FontWeight.Bold)
                            Text("$score points", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }, confirmButton = { TextButton(onClick = { showScores = false }) { Text("GOT IT", color = Accent) } })
    }
}

@Composable
private fun HomeStat(label: String, value: Int, modifier: Modifier, valueColor: Color) {
    Column(modifier.background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp)).padding(16.dp)) {
        Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
        Text(value.toString(), color = valueColor, fontSize = 30.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun HomeSecondary(text: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick, modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
