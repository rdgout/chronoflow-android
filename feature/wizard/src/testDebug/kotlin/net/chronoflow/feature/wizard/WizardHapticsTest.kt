package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.checking.CheckingOutcome
import net.chronoflow.feature.wizard.checking.WizardHapticCue
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class WizardHapticsTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun cuesPlayOnceAcrossRecompositionAndRestoration() {
        val haptics = RecordingHaptics()
        val state = mutableStateOf(completionBoard("WizardFineTune"))
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WizardTestHost {
                CompositionLocalProvider(LocalHaptics provides haptics) { WizardScreen(state.value, {}) }
            }
        }
        compose.runOnIdle { state.value = state.value.copy(cue = WizardHapticCue(CheckingOutcome.Success, 1)) }
        compose.runOnIdle { assertEquals(listOf(HapticEvent.Success), haptics.events) }
        compose.runOnIdle { state.value = state.value.copy(chrome = state.value.chrome.copy(progress = 0.81f)) }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle { assertEquals(listOf(HapticEvent.Success), haptics.events) }
        compose.runOnIdle { state.value = state.value.copy(cue = WizardHapticCue(CheckingOutcome.Warning, 2)) }
        compose.runOnIdle { state.value = state.value.copy(cue = WizardHapticCue(CheckingOutcome.Error, 3)) }
        compose.runOnIdle {
            assertEquals(listOf(HapticEvent.Success, HapticEvent.Warning, HapticEvent.Error), haptics.events)
        }
    }

    @Test fun cancelPlaysNothing() {
        val haptics = RecordingHaptics()
        val events = mutableListOf<WizardEvent>()
        val state = completionBoard("WizardSheet")
        compose.setContent {
            WizardTestHost {
                CompositionLocalProvider(LocalHaptics provides haptics) { WizardScreen(state, events::add) }
            }
        }
        compose.onNodeWithTag("wizard.answer.cancel").performClick()
        assertEquals(listOf(WizardEvent.CancelAnswer), events)
        assertEquals(emptyList<HapticEvent>(), haptics.events)
    }
}
