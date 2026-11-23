package net.chronoflow.feature.settings

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsShellInsetTest(
    dark: Boolean,
) : SettingsScreenTest(dark = dark) {
    @Test fun insetUsesTheAppBackground() {
        assertTrue(profile.insets.bottom > 0)
        var background = Color.Unspecified
        show {
            val color = CfTheme.colors.background
            SideEffect { background = color }
            SettingsBoardFixture("Home", shell = true)
        }
        val image = compose.onNodeWithTag("settings.capture.window").captureToImage()
        val pixel = IntArray(1)
        image.readPixels(pixel, image.width / 2, image.height - 1, 1, 1)
        assertEquals(background.toArgb(), pixel.single())
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun cases(): List<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}
