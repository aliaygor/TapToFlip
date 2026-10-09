package com.aliaygor.taptoflip

import android.app.Activity
import android.util.Log
import com.google.android.gms.common.api.ApiException
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.leaderboard.LeaderboardVariant

/** Google owns authentication and the real worldwide leaderboard UI. */
internal class PlayGamesCompetition(private val activity: ComponentActivity) {
    private val progress = PlayerProgress(activity)
    private val config = CompetitionConfig(activity.getString(R.string.game_services_project_id), mapOf(
        GameMode.CLASSIC to activity.getString(R.string.leaderboard_classic),
        GameMode.TIME_ATTACK to activity.getString(R.string.leaderboard_time_attack),
        GameMode.SURVIVAL to activity.getString(R.string.leaderboard_survival)))
    val configured get() = config.hasProject
    var authenticated by mutableStateOf(false); private set
    var enabled by mutableStateOf(progress.competitionEnabled); private set
    var busy by mutableStateOf(false); private set
    var playerName by mutableStateOf(""); private set
    var message by mutableStateOf<String?>(null); private set
    private var playerId: String? = null
    private var refreshing = false
    private var joinRequested = false
    private val submitting = mutableSetOf<GameMode>()
    private val leaderboardLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK && result.resultCode != Activity.RESULT_CANCELED) {
            Log.w("TapToFlipGames", "Leaderboard returned code ${result.resultCode}")
            message = gameText("Skor tablosu açılamadı", "Could not open leaderboard") + " (${result.resultCode})."
        }
        refresh()
    }
    fun readyFor(mode: GameMode) = config.readyFor(mode)
    fun rankedOwner(mode: GameMode): String? = if (enabled && authenticated && readyFor(mode)) playerId else null
    fun guest() {
        joinRequested = false
        enabled = false
        progress.competitionEnabled = false
        message = gameText("Misafir modu • rekorların bu cihazda saklanır.", "Guest mode • records stay on this device.")
    }
    fun join() {
        if (!configured) { unavailable(); return }
        if (busy || refreshing) return
        joinRequested = true
        busy = true
        message = null
        PlayGames.getGamesSignInClient(activity).signIn()
            .addOnSuccessListener { result ->
                if (result.isAuthenticated) loadPlayer()
                else { busy = false; joinRequested = false; signInFailed() }
            }.addOnFailureListener { busy = false; joinRequested = false; signInFailed() }
    }
    fun refresh() {
        if (!configured || refreshing || busy || activity.isDestroyed) return
        refreshing = true
        PlayGames.getGamesSignInClient(activity).isAuthenticated
            .addOnSuccessListener { result ->
                refreshing = false
                if (result.isAuthenticated) loadPlayer()
                else { authenticated = false; playerId = null; playerName = "" }
            }.addOnFailureListener { refreshing = false; authenticated = false; playerId = null }
    }
    private fun loadPlayer() {
        busy = true
        PlayGames.getPlayersClient(activity).currentPlayer
            .addOnSuccessListener { player ->
                playerId = player.playerId
                playerName = player.displayName
                authenticated = true
                busy = false
                if (joinRequested) {
                    enabled = true
                    progress.competitionEnabled = true
                    message = gameText("Rekabete katıldın! Yeni turların sıralamaya girebilir.", "Competition enabled! Your new runs can enter the rankings.")
                    joinRequested = false
                }
                flush()
            }.addOnFailureListener { busy = false; authenticated = false; playerId = null; joinRequested = false; signInFailed() }
    }
    fun openLeaderboard(mode: GameMode, timeSpan: Int = LeaderboardVariant.TIME_SPAN_ALL_TIME) {
        if (!readyFor(mode)) { unavailable(); return }
        if (!enabled || !authenticated) { message = gameText("Sıralamayı görmek için önce Play Games'e bağlan.", "Connect to Play Games to view the rankings."); return }
        if (busy) return
        busy = true
        PlayGames.getLeaderboardsClient(activity).getLeaderboardIntent(config.boards.getValue(mode), timeSpan)
            .addOnSuccessListener { intent ->
                busy = false
                if (!activity.isFinishing && !activity.isDestroyed) leaderboardLauncher.launch(intent)
            }.addOnFailureListener { error ->
                busy = false
                val code = (error as? ApiException)?.statusCode
                Log.w("TapToFlipGames", "Leaderboard request failed, status=$code", error)
                message = gameText("Skor tablosu açılamadı. Tekrar dene.", "Could not open leaderboard. Please retry.") + (code?.let { " ($it)" } ?: "")
            }
    }
    fun finishRun(engine: GameEngine, owner: String?) {
        val result = RankedResult(owner.orEmpty(), engine.mode, engine.score, engine.ranked,
            engine.state == GameStatus.GAME_OVER, engine.reviveUsed)
        if (!CompetitionPolicy.eligible(result)) return
        progress.queueRankedScore(result)
        flush()
    }
    private fun flush() {
        val owner = playerId ?: return
        if (!authenticated || !enabled) return
        for (mode in GameMode.entries) {
            if (!readyFor(mode) || mode in submitting) continue
            val score = progress.pendingRankedScore(owner, mode) ?: continue
            val submittedDay = CompetitionPolicy.leaderboardDay(System.currentTimeMillis())
            submitting += mode
            PlayGames.getLeaderboardsClient(activity).submitScoreImmediate(config.boards.getValue(mode), score.toLong(), CompetitionPolicy.RULESET)
                .addOnSuccessListener {
                    progress.ackRankedScore(owner, mode, score, submittedDay)
                    submitting -= mode
                    message = gameText("Skorun dünya sıralamasına gönderildi!", "Your score was submitted to the world rankings!")
                    // A higher result may have been queued while this request ran.
                    if (playerId == owner && progress.pendingRankedScore(owner, mode) != null) flush()
                }.addOnFailureListener {
                    submitting -= mode
                    message = gameText("Skor saklandı. Aynı gün bağlantı kurulunca tekrar gönderilecek.", "Score saved. We will retry when connected again today.")
                }
        }
    }
    private fun unavailable() { message = gameText("Çevrim içi rekabet henüz etkin değil. Misafir olarak oynayabilirsin.", "Online competition is not active yet. You can play as a guest.") }
    private fun signInFailed() { message = gameText("Play Games bağlantısı kurulamadı. Misafir olarak devam edebilir veya tekrar deneyebilirsin.", "Could not connect to Play Games. Continue as a guest or retry.") }
}
