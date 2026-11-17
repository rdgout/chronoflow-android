package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfBanner
import net.chronoflow.core.designsystem.component.CfButton
import net.chronoflow.core.designsystem.component.CfButtonStyle
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.component.CfNotice
import net.chronoflow.core.designsystem.component.CfNoticeTone
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.generated.CfLightColors
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
import kotlin.math.roundToInt

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class PrimitiveColorsTest(
    private val dark: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(DeviceProfiles.pixel8, dark = dark),
            ).around(compose)

    @Test fun designedFillsAndBordersUseTheGeneratedTheme() {
        val c = if (dark) CfDarkColors else CfLightColors
        compose.setContent {
            ChronoFlowTheme(darkTheme = dark) {
                Column(
                    Modifier.fillMaxSize().background(c.background).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CfButtonStyle.entries.forEach { style ->
                        CfButton(stringResource(R.string.auth_sign_in), {}, Modifier.testTag(style.name), style = style)
                    }
                    CfBanner(
                        stringResource(R.string.auth_something_went_wrong_on_our_side),
                        null,
                        Modifier.testTag("banner"),
                    )
                    CfNotice(
                        stringResource(R.string.reminders_this_device_can_t_show_reminders),
                        stringResource(R.string.reminders_you_can_still_follow_every_step_6dddec),
                        Modifier.testTag("notice"),
                        tone = CfNoticeTone.Warning,
                    )
                    CfCard(
                        Modifier.testTag("card"),
                        shadow = false,
                        border = false,
                    ) { Text(stringResource(R.string.settings_saved)) }
                }
            }
        }
        val expected =
            mapOf(
                "Primary" to c.primary,
                "Secondary" to c.secondary,
                "Outline" to c.background,
                "Ghost" to c.background,
                "Destructive" to c.destructive,
                "Dawn" to c.dawn,
                "OnSky" to c.skyCopyBackground.compositeOver(c.background),
                "banner" to c.authErrorBackground,
                "notice" to c.settingsWarningBackground,
                "card" to c.card,
            )
        expected.forEach { (tag, color) ->
            val p = compose.onNodeWithTag(tag).captureToImage().toPixelMap()
            val actual = p[p.width / 2, (4 * DeviceProfiles.pixel8.density).roundToInt()]
            assertColor(actual, color, tag)
        }
    }

    private fun assertColor(
        actual: Color,
        expected: Color,
        tag: String,
    ) {
        assertTrue("$tag red: $actual vs $expected", abs(actual.red - expected.red) <= 2f / 255)
        assertTrue("$tag green: $actual vs $expected", abs(actual.green - expected.green) <= 2f / 255)
        assertTrue("$tag blue: $actual vs $expected", abs(actual.blue - expected.blue) <= 2f / 255)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun cases(): Collection<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}
