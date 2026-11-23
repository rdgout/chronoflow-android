package net.chronoflow.feature.settings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.component.CfCard
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.designsystem.generated.PlanUITokens
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.settings.SettingsPage
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsComponentsLayoutTest(
    profile: DeviceProfile,
    dark: Boolean,
) : SettingsScreenTest(profile, dark = dark) {
    @Test fun missingBoxKeepsContentAlignedAndUsesWarningPixels() {
        show {
            SettingsPage(R.string.settings_body_clock, {}, "page") {
                CfCard {
                    SettingsQuestion(stringResource(R.string.settings_your_age), false, "regular") {
                        Box(Modifier.fillMaxWidth().height(PlanUITokens.space5.dp))
                    }
                    SettingsQuestion(stringResource(R.string.settings_your_age), true, "missing") {
                        Box(Modifier.fillMaxWidth().height(PlanUITokens.space5.dp))
                    }
                }
            }
        }
        val regular = compose.onNodeWithTag("regular.content", true).fetchSemanticsNode().boundsInRoot
        val missing = compose.onNodeWithTag("missing.content", true).fetchSemanticsNode().boundsInRoot
        assertEquals(regular.left, missing.left, 1f)
        val box = compose.onNodeWithTag("missing.question", true).fetchSemanticsNode().boundsInRoot
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        val colors = if (dark) CfDarkColors else CfLightColors
        val left = (box.left - PlanUITokens.space3 * profile.density).roundToInt()
        val middle = ((box.top + box.bottom) / 2).roundToInt()
        assertColor(colors.settingsWarningBackground, pixels[left + (3 * profile.density).roundToInt(), middle])
        assertColor(
            colors.settingsWarningBorder.compositeOver(colors.settingsWarningBackground),
            pixels[left + (profile.density / 2).toInt(), middle],
        )
    }

    @Test fun adaptiveTimeTilesUseTheIosMinimum() {
        show {
            SettingsPage(R.string.settings_body_clock, {}, "page") {
                CfCard {
                    SettingsTimeTilePair(
                        first = {
                            SettingsTimeTile(stringResource(R.string.settings_usual_bedtime), "23:00", {}, "bed")
                        },
                        second = {
                            SettingsTimeTile(stringResource(R.string.settings_usual_wake_time), "07:00", {}, "wake")
                        },
                    )
                }
            }
        }
        val bed = compose.onNodeWithTag("bed").fetchSemanticsNode().boundsInRoot
        val wake = compose.onNodeWithTag("wake").fetchSemanticsNode().boundsInRoot
        if (profile.widthDp == 280) {
            assertEquals(bed.left, wake.left, 1f)
            assertTrue(wake.top >= bed.bottom)
        } else {
            assertEquals(bed.top, wake.top, 1f)
            assertTrue(wake.left >= bed.right)
        }
    }

    @Test fun drinkControlsFitIncludingTheMinimumWidth() {
        show {
            SettingsPage(R.string.settings_body_clock, {}, "page") {
                CfCard { SettingsDrinkCounter(emptyMap(), { _, _ -> }) }
            }
        }
        compose.onNodeWithTag("settings.bodyClock.cupsEnergyDrink.more").performScrollTo()
        Guards.assertAll(compose, profile, headingTags = emptySet())
    }

    private fun assertColor(
        expected: Color,
        actual: Color,
    ) {
        assertEquals(expected.red, actual.red, 2f / 255)
        assertEquals(expected.green, actual.green, 2f / 255)
        assertEquals(expected.blue, actual.blue, 2f / 255)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-dark={1}")
        fun cases(): List<Array<Any>> =
            listOf(
                DeviceProfiles.pixel8,
                DeviceProfiles.small,
                DeviceProfiles.largeFont,
                DeviceProfiles.small.copy(name = "minimum280", widthDp = 280),
            ).flatMap { profile -> listOf(false, true).map { arrayOf(profile, it) } }
    }
}
