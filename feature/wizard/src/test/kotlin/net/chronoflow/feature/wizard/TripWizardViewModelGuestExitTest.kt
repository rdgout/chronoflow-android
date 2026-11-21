@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.wizard.model.MemberWizardDrafts
import net.chronoflow.feature.wizard.shell.WizardExit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripWizardViewModelGuestExitTest {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    @Test fun guestCloseExitsToWelcome() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = MemberWizardDrafts.guest() }) { f ->
                runCurrent()
                f.model.confirmDiscard()
                runCurrent()
                assertEquals(WizardExit.Welcome, f.model.exit.value)
            }
        }

    @Test fun memberCloseStillExitsToTrips() =
        runTest(timeout = 60.seconds) {
            withWizard { f ->
                runCurrent()
                f.model.requestClose()
                runCurrent()
                assertEquals(WizardExit.Trips, f.model.exit.value)
            }
        }

    @Test fun guestCrossOnEmptyWhereClosesAtOnce() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = MemberWizardDrafts.guest() }) { f ->
                runCurrent()
                f.model.requestClose()
                runCurrent()
                assertFalse(f.model.discardDialogVisible.value)
                assertEquals(WizardExit.Welcome, f.model.exit.value)
                assertEquals(1, f.deps.recovery.discarded.size)
            }
        }

    @Test fun exitIsEmittedOnce() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = MemberWizardDrafts.guest() }) { f ->
                runCurrent()
                f.model.requestClose()
                f.model.requestClose()
                runCurrent()
                val exit = f.model.exit.value
                f.model.confirmDiscard()
                runCurrent()
                assertEquals(WizardExit.Welcome, exit)
                assertEquals(exit, f.model.exit.value)
                assertEquals(1, f.deps.recovery.discarded.size)
            }
        }

    @Test fun guestStartFailureExitsToWelcomeWithoutAModel() =
        runTest(timeout = 60.seconds) {
            val fake = FakeWizardDependencies(this).apply { failure = DataFailure.Offline }
            val deps =
                object : TripWizardDependencies by fake {
                    override val guest = true
                }
            val fixture = WizardViewModelFixture(deps, net.chronoflow.core.navigation.WizardMode.Create)
            try {
                runCurrent()
                assertEquals(WizardExit.Welcome, fixture.model.exit.value)
            } finally {
                withContext(NonCancellable) {
                    fixture.clear()
                    fake.close()
                }
            }
        }
}
