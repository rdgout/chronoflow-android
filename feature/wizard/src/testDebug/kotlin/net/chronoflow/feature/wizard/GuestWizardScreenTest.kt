package net.chronoflow.feature.wizard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardScreen
import net.chronoflow.feature.wizard.shell.WizardStepKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class GuestWizardScreenTest {
    private val compose = createComposeRule()
    private val events = mutableListOf<WizardEvent>()
    private val profile = DeviceProfiles.pixel8
    private val appearance =
        TestRule { statement, description ->
            val dark = description.methodName == "testReadyGuestDarkNL"
            ProfileRule(profile, if (dark) AppLanguage.Nl else AppLanguage.En, dark).apply(statement, description)
        }

    @get:Rule val rules: RuleChain = RuleChain.outerRule(appearance).around(compose)

    private fun show(board: String) {
        val dark = board.endsWith("DarkNL")
        val initial = guestBoard(board, if (dark) Locale.forLanguageTag("nl") else Locale.ENGLISH)
        val reopen = initial.discardDialogVisible
        compose.setContent {
            ProfileHost(profile) {
                ChronoFlowTheme(darkTheme = dark, reduceMotion = true) {
                    var state by remember { mutableStateOf(initial) }
                    WizardScreen(state) { event ->
                        events += event
                        state =
                            when (event) {
                                WizardEvent.DismissDiscard -> state.copy(discardDialogVisible = false)
                                WizardEvent.RequestClose -> state.copy(discardDialogVisible = reopen)
                                else -> state
                            }
                    }
                }
            }
        }
    }

    @Test fun testWhereGuest() {
        show("WizardWhereGuest")
        compose.onNodeWithTag("wizard.from").assertExists()
        compose.onNodeWithTag("wizard.where.continue").assertIsEnabled()
    }

    @Test fun testOutboundPastGuest() {
        show("WizardOutboundPastGuest")
        compose.onNodeWithTag("wizard.outbound.past").assertExists()
        compose.onNodeWithTag("wizard.0.departure.date").assertIsEnabled()
    }

    @Test fun testSleepGuest() {
        show("WizardSleep")
        compose.onNodeWithTag("wizard.sleep.habitual_bed").assertExists()
        compose.onNodeWithTag("wizard.sleep.continue").assertIsEnabled()
    }

    @Test fun testCheckingGuest() {
        show("WizardCheckingGuest")
        compose.onNodeWithTag("wizard.checking.line.1").assertExists()
    }

    @Test fun testAgeGuest() {
        show("WizardQAge")
        compose.onNodeWithTag("wizard.choice.question.under_18").assertIsEnabled()
        compose.onNodeWithTag("wizard.question.disclaimer").performScrollTo().assertExists()
    }

    @Test fun testExcludedGuest() {
        show("WizardExcluded")
        compose.onNodeWithTag("wizard.excluded.secondary-close").performClick()
        assertEquals(WizardEvent.CloseNow, events.last())
    }

    @Test fun testDiscardGuest() {
        show("WizardDiscardGuest")
        compose.onNodeWithTag("cf.dialog.confirm").assertExists()
        compose.onNodeWithTag("cf.dialog.dismiss").performClick()
        compose.onNodeWithTag("wizard.outbound.continue").assertExists()
        assertEquals(WizardEvent.DismissDiscard, events.last())
        compose.onNodeWithTag("wizard.close").performClick()
        compose.onNodeWithTag("cf.dialog.confirm").assertExists()
        assertEquals(WizardEvent.RequestClose, events.last())
    }

    @Test fun testEmptyWhereClosesImmediately() {
        show("WizardWhereGuestEmpty")
        compose.onNodeWithTag("wizard.close").performClick()
        assertEquals(WizardEvent.RequestClose, events.last())
        compose.onNodeWithTag("cf.dialog.confirm").assertDoesNotExist()
    }

    @Test fun testFineTuneGuest() {
        show("WizardFineTuneGuest")
        compose.onNodeWithTag("wizard.fine-tune.continue").assertIsEnabled()
        compose.onNodeWithTag("wizard.extra.room.0").assertExists()
        compose.onNodeWithTag("wizard.sleep-override").assertDoesNotExist()
    }

    @Test fun testReadyGuest() {
        show("WizardReadyGuest")
        compose.onNodeWithTag("wizard.ready.sleep").assertExists()
        compose.onNodeWithTag("wizard.ready.questions").assertExists()
        compose.onNodeWithTag("wizard.ready.return").assertDoesNotExist()
        compose.onNodeWithTag("wizard.ready.guest-price").assertExists()
        compose.onNodeWithTag("wizard.ready.continue").performClick()
        assertEquals(WizardEvent.Continue(WizardStepKey.Ready), events.last())
    }

    @Test fun testReadyGuestFree() {
        show("WizardReadyGuestFree")
        compose.onNodeWithTag("wizard.ready.guest-price").assertExists()
        compose.onNodeWithTag("wizard.ready.continue").assertIsEnabled()
    }

    @Test fun testReadyGuestDarkNL() {
        show("WizardReadyGuestDarkNL")
        compose.onNodeWithTag("wizard.ready.questions").assertExists()
        compose.onNodeWithTag("wizard.ready.guest-price").assertExists()
        compose.onNodeWithTag("wizard.ready.continue").assertIsEnabled()
    }
}
