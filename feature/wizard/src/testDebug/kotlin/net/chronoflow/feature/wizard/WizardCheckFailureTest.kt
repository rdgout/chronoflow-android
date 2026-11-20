package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.shell.WizardStepKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardCheckFailureTest(
    private val board: String,
    private val editing: Boolean,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun failureCardAndActionsAreAccessible() {
        val base = completionBoard(board)
        val state =
            base.copy(
                checking = base.checking!!.copy(editing = editing),
                chrome = base.chrome.copy(showBack = !editing),
            )
        val events = mutableListOf<WizardEvent>()
        val haptics = RecordingHaptics()
        compose.setContent {
            WizardTestHost {
                CompositionLocalProvider(LocalHaptics provides haptics) { WizardScreen(state, events::add) }
            }
        }
        val tag = "wizard.checking.${state.checking!!.failure!!.tag}"
        compose
            .onNodeWithTag(
                tag,
            ).assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        val icon = compose.onNodeWithTag("$tag.icon", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue(icon.config.isClearingSemantics)
        compose.onNodeWithTag("wizard.checking.secondary-close").assertExists().performClick()
        assertEquals(listOf(WizardEvent.CloseFromFooter), events)
        val continueButton = compose.onNodeWithTag("wizard.checking.continue")
        if (board == "WizardCheckThrottle") {
            continueButton.assertIsNotEnabled()
        } else {
            continueButton.assertIsEnabled().performClick()
            assertEquals(listOf(WizardEvent.CloseFromFooter, WizardEvent.Continue(WizardStepKey.Checking)), events)
        }
        if (editing) {
            compose.onNodeWithTag("wizard.back").assertDoesNotExist()
        } else {
            compose.onNodeWithTag("wizard.back").assertExists()
        }
        assertTrue(haptics.events.isEmpty())
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-editing={1}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("WizardCheckOffline", false),
                arrayOf("WizardCheckServer", false),
                arrayOf("WizardCheckThrottle", false),
                arrayOf("WizardCheckOffline", true),
            )
    }
}
