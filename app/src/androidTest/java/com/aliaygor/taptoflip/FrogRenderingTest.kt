package com.aliaygor.taptoflip

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

class FrogRenderingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun spriteFollowsJumpFallAndResetWithSameEngineInstance() {
        val engine = GameEngine().apply { resize(400f, 800f) }
        val frameClock = mutableIntStateOf(0)
        val frog = ImageBitmap(16, 16)
        compose.setContent {
            Box(Modifier.fillMaxSize()) { FrogSprite(engine, frog, frameClock) }
        }
        fun spriteTop() = compose.onNodeWithContentDescription("Frog")
            .fetchSemanticsNode().boundsInRoot.top
        val initialTop = spriteTop()
        compose.runOnIdle {
            engine.jump()
            repeat(4) { engine.update(0.033f) }
            frameClock.intValue++
        }
        val jumpTop = spriteTop()
        assertTrue("The frog bitmap must rise along with the physics", jumpTop < initialTop - 40f)
        // Semantics bounds include the sprite's rotation and stretch.
        assertEquals(engine.player.y.roundToInt().toFloat(), jumpTop, 20f)
        compose.runOnIdle {
            // Observe falling independently of obstacle collisions.
            engine.player.velocityY = 300f
            repeat(4) { engine.update(0.033f) }
            frameClock.intValue++
        }
        assertTrue("The bitmap must also follow gravity", spriteTop() > jumpTop)
        compose.runOnIdle { engine.reset(); frameClock.intValue++ }
        assertEquals(initialTop, spriteTop(), 1f)
    }
}
