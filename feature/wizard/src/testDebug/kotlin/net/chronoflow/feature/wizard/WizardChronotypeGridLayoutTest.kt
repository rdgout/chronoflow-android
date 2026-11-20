package net.chronoflow.feature.wizard

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.Guards
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
class WizardChronotypeGridLayoutTest(
    private val profile: DeviceProfile,
    private val columns: Int,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, AppLanguage.Nl)).around(compose)

    @Test fun gridUsesEqualCellsAndFitsTheLongestHint() {
        lateinit var state: WizardUiState
        runTest(timeout = 60.seconds) { state = boardState("WizardSleep", Locale.forLanguageTag("nl")) }
        compose.setContent { WizardTestHost(profile) { WizardScreen(state, {}) } }
        val first = compose.onNodeWithTag("wizard.choice.sleep.early").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithTag("wizard.choice.sleep.intermediate").getUnclippedBoundsInRoot()
        val third = compose.onNodeWithTag("wizard.choice.sleep.late").getUnclippedBoundsInRoot()
        val fourth = compose.onNodeWithTag("wizard.choice.sleep.unsure").getUnclippedBoundsInRoot()
        assertTrue(first.height.value >= 76f)
        assertTrue(second.height.value >= 76f)
        assertTrue(third.height.value >= 76f)
        assertTrue(fourth.height.value >= 76f)
        assertEquals(first.width.value, second.width.value, .5f)
        if (columns == 2) {
            assertEquals(first.top.value, second.top.value, .5f)
            assertEquals(first.height.value, second.height.value, .5f)
            assertEquals(third.height.value, fourth.height.value, .5f)
        } else {
            assertTrue(second.top >= first.bottom)
            assertEquals(first.left.value, second.left.value, .5f)
        }
        Guards.assertTextFits(compose, profile)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-columns")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf(DeviceProfiles.pixel8, 2),
                arrayOf(DeviceProfiles.small.copy(name = "narrow340", widthDp = 340), 1),
                arrayOf(DeviceProfiles.largeFont, 2),
            )
    }
}
