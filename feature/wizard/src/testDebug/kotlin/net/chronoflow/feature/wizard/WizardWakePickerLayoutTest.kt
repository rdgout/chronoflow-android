package net.chronoflow.feature.wizard

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.wizard.shell.WizardScreen
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
class WizardWakePickerLayoutTest(
    private val profile: DeviceProfile,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(compose)

    @Test fun pickerIsCenteredInItsCard() {
        val state = completionBoard("WizardSheetWake")
        compose.setContent { WizardTestHost(profile) { WizardScreen(state, {}) } }
        val card = compose.onNodeWithTag("wizard.answer.latest_wake.card").getUnclippedBoundsInRoot()
        val picker = compose.onNodeWithTag("wizard.answer.latest_wake").getUnclippedBoundsInRoot()
        val cardCenter = (card.left.value + card.right.value) / 2
        val pickerCenter = (picker.left.value + picker.right.value) / 2
        assertTrue("card center=$cardCenter picker center=$pickerCenter", abs(cardCenter - pickerCenter) <= 1)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = listOf(arrayOf(DeviceProfiles.pixel8), arrayOf(DeviceProfiles.foldInner))
    }
}
