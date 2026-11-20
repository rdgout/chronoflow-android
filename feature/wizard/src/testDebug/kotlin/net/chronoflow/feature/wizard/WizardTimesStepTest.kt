@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
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
class WizardTimesStepTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    private fun show(name: String) {
        lateinit var state: WizardUiState
        runTest(timeout = 60.seconds) { state = boardState(name, Locale.ENGLISH) }
        compose.setContent { WizardTestHost { WizardScreen(state, {}) } }
    }

    @Test fun outboundHasContinueAndFlightTime() {
        show("WizardOutbound")
        compose.onNodeWithTag("wizard.outbound.continue").assertIsEnabled()
        compose.onNodeWithTag("wizard.flight-time").assertExists()
        compose.onNodeWithTag("wizard.0.arrival.error").assertDoesNotExist()
        compose
            .onNodeWithTag(
                "wizard.0.departure.date",
            ).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
    }

    @Test fun emptyDisablesContinue() {
        show("WizardOutboundEmpty")
        compose.onNodeWithTag("wizard.outbound.continue").assertIsNotEnabled()
        compose.onNodeWithTag("wizard.flight-time").assertDoesNotExist()
    }

    @Test fun stopEmptyDisablesContinue() {
        show("WizardOutboundStopEmpty")
        compose.onNodeWithTag("wizard.outbound.continue").assertIsNotEnabled()
        compose.onNodeWithTag("wizard.flight-time").assertDoesNotExist()
    }

    @Test fun errorsRingOnlyTheArrivalDatePill() {
        show("WizardOutboundErrors")
        compose.onNodeWithTag("wizard.0.arrival.error").assertExists()
        compose.onNodeWithTag("wizard.0.arrival.date").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        compose.onNodeWithTag("wizard.0.arrival.time").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
        compose
            .onNodeWithTag(
                "wizard.0.departure.date",
            ).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
        compose.onNodeWithTag("wizard.flight-time").assertDoesNotExist()
    }

    @Test fun secondCardOwnsItsError() {
        show("WizardOutboundTwoErrors")
        compose.onNodeWithTag("wizard.1.arrival.error").assertExists()
        compose.onNodeWithTag("wizard.0.arrival.error").assertDoesNotExist()
        compose.onNodeWithTag("wizard.1.arrival.date").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        compose.onNodeWithTag("wizard.flight-time").assertDoesNotExist()
    }

    @Test fun outboundPastHasTheBanner() {
        show("WizardOutboundPast")
        compose.onNodeWithTag("wizard.outbound.past").assertExists()
    }

    @Test fun homePastHasItsOwnBannerAndDuration() {
        show("WizardReturnFlightPast")
        compose.onNodeWithTag("wizard.return-flight.past").assertExists()
        compose.onNodeWithTag("wizard.flight-time").assertExists()
    }

    @Test fun failedContinueWarnsOnceAfterTheCounterChanges() {
        lateinit var initial: WizardUiState
        runTest(timeout = 60.seconds) { initial = boardState("WizardOutboundErrors", Locale.ENGLISH) }
        val state = mutableStateOf(initial)
        val haptics = RecordingHaptics()
        compose.setContent {
            WizardTestHost { CompositionLocalProvider(LocalHaptics provides haptics) { WizardScreen(state.value, {}) } }
        }
        compose.runOnIdle {
            assertEquals(emptyList<HapticEvent>(), haptics.events)
            state.value =
                initial.copy(times = initial.times!!.copy(continueFailures = 1))
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(listOf(HapticEvent.Warning), haptics.events)
            state.value =
                state.value.copy(retryEnabled = false)
        }
        compose.runOnIdle { assertEquals(listOf(HapticEvent.Warning), haptics.events) }
    }
}
