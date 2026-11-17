package net.chronoflow.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@Layout
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class CfFocusRingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun keyboardFocusDrawsTheRingOutsideTheBounds() {
        val requester = FocusRequester()
        lateinit var keyboardMode: () -> Unit
        compose.setContent {
            val input = LocalInputModeManager.current
            keyboardMode = { input.requestInputMode(InputMode.Keyboard) }
            ChronoFlowTheme {
                Box(Modifier.fillMaxSize().background(CfLightColors.background), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(100.dp)
                            .focusRequester(requester)
                            .cfClickable({})
                            .background(CfLightColors.card),
                    )
                }
            }
        }
        compose.runOnIdle {
            keyboardMode()
            requester.requestFocus()
        }
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        val x = ((pixels.width - 100 * 2.625f) / 2 - 2.625f).toInt()
        val pixel = pixels[x, pixels.height / 2]
        val expected = CfLightColors.ring
        assertTrue(abs(pixel.red - expected.red) <= 2f / 255)
        assertTrue(abs(pixel.green - expected.green) <= 2f / 255)
        assertTrue(abs(pixel.blue - expected.blue) <= 2f / 255)
    }
}
