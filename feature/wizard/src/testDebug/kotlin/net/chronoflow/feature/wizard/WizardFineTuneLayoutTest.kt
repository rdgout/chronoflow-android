package net.chronoflow.feature.wizard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
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

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardFineTuneLayoutTest(
    private val small: Boolean,
) {
    private val profile = if (small) DeviceProfiles.small else wizardBoardProfile
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(compose)

    @Test fun firstTwoCardsFitOrScrollWithTheFooterFixed() {
        val state = completionBoard("WizardFineTune")
        compose.setContent { WizardTestHost(profile) { WizardScreen(state, {}) } }
        val card = compose.onNodeWithTag("wizard.preparation-card").getUnclippedBoundsInRoot()
        val footer = compose.onNodeWithTag("wizard.fine-tune.footer").getUnclippedBoundsInRoot()
        val window = compose.onNodeWithTag("wizard.window").getUnclippedBoundsInRoot()
        assertTrue(footer.bottom <= window.bottom)
        if (small) {
            assertTrue(card.bottom > footer.top)
        } else {
            assertTrue(card.bottom <= footer.top)
            val preferences = compose.onNodeWithTag("wizard.preferences").getUnclippedBoundsInRoot()
            val viewport = compose.onNodeWithTag("wizard.scroll").getUnclippedBoundsInRoot()
            assertTrue("preferences=$preferences viewport=$viewport", preferences.top >= viewport.bottom)
        }
    }

    @Test fun preferencesFollowAHeightOnlyResize() {
        val state = completionBoard("WizardFineTuneNone")
        val height = mutableStateOf(profile.heightDp.dp)
        compose.setContent {
            WizardTestHost(profile) {
                Box(Modifier.height(height.value)) { WizardScreen(state, {}) }
            }
        }
        val before =
            compose
                .onNodeWithTag("wizard.preferences")
                .getUnclippedBoundsInRoot()
                .top.value
        compose.runOnIdle { height.value -= 80.dp }
        val after =
            compose
                .onNodeWithTag("wizard.preferences")
                .getUnclippedBoundsInRoot()
                .top.value
        org.junit.Assert.assertEquals(before - 80, after, 1f)
        val viewport = compose.onNodeWithTag("wizard.scroll").getUnclippedBoundsInRoot()
        assertTrue(after >= viewport.bottom.value)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "small={0}")
        fun cases(): List<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}
