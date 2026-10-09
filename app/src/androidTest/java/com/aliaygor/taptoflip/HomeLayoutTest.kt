package com.aliaygor.taptoflip

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import org.junit.Rule
import org.junit.Test

class HomeLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun playAndRankingsAreVisibleWithoutScrolling() {
        compose.onNodeWithText("LET'S PLAY").assertIsDisplayed()
        compose.onNodeWithText(gameText("SIRALAMA", "RANKINGS")).assertIsDisplayed()
        compose.onNodeWithText(gameText("Hesap", "Account")).assertIsDisplayed().performClick()
        compose.onNodeWithText("Google Play Games", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText(gameText("KAPAT", "CLOSE")).performClick()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null), "home-layout.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        val progress = PlayerProgress(compose.activity)
        val original = progress.frogStyle
        val choice = if (original == FrogStyle.BLUE) FrogStyle.PINK else FrogStyle.BLUE
        try {
            compose.onNodeWithText(gameText("Kurbağa rengi", "Frog color")).performClick()
            val label = gameText(choice.tr, choice.en) + if (!progress.frogUnlocked(choice)) gameText(" • Kilitli", " • Locked") else ""
            compose.onNodeWithText(label).performClick()
            if (progress.frogUnlocked(choice)) compose.onNodeWithText(gameText(choice.tr, choice.en) + " ✓").assertIsDisplayed()
            else {
                compose.onNodeWithText(gameText("REKLAM İZLE • RENGİ AÇ", "WATCH AD • UNLOCK COLOR")).assertIsDisplayed()
                org.junit.Assert.assertEquals(original, progress.frogStyle)
            }
            val preview = compose.onNode(isDialog()).captureToImage().asAndroidBitmap()
            File(compose.activity.getExternalFilesDir(null), "frog-colors.png").outputStream().use {
                preview.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            compose.onNodeWithText(gameText("TAMAM", "DONE")).performClick()
        } finally { progress.frogStyle = original }
    }
}
