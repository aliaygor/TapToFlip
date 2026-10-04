package com.aliaygor.taptoflip

import android.content.pm.ApplicationInfo
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.*
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class RewardedContinue(private val activity: ComponentActivity, private val onShown: () -> Unit) {
    private val unitId = if (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
        "ca-app-pub-3940256099942544/5224354917"
    else activity.getString(R.string.rewarded_ad_unit_id).trim()
    val configured get() = unitId.isNotEmpty()
    var ready by mutableStateOf(false)
        private set
    var showing by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    private var loading = false
    private var ad: RewardedAd? = null
    private var loadedAt = 0L
    private var failures = 0

    fun load() {
        if (!configured || loading || showing || ad != null || activity.isDestroyed) return
        loading = true
        RewardedAd.load(activity, unitId, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(value: RewardedAd) {
                loading = false; failures = 0; ad = value; ready = true; message = null
                loadedAt = SystemClock.elapsedRealtime()
                Log.i("TapToFlipAds", "Rewarded continue ready")
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                loading = false; ready = false; ad = null
                message = "Ad unavailable. You can try again or start a new run."
                Log.w("TapToFlipAds", "Rewarded load failed: ${error.code} ${error.message}")
                val wait = (30_000L * (1L shl failures.coerceAtMost(3))).coerceAtMost(240_000L)
                failures++
                activity.lifecycleScope.launch { delay(wait); load() }
            }
        })
    }

    fun show(onResult: (Boolean) -> Unit) {
        if (showing) return
        val current = ad
        if (current == null || SystemClock.elapsedRealtime() - loadedAt >= 3_600_000L) {
            ad = null; ready = false; load()
            onResult(false)
            return
        }
        showing = true; ready = false; ad = null
        var earned = false
        var delivered = false
        fun finish(success: Boolean) {
            if (delivered) return
            delivered = true; showing = false
            message = if (success) null else "Ad not completed. Your run has not continued."
            onResult(success)
            load()
        }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() { onShown() }
            override fun onAdDismissedFullScreenContent() = finish(earned)
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w("TapToFlipAds", "Rewarded show failed: ${error.code} ${error.message}")
                finish(false)
            }
        }
        current.show(activity) { earned = true }
    }
}
