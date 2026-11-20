@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class CfButtonTest {
    @get:Rule val compose = createComposeRule()

    @Test fun disabledAlphaCanMatchTheWizard() {
        compose.setContent {
            ChronoFlowTheme(reduceMotion = true) {
                Box(Modifier.background(CfLightColors.background).testTag("background")) {
                    CfButton(
                        "",
                        {},
                        Modifier.size(200.dp, 52.dp).testTag("button"),
                        enabled = false,
                        disabledAlpha = 0.4f,
                    )
                }
            }
        }
        compose.onNodeWithTag("button").assertIsNotEnabled()
        val pixels = compose.onNodeWithTag("background").captureToImage().toPixelMap()
        val expected = CfLightColors.primary.copy(alpha = 0.4f).compositeOver(CfLightColors.background)
        val actual = pixels[pixels.width / 2, 5]
        assertEquals(expected.red, actual.red, 1.1f / 255)
        assertEquals(expected.green, actual.green, 1.1f / 255)
        assertEquals(expected.blue, actual.blue, 1.1f / 255)
    }
}
