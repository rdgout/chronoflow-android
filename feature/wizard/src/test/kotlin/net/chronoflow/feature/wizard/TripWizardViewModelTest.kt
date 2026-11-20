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
import kotlinx.coroutines.yield
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.testing.sync.eventually
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

internal class WizardMainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(StandardTestDispatcher())

    override fun finished(description: Description) = Dispatchers.resetMain()
}

private class FakeWizardDependencies(
    scope: TestScope,
) : TripWizardDependencies {
    val recovery = InMemoryWizardDraftStore()
    var discardFailure: IOException? = null
    override val store =
        object : WizardDraftStore by recovery {
            override suspend fun discard(instance: String) {
                discardFailure?.let { throw it }
                recovery.discard(instance)
            }
        }
    val wall = WizardTestClock()
    override val now = { wall.now }
    val checking = CheckingTestClock(scope.testScheduler)
    override val checkingClock = checking.clock
    override val writerScope =
        CoroutineScope(
            scope.backgroundScope.coroutineContext +
                SupervisorJob(
                    scope.backgroundScope.coroutineContext.job,
                ),
        )
    override val source = GenerationSource()
    val queries = mutableListOf<String>()
    override val airportSearch =
        AirportSearchService(
            search = { query ->
                queries.add(query)
                emptyList()
            },
            now = now,
            sleep = { delay((it * 1000).roundToLong()) },
        )
    var draft = MemberWizardDraft()
    var failure: DataFailure? = null
    var starts = 0
    val tripStarts = mutableListOf<Pair<String, TripWizardAction>>()
    val generations = mutableListOf<TripGenerationService>()

    override suspend fun start(): MemberWizardDraft {
        starts++
        failure?.let { throw it }
        return draft
    }

    override suspend fun startTrip(
        id: String,
        action: TripWizardAction,
    ): MemberWizardDraft {
        tripStarts.add(id to action)
        failure?.let { throw it }
        return draft
    }

    override fun newGeneration() =
        TripGenerationService(
            source,
            TripGenerationEffects({}, {}, {}),
            parentContext = writerScope.coroutineContext + Dispatchers.Default,
        ).also { generations.add(it) }

    fun checkedDraft(step: MemberWizardStep): MemberWizardDraft {
        val review = generationReview()
        val input = generationInput()
        val form =
            WizardForm.from(input).let { initial ->
                initial.copy(
                    identity = review.identity,
                    legs =
                        initial.legs.map {
                            it.copy(departLocal = "2028-10-13T09:15", arriveLocal = "2028-10-13T15:15")
                        },
                )
            }
        val draft =
            MemberWizardDraft(
                form = form,
                state =
                    draft.state.copy(
                        step = step,
                        review = review,
                        reviewedAt = wall.now,
                        sleepNeeded = false,
                    ),
            )
        return draft.copy(state = draft.state.copy(checkedInput = draft.inputFingerprint))
    }

    suspend fun close() =
        withContext(NonCancellable) {
            generations.forEach { it.cancel() }
            writerScope.coroutineContext.job.cancelAndJoin()
        }
}

private class WizardViewModelFixture(
    deps: TripWizardDependencies?,
    entry: WizardEntry,
) {
    val store = ViewModelStore()
    val model =
        ViewModelProvider.create(
            store,
            TripWizardViewModel.factory(
                entry,
            ),
            MutableCreationExtras().apply {
                deps?.let { set(TripWizardDependenciesKey, it) }
            },
        )[TripWizardViewModel::class.java]

    suspend fun clear() {
        store.clear()
        model.viewModelScope.coroutineContext.job
            .cancelAndJoin()
    }
}

private class WizardFixture(
    val deps: FakeWizardDependencies,
    val vm: WizardViewModelFixture,
) {
    val model get() = vm.model

    suspend fun clear() = vm.clear()
}

private suspend fun TestScope.withWizard(
    entry: WizardEntry = WizardEntry.Create,
    configure: FakeWizardDependencies.() -> Unit = {},
    block: suspend TestScope.(WizardFixture) -> Unit,
) {
    val deps = FakeWizardDependencies(this).apply(configure)
    val vm = WizardViewModelFixture(deps, entry)
    try {
        block(WizardFixture(deps, vm))
    } finally {
        withContext(NonCancellable) {
            vm.clear()
            deps.close()
        }
    }
}

private suspend fun awaitWizard(condition: suspend () -> Boolean) {
    assertTrue(
        eventually {
            if (condition()) {
                true
            } else {
                yield()
                false
            }
        },
    )
}
