package com.aliaygor.taptoflip

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import java.io.File
import org.junit.Rule
import org.junit.Test

/** Captures actual running gameplay, without fabricating scores or ranking data. */
class StoreCaptureTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun captureGameplay() {
        compose.runOnUiThread { PlayerProgress(compose.activity).tutorialSeen = true }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("LET'S PLAY").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(4500)
        Thread.sleep(300)
        compose.onNodeWithTag("gameplay-scene").performTouchInput { click() }
        compose.mainClock.advanceTimeBy(400)
        val image = compose.onNodeWithTag("gameplay-scene").captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null), "store-gameplay-1.4.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.mainClock.advanceTimeBy(3000)
        compose.onNodeWithTag("gameplay-scene").performTouchInput { click() }
        compose.mainClock.advanceTimeBy(400)
        val second = compose.onNodeWithTag("gameplay-scene").captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null), "store-gameplay-bubbles-1.4.png").outputStream().use { second.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
