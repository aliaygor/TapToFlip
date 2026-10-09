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
    competition: PlayGamesCompetition? = null,
    soundEnabled: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onStart: () -> Unit,
    onHowToPlay: () -> Unit,
    onShare: () -> Unit,
    onRate: () -> Unit = {}
) {
    val homeActivity = androidx.activity.compose.LocalActivity.current as? MainActivity
    LaunchedEffect(competition) { competition?.refresh(); homeActivity?.reviewPrompt?.maybeRequest() }
    var mode by remember { mutableStateOf(progress.selectedMode.takeUnless { it == GameMode.SURVIVAL } ?: GameMode.CLASSIC) }
    LaunchedEffect(mode) { progress.selectedMode = mode }
    var style by remember { mutableStateOf(progress.frogStyle) }
    var previewStyle by remember { mutableStateOf(progress.frogStyle) }
    var unlockVersion by remember { mutableIntStateOf(0) }
    var unlockMessage by remember { mutableStateOf<String?>(null) }
    val reward = (androidx.activity.compose.LocalActivity.current as? MainActivity)?.rewardedContinue
    var showAppearance by remember { mutableStateOf(false) }
    var showScores by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }
    var showTasks by remember { mutableStateOf(false) }
    val best = progress.modeBest(mode)
    val today = progress.modeTodayBest(mode)
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
            Box(Modifier.size(if (compact) 64.dp else 116.dp), contentAlignment = Alignment.Center) {
                Image(styledFrogBitmap(frog, style), "Tap to Flip frog", Modifier.fillMaxSize().graphicsLayer { translationY = lift }, contentScale = ContentScale.Fit, colorFilter = style.filter())
            }
            Text(gameText("Ritmini bul. Rekorunu geç.", "Find your rhythm. Beat your best."), color = Muted, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(GameMode.CLASSIC, GameMode.TIME_ATTACK).forEach { item ->
                    FilterChip(selected = mode == item, onClick = { mode = item; progress.selectedMode = item; Telemetry.emit("game_mode_selected", "mode" to item) },
                        label = { Text(when (item) { GameMode.CLASSIC -> gameText("Sonsuz koşu", "Endless run"); GameMode.TIME_ATTACK -> gameText("60 saniye yarış", "60-second race"); GameMode.SURVIVAL -> gameText("Hayatta kal", "Survival") }, fontSize = 11.sp) })
                }
            }
            Text(if (mode == GameMode.CLASSIC)
                gameText("Süre sınırı yok. Engellerden kaç, dayanabildiğin kadar puan topla.", "No time limit. Dodge obstacles and score as long as you can.")
                else gameText("60 saniyede en yüksek puanı topla. Çarpışırsan tur erken biter.", "Score as much as you can in 60 seconds. A collision ends your run early."),
                color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeStat("PERSONAL BEST", best, Modifier.weight(1f), Accent)
                HomeStat("TODAY'S BEST", today, Modifier.weight(1f), Color.White)
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
                HomeSecondary(gameText("SIRALAMA", "RANKINGS"), Modifier.weight(1f)) { showAccount = true }
                HomeSecondary(gameText("REKORLARIM", "MY RECORDS"), Modifier.weight(1f)) { showScores = true }
            }
            if (competition != null) {
                TextButton(onClick = { showAccount = true }) {
                    Text(if (competition.enabled && competition.authenticated) competition.playerName + " • " + gameText("Hesap", "Account")
                        else gameText("Misafir • Play Games'e bağlan", "Guest • Connect Play Games"), color = Muted, fontSize = 12.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = onHowToPlay) { Text(gameText("Nasıl oynanır", "How to play"), fontSize = 11.sp) }
                TextButton(onClick = { showTasks = true }) { Text(gameText("Görevler", "Tasks") + " • ${progress.rewardStars} ★", fontSize = 11.sp) }
                TextButton(onClick = { showAppearance = true }) { Text(gameText("Kurbağa rengi", "Frog color"), fontSize = 11.sp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onRate) { Text(gameText("Google Play'de değerlendir", "Rate on Google Play"), color = Muted, fontSize = 11.sp) }
                TextButton(onClick = onShare) { Text(gameText("Arkadaşınla paylaş", "Share with a friend"), color = Muted, fontSize = 11.sp) }
            }
            Text(gameText("Yorumların TapToFlip'i geliştirmemize yardımcı olur.", "Your feedback helps us improve TapToFlip."), color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
            if (showAccount && competition != null) {
                AlertDialog(onDismissRequest = { showAccount = false }, containerColor = Panel,
                    title = { Text(gameText("Hesap ve sıralama", "Account & rankings")) },
                    confirmButton = { TextButton(onClick = { showAccount = false }) { Text(gameText("KAPAT", "CLOSE")) } },
                    text = {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(gameText("DÜNYA SIRALAMASI", "WORLD RANKINGS"), color = Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(if (competition.enabled && competition.authenticated) competition.playerName
                            else gameText("Misafir olarak hemen oynayabilir veya rekabete katılabilirsin.", "Play immediately as a guest or join the competition."), color = Color.White, fontSize = 12.sp)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { competition.guest() }, enabled = !competition.busy, modifier = Modifier.weight(1f)) {
                                Text(if (competition.enabled) gameText("BAĞLANTIYI KES", "DISCONNECT") else gameText("MİSAFİR", "GUEST"), fontSize = 10.sp, color = Muted)
                            }
                            Button(onClick = { competition.join() }, enabled = !competition.busy && !(competition.enabled && competition.authenticated), modifier = Modifier.weight(1f)) {
                                Text(when {
                                    competition.enabled && competition.authenticated -> gameText("BAĞLANDI ✓", "CONNECTED ✓")
                                    competition.busy -> gameText("BAĞLANIYOR…", "CONNECTING…")
                                    else -> gameText("PLAY GAMES'E BAĞLAN", "CONNECT PLAY GAMES")
                                }, fontSize = 10.sp)
                            }
                        }
                        if (!competition.configured) Text(gameText("Çevrim içi rekabet yakında. Şimdilik misafir olarak oynayabilirsin.", "Online competition is coming soon. Play as a guest for now."), color = Muted, fontSize = 11.sp)
                        Text(gameText("Seçili modun sıralaması", "Rankings for the selected mode"), color = Muted, fontSize = 11.sp)
                        OutlinedButton(onClick = { competition.openLeaderboard(mode) }, enabled = !competition.busy, modifier = Modifier.fillMaxWidth()) {
                            Text(gameText("SKOR TABLOSUNU AÇ", "OPEN LEADERBOARD"), color = Accent, fontSize = 12.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(
                                gameText("Günlük", "Daily") to com.google.android.gms.games.leaderboard.LeaderboardVariant.TIME_SPAN_DAILY,
                                gameText("Haftalık", "Weekly") to com.google.android.gms.games.leaderboard.LeaderboardVariant.TIME_SPAN_WEEKLY,
                                gameText("Tümü", "All time") to com.google.android.gms.games.leaderboard.LeaderboardVariant.TIME_SPAN_ALL_TIME
                            ).forEach { (label, span) ->
                                TextButton(onClick = { competition.openLeaderboard(mode, span) }, enabled = !competition.busy, modifier = Modifier.weight(1f)) { Text(label, color = Accent, fontSize = 11.sp) }
                            }
                        }
                        if (competition.enabled && competition.authenticated && competition.readyFor(mode))
                            Text(gameText("Rekabet turları aynı zorlukla başlar. Reklamla devam edilen skorlar kişisel kalır.", "Ranked runs use standard difficulty. Scores continued with an ad stay personal."), color = Muted, fontSize = 10.sp)
                        competition.message?.let { Text(it, color = Accent, fontSize = 11.sp) }
                    }
                })
            }
    if (showAppearance) {
        LaunchedEffect(Unit) { reward?.load() }
        AlertDialog(onDismissRequest = { showAppearance = false }, containerColor = Panel,
            title = { Text(gameText("Kurbağanı seç", "Choose your frog")) },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(styledFrogBitmap(frog, previewStyle), "Frog color preview", Modifier.size(120.dp), colorFilter = previewStyle.filter())
                unlockVersion
                FrogStyle.entries.forEach { option ->
                    OutlinedButton(onClick = {
                        previewStyle = option
                        if (progress.frogUnlocked(option)) { style = option; progress.frogStyle = option }
                        unlockMessage = null
                    }, modifier = Modifier.fillMaxWidth(), enabled = reward?.showing != true) {
                        Text(gameText(option.tr, option.en) + if (!progress.frogUnlocked(option)) gameText(" • Kilitli", " • Locked") else if (style == option) " ✓" else "", color = option.color(Accent))
                    }
                }
                if (!progress.frogUnlocked(previewStyle)) {
                    OutlinedButton(onClick = {
                        if (progress.buyFrog(previewStyle)) {
                            progress.frogStyle = previewStyle; style = previewStyle; unlockVersion++
                        }
                    }, enabled = progress.rewardStars >= 20 && reward?.showing != true) {
                        Text(gameText("20 ★ ile aç", "Unlock for 20 ★") + " • ${progress.rewardStars} ★")
                    }
                    Button(onClick = {
                        val selected = previewStyle
                        reward?.show(onEarned = {
                            progress.unlockFrog(selected); progress.frogStyle = selected
                            style = selected; unlockVersion++
                        }) { earned ->
                            unlockMessage = if (earned) gameText("Renk kalıcı olarak açıldı!", "Color permanently unlocked!")
                                else gameText("Reklam tamamlanmadı veya hazır değil. Renk kilitli kaldı.", "Ad incomplete or unavailable. Color remains locked.")
                        }
                    }, enabled = reward?.ready == true && !reward.showing) {
                        Text(gameText("REKLAM İZLE • RENGİ AÇ", "WATCH AD • UNLOCK COLOR"), fontSize = 11.sp)
                    }
                    if (reward?.ready != true) Text(gameText("Reklam hazırlanıyor. Daha sonra tekrar deneyebilirsin.", "Ad is preparing. You can try again later."), color = Muted, fontSize = 11.sp)
                }
                unlockMessage?.let { Text(it, fontSize = 11.sp) }
                Text(gameText("Yeşil ücretsiz. Görünümleri reklamla veya 20 yıldızla kalıcı aç. Puan avantajı vermez.", "Green is free. Unlock appearances with a rewarded ad or 20 stars. No score advantage."), color = Muted, fontSize = 11.sp)
            } }, confirmButton = { TextButton(onClick = { showAppearance = false }) { Text(gameText("TAMAM", "DONE")) } })
    }
    if (showTasks) {
        AlertDialog(onDismissRequest = { showTasks = false }, containerColor = Panel,
            title = { Text(gameText("Günlük görevler", "Daily tasks")) },
            text = { val tasks = progress.tasks
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(gameText("Puan", "Points") + " ${tasks.points}/100")
                    Text(gameText("Oyun", "Games") + " ${tasks.games}/3")
                    Text(gameText("Kombo", "Combos") + " ${tasks.combos}/5")
                    Text("${progress.rewardStars} ★")
                }
            }, confirmButton = { TextButton(onClick = { showTasks = false }) { Text(gameText("KAPAT", "CLOSE")) } })
    }
    if (showScores) {
        AlertDialog(onDismissRequest = { showScores = false }, containerColor = Panel,
            title = { Text("Your best hops", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(gameText("Her modun kişisel rekoru", "Personal best by mode"), color = Muted, fontSize = 12.sp)
                    GameMode.entries.forEach { item -> Text("${item.name}: ${progress.modeBest(item)}", color = Color.White) }
                    Text(gameText("Klasik • en iyi 5", "Classic • top 5"), color = Muted, fontSize = 12.sp)
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
