package net.chronoflow.feature.wizard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import net.chronoflow.core.designsystem.haptics.HapticEvent
import net.chronoflow.core.designsystem.haptics.LocalHaptics
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.RecordingHaptics
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.ready.ReadyRow
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.shell.WizardStepKey
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
class GuestReadyScreenTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private val events = mutableListOf<WizardEvent>()
    private val haptics = RecordingHaptics()

    private fun show() {
        val state = guestBoard("WizardReadyGuest")
        compose.setContent {
            WizardTestHost {
                CompositionLocalProvider(LocalHaptics provides haptics) { WizardScreen(state, events::add) }
            }
        }
    }

    @Test fun guestRowsAndPriceArePresentWithoutReturn() {
        show()
        compose.onNodeWithTag("wizard.ready.questions").assertExists()
        compose.onNodeWithTag("wizard.ready.guest-price").assertExists()
        compose.onNodeWithTag("wizard.ready.return").assertDoesNotExist()
    }

    @Test fun questionsRequestsItsOwnRevisitAndSelectionHaptic() {
        show()
        compose.onNodeWithTag("wizard.ready.questions").performScrollTo().performClick()
        assertEquals(listOf(HapticEvent.Selection), haptics.events)
        assertEquals(WizardEvent.RevisitFromReady(ReadyRow.Questions), events.last())
    }

    @Test fun makePlanUsesMediumHaptic() {
        show()
        compose.onNodeWithTag("wizard.ready.continue").performClick()
        assertEquals(listOf(HapticEvent.Medium), haptics.events)
        assertEquals(WizardEvent.Continue(WizardStepKey.Ready), events.last())
    }
}
