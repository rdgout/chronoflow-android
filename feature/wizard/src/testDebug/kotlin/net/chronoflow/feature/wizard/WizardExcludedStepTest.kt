package net.chronoflow.feature.wizard

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.shell.WizardStepKey
import net.chronoflow.feature.wizard.ui.WizardUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardExcludedStepTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private val events = mutableListOf<WizardEvent>()
    private lateinit var formatter: CopyLineFormatter

    private fun state(board: String = "WizardExcluded"): WizardUiState {
        lateinit var state: WizardUiState
        runTest(timeout = 60.seconds) {
            state = boardState(board, Locale.ENGLISH)
            formatter = wizardCopyFormatter()
        }
        return state
    }

    @Test fun reasonsAndNoteHaveTheDesignedFooterOrderAndTargets() {
        val state = state()
        compose.setContent { WizardTestHost { WizardScreen(state, formatter, events::add) } }
        compose.onNodeWithTag("wizard.excluded.reasons").onChildren().assertCountEquals(state.excluded!!.reasons.size)
        compose.onNodeWithTag("wizard.excluded.note").assertExists()
        val close = compose.onNodeWithTag("wizard.excluded.secondary-close").assertHeightIsAtLeast(48.dp)
        val primary = compose.onNodeWithTag("wizard.excluded.continue").assertHeightIsAtLeast(48.dp)
        assertTrue(close.getUnclippedBoundsInRoot().top < primary.getUnclippedBoundsInRoot().top)
        close.performClick()
        primary.performClick()
        assertEquals(listOf(WizardEvent.CloseNow, WizardEvent.Continue(WizardStepKey.Excluded)), events)
    }

    private fun fill(tag: String): Int {
        val pixels = compose.onNodeWithTag(tag).captureToImage().toPixelMap()
        return pixels[(10 * compose.density.density).toInt(), pixels.height / 2].toArgb()
    }

    @Test fun excludedUsesSurfaceTwoAndOutboundKeepsPrimary() {
        val initial = state()
        val outbound = state("WizardOutbound")
        val current = mutableStateOf(initial)
        compose.setContent { WizardTestHost { WizardScreen(current.value, formatter, {}) } }
        assertEquals(CfLightColors.surface2.toArgb(), fill("wizard.excluded.continue"))
        compose.runOnIdle { current.value = outbound }
        assertEquals(CfLightColors.primary.toArgb(), fill("wizard.outbound.continue"))
    }
}
