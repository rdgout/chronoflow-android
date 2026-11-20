package net.chronoflow.feature.wizard

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.ui.WizardUiState
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
import kotlin.time.Duration.Companion.seconds

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardDateTimeRowLayoutTest(
    private val profile: DeviceProfile,
    private val stacked: Boolean,
    private val language: AppLanguage,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, language)).around(compose)

    @Test fun pillsReflowWithoutChangingTheirIdentity() {
        lateinit var state: WizardUiState
        runTest(timeout = 60.seconds) {
            state = boardState("WizardOutbound", Locale.forLanguageTag(language.tag))
        }
        compose.setContent { WizardTestHost(profile) { WizardScreen(state, {}) } }
        val label = compose.onNodeWithTag("wizard.0.departure.label", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val date = compose.onNodeWithTag("wizard.0.departure.date").getUnclippedBoundsInRoot()
        val time = compose.onNodeWithTag("wizard.0.departure.time").getUnclippedBoundsInRoot()
        assertTrue(date.right - date.left >= 48.dp && time.right - time.left >= 48.dp)
        assertTrue(date.bottom - date.top >= 48.dp && time.bottom - time.top >= 48.dp)
        if (profile == wizardBoardProfile) {
            val visual = compose.onNodeWithTag("wizard.0.departure.date.visual", true).getUnclippedBoundsInRoot()
            assertEquals(34f, (visual.bottom - visual.top).value, .5f)
            assertEquals((date.top + date.bottom).value / 2, (visual.top + visual.bottom).value / 2, .5f)
        }
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
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-stacked={1}-{2}")
        fun cases() =
            listOf(
                arrayOf<Any>(DeviceProfiles.pixel8, false, AppLanguage.Nl),
                arrayOf<Any>(wizardBoardProfile, false, AppLanguage.En),
                arrayOf<Any>(
                    DeviceProfiles.small.copy(name = "narrowLargeFont", widthDp = 280, fontScale = 1.3f),
                    true,
                    AppLanguage.Nl,
                ),
            )
    }
}
