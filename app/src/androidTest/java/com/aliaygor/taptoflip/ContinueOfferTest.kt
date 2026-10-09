package com.aliaygor.taptoflip

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class ContinueOfferTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun continueOfferIsOnlyOnGameOverAndNewRunRemainsFree() {
        val offer = hasText("WATCH AD", substring = true) or hasText("PREPARING CONTINUE", substring = true)
        compose.onNode(offer).assertDoesNotExist()
        // This test covers game-over actions rather than the first-run tutorial.
        compose.runOnUiThread { PlayerProgress(compose.activity).tutorialSeen = true }
        compose.onNodeWithText("LET'S PLAY").performScrollTo().performClick()
        compose.waitUntil(60_000) { compose.onAllNodesWithText("ONE MORE TRY").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(offer).performScrollTo().assertIsDisplayed()
        compose.waitUntil(5_000) { runCatching { compose.onNodeWithText("ONE MORE TRY").assertIsEnabled(); true }.getOrDefault(false) }
        compose.onNodeWithText("MAIN MENU").performScrollTo().performClick()
        compose.onNodeWithText("LET'S PLAY").performScrollTo().assertIsDisplayed()
        compose.onNode(offer).assertDoesNotExist()
    }
}
