package net.chronoflow.feature.wizard

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import net.chronoflow.feature.wizard.times.WizardDateTimeRow
import net.chronoflow.feature.wizard.times.WizardTimesUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardDateTimeRowLayoutTest(
    private val profile: DeviceProfile,
    private val stacked: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, AppLanguage.Nl)).around(compose)

    @Test fun pillsReflowWithoutChangingTheirIdentity() {
        val ui =
            WizardTimesUi.from(
                WizardBoardCaptureSeed.draft("WizardOutbound", Locale.forLanguageTag("nl")),
                false,
                now = WizardBoardCaptureSeed.now,
            )
        compose.setContent {
            WizardTestHost(
                profile,
            ) { WizardDateTimeRow(ui.cards[0].departure, ui.timeFormat, { _, _ -> }) }
        }
        val label = compose.onNodeWithTag("wizard.0.departure.label", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val date = compose.onNodeWithTag("wizard.0.departure.date").getUnclippedBoundsInRoot()
        val time = compose.onNodeWithTag("wizard.0.departure.time").getUnclippedBoundsInRoot()
        assertEquals(date.top.value, time.top.value, .5f)
        if (stacked) {
            assertTrue(date.top >= label.bottom)
            assertEquals((date.right - date.left).value, (time.right - time.left).value, .5f)
        } else {
            assertEquals(label.top.value, date.top.value, .5f)
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-stacked={1}")
        fun cases() =
            listOf(
                arrayOf<Any>(DeviceProfiles.pixel8, false),
                arrayOf<Any>(
                    DeviceProfiles.small.copy(name = "narrowLargeFont", widthDp = 280, fontScale = 1.3f),
                    true,
                ),
            )
    }
}
