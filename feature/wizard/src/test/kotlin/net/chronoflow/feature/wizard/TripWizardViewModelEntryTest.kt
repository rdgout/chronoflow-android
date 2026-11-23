@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.wizard.generation.TripEditContext
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.feature.wizard.shell.WizardExit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripWizardViewModelEntryTest {
    @get:Rule val main: org.junit.rules.TestWatcher = WizardMainDispatcherRule()

    private fun checkEntry(
        mode: WizardMode,
        recovered: MemberWizardDraft?,
        failure: DataFailure? = null,
        check: (FakeWizardDependencies, TripWizardViewModel) -> Unit,
    ) = runTest(timeout = 60.seconds) {
        val fake = FakeWizardDependencies(this).apply { this.failure = failure }
        val deps =
            object : TripWizardDependencies by fake {
                override suspend fun recover() = recovered
            }
        val fixture = WizardViewModelFixture(deps, mode)
        try {
            runCurrent()
            check(fake, fixture.model)
        } finally {
            withContext(NonCancellable) {
                fixture.clear()
                fake.close()
            }
        }
    }

    private fun editing(id: String) =
        MemberWizardDraft().let {
            it.copy(state = it.state.copy(edit = TripEditContext(id, "source")))
        }

    @Test fun editUsesThePreparedRowWithoutReloading() =
        checkEntry(WizardMode.Edit("t1"), editing("t1")) { fake, vm ->
            assertTrue(fake.tripStarts.isEmpty())
            assertTrue((vm.state.value as TripWizardUiState.Active).isEditing)
        }

    @Test fun editFallsBackToStartTripForAnotherTrip() =
        checkEntry(WizardMode.Edit("t1"), editing("t2")) { fake, _ ->
            assertEquals(listOf("t1" to TripWizardAction.edit), fake.tripStarts)
        }

    @Test fun duplicateRejectsACreateDraft() =
        checkEntry(WizardMode.Duplicate("t1"), MemberWizardDraft()) { fake, _ ->
            assertEquals(listOf("t1" to TripWizardAction.duplicate), fake.tripStarts)
        }

    @Test fun duplicateAcceptsOnlyACopiedDraft() =
        MemberWizardDraft().let { draft ->
            checkEntry(
                WizardMode.Duplicate("t1"),
                draft.copy(state = draft.state.copy(copiedRoute = "AMS → NRT")),
            ) { fake, vm ->
                assertTrue(fake.tripStarts.isEmpty())
                assertEquals(
                    "AMS → NRT",
                    (vm.state.value as TripWizardUiState.Active)
                        .wizard.draft.state.copiedRoute,
                )
            }
        }

    @Test fun editLoadFailureClosesQuietly() =
        checkEntry(WizardMode.Edit("t1"), null, DataFailure.Offline) { fake, vm ->
            assertEquals(listOf("t1" to TripWizardAction.edit), fake.tripStarts)
            assertEquals(WizardExit.Trips, vm.exit.value)
            vm.onEvent(net.chronoflow.feature.wizard.shell.WizardEvent.RequestClose)
            assertEquals(WizardExit.Trips, vm.exit.value)
        }

    @Test fun duplicateLoadFailureClosesQuietly() =
        checkEntry(WizardMode.Duplicate("t1"), null, DataFailure.Offline) { fake, vm ->
            assertEquals(listOf("t1" to TripWizardAction.duplicate), fake.tripStarts)
            assertEquals(WizardExit.Trips, vm.exit.value)
            vm.onEvent(net.chronoflow.feature.wizard.shell.WizardEvent.RequestClose)
            assertEquals(WizardExit.Trips, vm.exit.value)
        }
}
