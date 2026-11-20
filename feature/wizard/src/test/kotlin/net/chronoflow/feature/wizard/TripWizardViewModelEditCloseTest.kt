@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.feature.wizard.shell.WizardEvent
import net.chronoflow.feature.wizard.shell.WizardExit
import net.chronoflow.feature.wizard.shell.WizardUiOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripWizardViewModelEditCloseTest {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    @Test fun untouchedEditClosesAtOnce() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = editReadyActive().wizard.draft
                uiOverride = WizardUiOverride(reviewing = false)
            }) { f ->
                runCurrent()
                assertTrue((f.model.state.value as TripWizardUiState.Active).firstScreen)
                f.model.onEvent(WizardEvent.RequestClose)
                runCurrent()
                assertFalse(f.model.discardDialogVisible.value)
                assertEquals(WizardExit.Trips, f.model.exit.value)
                assertEquals(1, f.deps.recovery.discarded.size)
            }
        }

    @Test fun changedEditAsksBeforeClosing() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = editReadyActive("EditReturn").wizard.draft
                uiOverride = WizardUiOverride(reviewing = false)
            }) { f ->
                runCurrent()
                val before = f.model.wizard!!.draft
                f.model.onEvent(WizardEvent.RequestClose)
                runCurrent()
                assertTrue(f.model.discardDialogVisible.value)
                assertNull(f.model.exit.value)
                f.model.onEvent(WizardEvent.DismissDiscard)
                runCurrent()
                assertFalse(f.model.discardDialogVisible.value)
                assertEquals(before, f.model.wizard!!.draft)
            }
        }

    @Test fun confirmEditDiscardDeletesRecoveryAndExitsOnce() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = editReadyActive("EditReturn").wizard.draft
                uiOverride = WizardUiOverride(reviewing = false)
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.RequestClose)
                f.model.onEvent(WizardEvent.ConfirmDiscard)
                f.model.onEvent(WizardEvent.ConfirmDiscard)
                runCurrent()
                assertEquals(WizardExit.Trips, f.model.exit.value)
                assertEquals(1, f.deps.recovery.discarded.size)
            }
        }

    @Test fun noCreditsFooterUsesTheEditDiscardRule() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = editReadyActive("EditReturnNoCredits").wizard.draft
                uiOverride = WizardUiOverride(reviewing = false)
            }) { f ->
                runCurrent()
                f.model.onEvent(WizardEvent.CloseFromFooter)
                runCurrent()
                assertTrue(f.model.discardDialogVisible.value)
                assertNull(f.model.exit.value)
            }
        }
}
