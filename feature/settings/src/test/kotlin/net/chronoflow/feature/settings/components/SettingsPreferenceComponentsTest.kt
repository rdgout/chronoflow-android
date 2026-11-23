package net.chronoflow.feature.settings.components

import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsToggleable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsPreferenceComponentsTest : SettingsScreenTest() {
    @Test fun notSureHasItsTrailingDotAndRadioSelection() {
        show {
            SettingsRadioCards(
                listOf(
                    SettingsOption(
                        "",
                        stringResource(R.string.settings_not_sure),
                        stringResource(R.string.settings_we_use_your_sleep_times_instead),
                    ),
                ),
                "",
                "settings.bodyClock.chronotype",
                {},
            )
        }
        compose.onNodeWithTag("settings.bodyClock.chronotype.").assertIsSelected().assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton),
        )
    }

    @Test fun drinkButtonsRespectZeroAndTwenty() {
        var change: Pair<String, Int>? = null
        show {
            SettingsDrinkCounter(mapOf("cupsTea" to 20), { key, count -> change = key to count })
        }
        compose.onNodeWithTag("settings.bodyClock.cupsFilterCoffee.fewer").assertIsNotEnabled()
        compose.onNodeWithTag("settings.bodyClock.cupsTea.more").assertIsNotEnabled()
        compose.onNodeWithTag("settings.bodyClock.cupsFilterCoffee.more").assertIsEnabled().performClick()
        assertEquals("cupsFilterCoffee" to 1, change)
    }

    @Test fun preferenceIsOneSwitchNode() {
        var checked = false
        show {
            SettingsPreferenceSwitch(
                stringResource(R.string.settings_leave_caffeine_out_of_my_plan),
                stringResource(R.string.settings_your_plan_won_t_suggest_any_caffeine),
                false,
                { checked = it },
                "preference",
            )
        }
        compose
            .onNodeWithTag("preference")
            .assertIsToggleable()
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch),
            ).performClick()
        assertTrue(checked)
    }

    @Test fun savedLabelMergesItsDescendants() {
        show { SettingsSavedLabel("about") }
        val node = compose.onNodeWithTag("settings.bodyClock.saved.about").fetchSemanticsNode()
        assertTrue(node.config.isMergingSemanticsOfDescendants)
    }
}
