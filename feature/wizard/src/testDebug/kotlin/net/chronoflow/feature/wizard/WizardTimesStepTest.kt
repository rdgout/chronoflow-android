package net.chronoflow.feature.wizard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.shell.WizardStepKey
import net.chronoflow.feature.wizard.times.WizardTimesStep
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

    @Test fun returningToOutboundDoesNotReplayHomeFailureFeedback() {
        lateinit var outbound: WizardUiState
        lateinit var home: WizardUiState
        lateinit var choice: WizardUiState
        runTest(timeout = 60.seconds) {
            outbound = boardState("WizardOutboundScrollErrors", Locale.ENGLISH)
            home = boardState("WizardReturnFlight", Locale.ENGLISH)
            choice = boardState("WizardReturn", Locale.ENGLISH)
        }
        val state = mutableStateOf(outbound)
        val haptics = RecordingHaptics()
        compose.setContent {
            WizardTestHost {
                CompositionLocalProvider(LocalHaptics provides haptics) {
                    val holder = rememberSaveableStateHolder()
                    val current = state.value
                    holder.SaveableStateProvider(current.step.debugName) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("wizard.scroll")) {
                            current.times?.let { WizardTimesStep(it, {}) }
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("wizard.scroll").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100_000f) }
        compose.waitForIdle()
        val before = compose.onNodeWithTag("wizard.flight.errors.0").getUnclippedBoundsInRoot()
        val viewport = compose.onNodeWithTag("wizard.scroll").getUnclippedBoundsInRoot()
        assertTrue(before.bottom < viewport.top)
        compose.runOnIdle { state.value = home }
        compose.waitForIdle()
        compose.runOnIdle { state.value = home.copy(times = home.times!!.copy(continueFailures = 1)) }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(listOf(HapticEvent.Warning), haptics.events)
            state.value = choice
        }
        compose.waitForIdle()
        compose.runOnIdle { state.value = outbound }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(WizardStepKey.Outbound, state.value.step)
            assertEquals(listOf(HapticEvent.Warning), haptics.events)
        }
        assertEquals(before, compose.onNodeWithTag("wizard.flight.errors.0").getUnclippedBoundsInRoot())
    }

    @Test fun firstFailureAfterRestorationWarnsOnceAndRevealsTheFirstError() {
        lateinit var initial: WizardUiState
        runTest(timeout = 60.seconds) { initial = boardState("WizardOutboundScrollErrors", Locale.ENGLISH) }
        val haptics = RecordingHaptics()
        lateinit var current: MutableState<WizardUiState>
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            val state = remember { mutableStateOf(initial) }
            SideEffect { current = state }
            WizardTestHost {
                CompositionLocalProvider(LocalHaptics provides haptics) { WizardScreen(state.value, {}) }
            }
        }
        compose.runOnIdle { current.value = initial.copy(times = initial.times!!.copy(continueFailures = 1)) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(listOf(HapticEvent.Warning), haptics.events) }

        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertEquals(0, current.value.times!!.continueFailures)
            assertEquals(listOf(HapticEvent.Warning), haptics.events)
        }
        compose.onNodeWithTag("wizard.scroll").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 100_000f) }
        compose.waitForIdle()
        val viewport = compose.onNodeWithTag("wizard.scroll").getUnclippedBoundsInRoot()
        val footer = compose.onNodeWithTag("wizard.outbound.footer").getUnclippedBoundsInRoot()
        val before = compose.onNodeWithTag("wizard.flight.errors.0").getUnclippedBoundsInRoot()
        assertTrue("the first error must start offscreen: $before / $viewport", before.bottom < viewport.top)

        compose.runOnIdle { current.value = initial.copy(times = initial.times!!.copy(continueFailures = 1)) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(listOf(HapticEvent.Warning, HapticEvent.Warning), haptics.events) }
        val revealed = compose.onNodeWithTag("wizard.flight.errors.0").getUnclippedBoundsInRoot()
        assertTrue(
            "the entire error group must be revealed: $revealed / $viewport",
            revealed.top >= viewport.top - .5.dp && revealed.bottom <= minOf(viewport.bottom, footer.top) + .5.dp,
        )
        compose.runOnIdle { current.value = current.value.copy(retryEnabled = false) }
        compose.runOnIdle { assertEquals(listOf(HapticEvent.Warning, HapticEvent.Warning), haptics.events) }
    }
}
