package com.aliaygor.taptoflip

import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.content.pm.ApplicationInfo
import androidx.activity.ComponentActivity
import com.google.android.play.core.review.ReviewManagerFactory

internal object ReviewPolicy {
    const val DAY = 86_400_000L
    fun eligible(games: Int, now: Long, firstSeen: Long, lastAttempt: Long) =
        games >= 5 && now - firstSeen >= 3 * DAY && (lastAttempt == 0L || now - lastAttempt >= 90 * DAY)
}
internal class ReviewPrompt(private val activity: ComponentActivity) {
    private val prefs = activity.getSharedPreferences("tap_to_flip", 0)
    private var requesting = false
    fun maybeRequest() {
        val now = System.currentTimeMillis()
        if (!prefs.contains("review_first_seen")) prefs.edit().putLong("review_first_seen", now).apply()
        if (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 || requesting || !activity.hasWindowFocus()) return
        if (!ReviewPolicy.eligible(PlayerProgress(activity).totalGames, now, prefs.getLong("review_first_seen", now), prefs.getLong("review_last_attempt", 0))) return
        requesting = true
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnSuccessListener { info ->
            if (!activity.isFinishing && !activity.isDestroyed) {
                prefs.edit().putLong("review_last_attempt", now).apply()
                manager.launchReviewFlow(activity, info).addOnCompleteListener { requesting = false }
            } else requesting = false
        }.addOnFailureListener { requesting = false }
    }
    fun openStore() {
        val id = activity.packageName
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")).setPackage("com.android.vending"))
        } catch (_: ActivityNotFoundException) {
            try { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id"))) }
            catch (_: ActivityNotFoundException) { }
        }
    }
}
