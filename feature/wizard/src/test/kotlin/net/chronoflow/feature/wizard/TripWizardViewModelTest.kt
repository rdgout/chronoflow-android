@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.testing.wizard.CheckingTestClock
import net.chronoflow.core.testing.wizard.GenerationSource
import net.chronoflow.core.testing.wizard.InMemoryWizardDraftStore
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.generationReview
import net.chronoflow.core.wizard.generation.TripGenerationEffects
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.AirportSearchState
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.core.wizard.model.WizardDraftStore
import net.chronoflow.core.wizard.model.WizardForm
import net.chronoflow.core.wizard.model.from
import net.chronoflow.core.wizard.model.inputFingerprint
import net.chronoflow.core.wizard.model.reviewInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds
import net.chronoflow.core.navigation.WizardMode as WizardEntry

class TripWizardViewModelTest {
    @get:Rule val main: TestWatcher = WizardMainDispatcherRule()

    @Test fun createStartsOrRecoversThroughStore() =
        runTest(timeout = 60.seconds) {
            withWizard { f ->
                assertEquals(TripWizardUiState.Loading, f.model.state.value)
                runCurrent()
                assertEquals(1, f.deps.starts)
                assertEquals(emptyList<Pair<String, TripWizardAction>>(), f.deps.tripStarts)
                assertEquals(f.deps.draft, (f.model.state.value as TripWizardUiState.Active).wizard.draft)
                assertNotNull(f.model.wizard)
                assertEquals(1, f.deps.generations.size)
            }
        }

    @RunWith(Parameterized::class)
    class Entries(
        private val action: TripWizardAction,
    ) {
        @get:Rule val main: TestWatcher = WizardMainDispatcherRule()

        @Test fun editAndDuplicateLoadTripInput() =
            runTest(timeout = 60.seconds) {
                val entry =
                    when (action) {
                        TripWizardAction.edit -> WizardEntry.Edit("trip")
                        TripWizardAction.duplicate -> WizardEntry.Duplicate("trip")
                    }
                withWizard(entry = entry) { f ->
                    runCurrent()
                    assertEquals(listOf("trip" to action), f.deps.tripStarts)
                    assertEquals(0, f.deps.starts)
                    assertTrue(f.model.state.value is TripWizardUiState.Active)
                }
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun cases() = listOf(TripWizardAction.edit, TripWizardAction.duplicate)
        }
    }

    @Test fun editLoadFailureIsExposed() =
        runTest(timeout = 60.seconds) {
            withWizard(entry = WizardEntry.Edit("trip"), configure = { failure = DataFailure.Offline }) { f ->
                runCurrent()
                assertEquals(TripWizardUiState.LoadFailed(DataFailure.Offline), f.model.state.value)
                assertNull(f.model.wizard)
                assertTrue(f.deps.generations.isEmpty())
            }
        }

    @Test fun recoveredCalculatingResumesSaveOnce() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = checkedDraft(MemberWizardStep.calculating) }) { f ->
                awaitWizard { f.model.state.value is TripWizardUiState.Closed }
                assertEquals(TripWizardUiState.Closed("created"), f.model.state.value)
                assertEquals(1, f.deps.source.count("save"))
                runCurrent()
                assertEquals(1, f.deps.source.count("save"))
                assertEquals(1, f.deps.recovery.saves.size)
            }
        }

    @Test fun recoveredCheckingRunsCheckOnce() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { draft = checkedDraft(MemberWizardStep.checking) }) { f ->
                awaitWizard { f.deps.source.count("review") == 1 && f.deps.checking.waiterCount >= 2 }
                f.deps.checking.advance(1200)
                awaitWizard {
                    f.model.wizard!!
                        .checking.ticks == 3 && 1500L in f.deps.checking.deadlines
                }
                f.deps.checking.advance(1500)
                awaitWizard {
                    f.model.wizard!!
                        .draft.state.step != MemberWizardStep.checking
                }
                assertEquals(1, f.deps.source.count("review"))
                assertFalse(f.model.state.value is TripWizardUiState.LoadFailed)
            }
        }

    @Test fun clearingCancelsGenerationButFinishesPendingWrite() =
        runTest(timeout = 60.seconds) {
            val gate = CompletableDeferred<Unit>()
            val entered = CompletableDeferred<Unit>()
            withWizard(configure = {
                recovery.beforePersist = {
                    entered.complete(Unit)
                    gate.await()
                }
            }) { f ->
                try {
                    runCurrent()
                    val wizard = f.model.wizard!!
                    wizard.go(MemberWizardStep.outbound)
                    runCurrent()
                    assertTrue(entered.isCompleted)
                    wizard.go(MemberWizardStep.sleep)
                    val latest = wizard.draft.state.revision
                    f.clear()
                    assertTrue(
                        f.deps.recovery.persisted
                            .isEmpty(),
                    )
                    var cancelled = false
                    try {
                        f.deps.generations
                            .single()
                            .review("later", wizard.reviewInput, null)
                    } catch (_: CancellationException) {
                        cancelled = true
                    }
                    assertTrue(cancelled)
                    gate.complete(Unit)
                    wizard.flush()
                    runCurrent()
                    assertFalse(
                        f.deps.writerScope.coroutineContext.job.children
                            .any { it.isActive },
                    )
                    assertEquals(
                        latest,
                        f.deps.recovery.last!!
                            .state.revision,
                    )
                    assertTrue(
                        f.deps.recovery.discarded
                            .isEmpty(),
                    )
                } finally {
                    gate.complete(Unit)
                }
            }
        }

    @Test fun searchQueryReachesSharedService() =
        runTest(timeout = 60.seconds) {
            withWizard { f ->
                runCurrent()
                f.model.search.setQuery(" AMS ")
                f.deps.checking.advance(350)
                assertEquals(listOf("AMS"), f.deps.queries)
                assertTrue(f.model.search.state.value is AirportSearchState.Empty)
            }
        }

    @Test fun failedDiscardStaysActiveAndRethrows() =
        runTest(timeout = 60.seconds) {
            withWizard(configure = { discardFailure = IOException() }) { f ->
                runCurrent()
                var failed = false
                try {
                    f.model.discard()
                } catch (_: IOException) {
                    failed = true
                }
                runCurrent()
                assertTrue(failed)
                assertTrue(f.model.state.value is TripWizardUiState.Active)
                assertFalse(f.model.wizard!!.closed)
                f.deps.discardFailure = null
                f.model.discard()
                runCurrent()
                assertEquals(TripWizardUiState.Closed(null), f.model.state.value)
            }
        }

    @Test fun missingSessionIsUnauthorized() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardViewModelFixture(null, WizardEntry.Create)
            try {
                assertEquals(
                    TripWizardUiState.LoadFailed(
                        DataFailure.Unauthorized(
                            401,
                        ),
                    ),
                    fixture.model.state.value,
                )
            } finally {
                fixture.clear()
            }
        }
}
