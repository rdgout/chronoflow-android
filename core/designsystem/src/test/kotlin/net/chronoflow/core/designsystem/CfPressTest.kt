package net.chronoflow.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.effect.cfClickable
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class CfPressTest(
    private val reduceMotion: Boolean,
) {
    @get:Rule val compose = createComposeRule()

    @Test fun pressedPixelsScaleAndBlendWithoutRecomposingContent() {
        var compositions = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            ChronoFlowTheme(reduceMotion = reduceMotion) {
                Box(Modifier.fillMaxSize().background(CfLightColors.background), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(100.dp)
                            .testTag("press")
                            .cfClickable({})
                            .background(CfLightColors.foreground),
                    ) {
                        SideEffect { compositions++ }
                    }
                }
            }
        }
        compose.onRoot().captureToImage()
        val before = compositions
        compose.onNodeWithTag("press").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(if (reduceMotion) 32 else 160)
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        val row = pixels.height / 2
        val run = (0 until pixels.width).count { pixels[it, row] != CfLightColors.background }
        val expectedWidth = (if (reduceMotion) 100 else 97) * 2.625f
        assertTrue("width $run vs $expectedWidth", abs(run - expectedWidth) <= 2)
        val center = pixels[pixels.width / 2, row]
        val bg = CfLightColors.background
        val fg = CfLightColors.foreground
        listOf(
            center.red to (bg.red + (fg.red - bg.red) * .85f),
            center.green to (bg.green + (fg.green - bg.green) * .85f),
            center.blue to (bg.blue + (fg.blue - bg.blue) * .85f),
        ).forEach { (a, b) ->
            assertTrue(abs(a - b) <= 2f / 255)
        }
        compose.onNodeWithTag("press").performTouchInput { up() }
        compose.mainClock.advanceTimeBy(160)
        compose.onRoot().captureToImage()
        assertEquals(before, compositions)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "reduceMotion={0}")
        fun cases(): Collection<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}
