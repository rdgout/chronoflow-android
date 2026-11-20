@file:Suppress("ktlint:standard:max-line-length")
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.feature.wizard.shell.WizardExit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripWizardViewModelCloseTest {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    @Test fun requestCloseOnAnEmptyWhereExitsAtOnce() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { }) { f ->
                runCurrent()
                f.model.requestClose()
                runCurrent()
                assertEquals(WizardExit.Trips, f.model.exit.value)
                assertEquals(1, f.deps.recovery.discarded.size)
                assertFalse(f.model.discardDialogVisible.value)
            }
        }

    @Test fun requestCloseWithChangesShowsTheDialog() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    routeDraft()
            }) { f ->
                runCurrent()
                f.model.requestClose()
                runCurrent()
                assertTrue(f.model.discardDialogVisible.value)
                assertNull(f.model.exit.value)
            }
        }

    @Test fun dismissDiscardKeepsEveryValue() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    routeDraft()
            }) { f ->
                runCurrent()
                val before = f.model.wizard!!.draft
                f.model.requestClose()
                f.model.dismissDiscard()
                runCurrent()
                assertFalse(f.model.discardDialogVisible.value)
                assertEquals(before, f.model.wizard!!.draft)
                assertNull(f.model.exit.value)
            }
        }

    @Test fun confirmDiscardExitsOnceAndClosingBlocksRepeats() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    routeDraft()
            }) { f ->
                runCurrent()
                f.model.requestClose()
                f.model.confirmDiscard()
                f.model.confirmDiscard()
                runCurrent()
                f.model.requestClose()
                runCurrent()
                assertEquals(1, f.deps.recovery.discarded.size)
                assertEquals(WizardExit.Trips, f.model.exit.value)
            }
        }

    @Test fun discardFailureStaysOnScreen() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft = routeDraft()
                discardFailure =
                    java.io.IOException()
            }) { f ->
                runCurrent()
                f.model.confirmDiscard()
                runCurrent()
                assertFalse(f.model.closing.value)
                assertNull(f.model.exit.value)
                assertFalse(f.model.wizard!!.closed)
            }
        }

    @Test fun dialogIsNotRestoredByRecovery() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = {
                draft =
                    routeDraft()
            }) { f ->
                runCurrent()
                assertFalse(f.model.discardDialogVisible.value)
            }
        }

    @Test fun loadFailedExitsQuietly() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardViewModelFixture(null, net.chronoflow.core.navigation.WizardMode.Create)
            try {
                assertEquals(WizardExit.Trips, fixture.model.exit.value)
            } finally {
                fixture.clear()
            }
        }
}
