@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.designsystem.system.CfTimePickerDialog
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TimePickerTintTest(
    private val dark: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(DeviceProfiles.pixel8, dark = dark),
            ).around(compose)

    @Test fun timePickerSurfaceKeepsThePopoverToken() {
        compose.setContent {
            ChronoFlowTheme(darkTheme = dark) {
                CfTimePickerDialog(
                    rememberTimePickerState(initialHour = 22, initialMinute = 30, is24Hour = true),
                    {},
                    {},
                    stringResource(R.string.settings_done),
                    stringResource(R.string.settings_cancel),
                    stringResource(R.string.settings_bedtime),
                )
            }
        }
        val pixels = compose.onNode(isDialog()).captureToImage().toPixelMap()
        val actual = pixels[pixels.width - 4, pixels.height / 2]
        val expected = (if (dark) CfDarkColors else CfLightColors).popover
        assertTrue(abs(actual.red - expected.red) <= 2f / 255)
        assertTrue(abs(actual.green - expected.green) <= 2f / 255)
        assertTrue(abs(actual.blue - expected.blue) <= 2f / 255)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun cases(): Collection<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}
