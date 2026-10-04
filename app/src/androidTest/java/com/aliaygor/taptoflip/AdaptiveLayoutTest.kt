package com.aliaygor.taptoflip

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class AdaptiveLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun menuAndInstructionsRemainUsableAcrossRotation() {
        val activity = compose.activity
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, activity.requestedOrientation)
        compose.onNodeWithText("LET'S PLAY").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("HOW TO PLAY").performScrollTo().performClick()
        compose.onNodeWithText("LET'S HOP").performScrollTo().assertIsDisplayed()
        compose.runOnUiThread { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        compose.waitUntil(10_000) { activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
        awaitText("LET'S HOP")
        compose.onNodeWithText("LET'S HOP").performScrollTo().assertIsDisplayed()
        saveScreenshot("adaptive-instructions-landscape.png")
        compose.onNodeWithText("BACK").performScrollTo().performClick()
        compose.onNodeWithText("LET'S PLAY").performScrollTo().assertIsDisplayed()
        saveScreenshot("adaptive-menu-landscape.png")
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("LET'S PLAY").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Ⅱ").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.autoAdvance = true
        compose.onNodeWithText("RESUME").assertIsDisplayed()
        compose.onNodeWithText("MAIN MENU").assertIsDisplayed()
        saveScreenshot("adaptive-game-landscape.png")
        compose.runOnUiThread { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
        compose.waitUntil(10_000) { activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT }
        awaitText("RESUME")
        compose.onNodeWithText("RESUME").assertIsDisplayed()
        compose.onNodeWithText("MAIN MENU").performScrollTo().performClick()
        awaitText("LET'S PLAY")
        compose.onNodeWithText("LET'S PLAY").performScrollTo().assertIsDisplayed()
        compose.runOnUiThread { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }

    private fun awaitText(text: String) {
        compose.waitUntil(15_000) {
            runCatching { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
    }

    private fun saveScreenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: error("No screenshot")
        File(instrumentation.targetContext.filesDir, name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
