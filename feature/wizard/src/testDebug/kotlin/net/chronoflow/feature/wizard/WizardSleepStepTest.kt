package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.ui.WizardUiState
import org.junit.Assert.assertEquals
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
class WizardSleepStepTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private val events = mutableListOf<WizardEvent>()
    private val haptics = RecordingHaptics()

    private fun state(board: String = "WizardSleepMember"): WizardUiState {
        lateinit var value: WizardUiState
        runTest(timeout = 60.seconds) { value = boardState(board, Locale.ENGLISH) }
        return value
    }

    private fun show(value: WizardUiState = state()) {
        compose.setContent {
            WizardTestHost {
                CompositionLocalProvider(LocalHaptics provides haptics) { WizardScreen(value, events::add) }
            }
        }
    }

    private fun radio(key: String) {
        compose
            .onNodeWithTag("wizard.choice.sleep.$key")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
    }

    @Test fun timeRowsAndChronotypesExposeSingleTargets() {
        show()
        compose
            .onNodeWithTag("wizard.sleep.habitual_bed")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
        compose
            .onNodeWithTag("wizard.sleep.habitual_wake")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
        radio("early")
        radio("intermediate")
        radio("late")
        radio("unsure")
        compose.onNodeWithTag("wizard.choice.sleep.intermediate").assertIsSelected()
        compose
            .onNodeWithTag("wizard.sleep.chronotypes")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
    }

    @Test fun notSureEmitsJsonNullAndOneSelection() {
        show()
        compose.onNodeWithTag("wizard.choice.sleep.unsure").performClick()
        assertEquals(listOf(WizardEvent.SetSleep("chronotype", JsonNull)), events)
        assertEquals(listOf(HapticEvent.Selection), haptics.events)
    }

    @Test fun bedRowOpensTheTimeSheet() {
        show()
        compose.onNodeWithTag("wizard.sleep.habitual_bed").performClick()
        assertEquals(listOf(WizardEvent.OpenTimeSheet("habitual_bed")), events)
    }

    @Test fun absentChronotypeDisablesContinue() {
        val initial = state()
        show(
            initial.copy(
                chrome = initial.chrome.copy(continueEnabled = false),
                sleep = initial.sleep!!.copy(chronotype = null),
            ),
        )
        compose.onNodeWithTag("wizard.sleep.continue").assertIsNotEnabled()
    }

    @Test fun errorSeedHasTheProfileError() {
        show(state("WizardSleepError"))
        compose.onNodeWithTag("wizard.sleep.error.profile.habitual_bed").assertExists()
    }

    @Test fun failedContinueWarnsOnceAndRestorationDoesNotReplayIt() {
        val current = mutableStateOf(state())
        val restore = StateRestorationTester(compose)
        restore.setContent {
            WizardTestHost {
                CompositionLocalProvider(LocalHaptics provides haptics) { WizardScreen(current.value, events::add) }
            }
        }
        compose.runOnIdle {
            current.value =
                current.value.copy(
                    sleep = current.value.sleep!!.copy(continueFailures = 1),
                )
        }
        compose.waitForIdle()
        assertEquals(listOf(HapticEvent.Warning), haptics.events)
        restore.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        assertEquals(listOf(HapticEvent.Warning), haptics.events)
    }

    @Test fun untouchedDoneEmitsAConfirmWithoutHaptic() {
        show(state("PickerTime"))
        compose.onNodeWithTag("wizard.time-sheet").assertExists()
        compose.onNodeWithTag("time-sheet.cancel").assertExists()
        compose.onNodeWithTag("time-sheet.done").performScrollTo().performClick()
        assertEquals(listOf(WizardEvent.ConfirmTimeSheet("habitual_bed", 23, 30, false)), events)
        assertEquals(emptyList<HapticEvent>(), haptics.events)
    }
}
