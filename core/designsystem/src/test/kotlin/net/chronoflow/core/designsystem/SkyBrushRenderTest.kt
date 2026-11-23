package net.chronoflow.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import net.chronoflow.core.designsystem.effect.CfSky
import net.chronoflow.core.designsystem.effect.skyBackground
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.roundToInt

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class SkyBrushRenderTest(
    private val dark: Boolean,
) {
    @get:Rule val compose = createComposeRule()

    @Test fun skyShaderMatchesTheEncodedSrgbSamples() {
        compose.setContent { Box(Modifier.fillMaxSize().skyBackground(dark)) }
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        for (i in 0..10) {
            val expected = CfSky.colorAt(dark, i / 10f)
            val actual = pixels[pixels.width / 2, ((pixels.height - 1) * i / 10f).roundToInt()]
            listOf(
                actual.red to expected.red,
                actual.green to expected.green,
                actual.blue to expected.blue,
            ).forEach { (a, b) -> assertTrue(abs(a - b) <= 2f / 255) }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun cases(): Collection<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}
