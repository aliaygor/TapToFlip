package com.aliaygor.taptoflip

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class PlayGamesGuestTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun unconfiguredOnlineButtonsLeaveGuestGameUsable() {
        assumeTrue(compose.activity.getString(R.string.game_services_project_id).isBlank())
        compose.onNodeWithText(gameText("SIRALAMA", "RANKINGS")).performScrollTo().performClick()
        compose.onNodeWithText(gameText("PLAY GAMES'E BAĞLAN", "CONNECT PLAY GAMES")).performScrollTo().performClick()
        compose.onNodeWithText(gameText("Çevrim içi rekabet henüz etkin değil. Misafir olarak oynayabilirsin.",
            "Online competition is not active yet. You can play as a guest.")).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(gameText("MİSAFİR", "GUEST")).performScrollTo().performClick()
        compose.onNodeWithText(gameText("KAPAT", "CLOSE")).performClick()
        compose.onNodeWithText("LET'S PLAY").assertIsDisplayed()
    }
    @Test fun guestChoiceLeavesGameUsableWithConfiguredServices() {
        compose.onNodeWithText(gameText("SIRALAMA", "RANKINGS")).performScrollTo().performClick()
        compose.onNodeWithText(gameText("MİSAFİR", "GUEST")).performScrollTo().performClick()
        compose.onNodeWithText(gameText("Misafir modu • rekorların bu cihazda saklanır.",
            "Guest mode • records stay on this device.")).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(gameText("KAPAT", "CLOSE")).performClick()
        compose.onNodeWithText("LET'S PLAY").assertIsDisplayed()
    }
}
