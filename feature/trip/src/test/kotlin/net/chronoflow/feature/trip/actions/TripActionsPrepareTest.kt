@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.trip.actions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.navigation.WizardMode
import net.chronoflow.core.plan.actions.TripAction
import net.chronoflow.core.plan.actions.TripActionsHeader
import net.chronoflow.core.sync.SyncPathMonitoring
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.core.testing.FakeTextMetrics
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.feature.trip.page.FakeTripPageSource
import net.chronoflow.feature.trip.page.PipelineOutput
import net.chronoflow.feature.trip.page.TripPageArgs
import net.chronoflow.feature.trip.page.TripPageData
import net.chronoflow.feature.trip.page.TripPageViewModel
import net.chronoflow.feature.trip.page.TripPlanPipeline
import net.chronoflow.feature.trip.timeline.TimelineTracer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

internal suspend fun TestScope.withActionsModel(
    preparer: TripWizardPreparer?,
    online: Boolean = true,
    canEdit: Boolean = true,
    block: suspend TestScope.(TripPageViewModel) -> Unit,
) {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    val store = ViewModelStore()
    val source = FakeTripPageSource.fixture("S1")
    val card = LargeTimelineFixtures.fixture("S1").card.copy(canEdit = canEdit, canDuplicate = true)
    val path =
        object : SyncPathMonitoring {
            override fun updates() = MutableStateFlow(online)

            override suspend fun stop() = Unit
        }
    val model =
        ViewModelProvider(
            store,
            viewModelFactory {
                initializer {
                    TripPageViewModel(
                        TripPageArgs("t"),
                        source,
                        FakePlanClock(Instant.EPOCH),
                        TripPlanPipeline { input, _ ->
                            PipelineOutput(
                                input.snapshot.trip,
                                null,
                                false,
                                null,
                                TripPageData(card = card, actionsHeader = TripActionsHeader("AMS → NRT", "")),
                            )
                        },
                        SavedStateHandle(),
                        Language.En,
                        dispatcher,
                        dispatcher,
                        { FakeTextMetrics() },
                        formatter = { error("no step copy in an actions test") },
                        tracer = TimelineTracer.None,
                        makePath = { path },
                        preparer = preparer,
                    )
                }
            },
        )[TripPageViewModel::class.java]
    backgroundScope.launch { model.runFetch() }
    try {
        runCurrent()
        block(model)
    } finally {
        withContext(NonCancellable) {
            store.clear()
            model.viewModelScope.coroutineContext.job
                .cancelAndJoin()
            Dispatchers.resetMain()
        }
    }
}

class TripActionsPrepareTest {
    @Test fun busyWhilePreparingAndClearedAfter() =
        runTest(timeout = 60.seconds) {
            val gate = CompletableDeferred<TripWizardPrepareOutcome>()
            withActionsModel(TripWizardPreparer { _, _ -> gate.await() }) { vm ->
                vm.startAction(TripAction.Edit)
                runCurrent()
                assertTrue(
                    vm.tripActions.value!!
                        .availability.busy,
                )
                gate.complete(TripWizardPrepareOutcome.Ready)
                runCurrent()
                assertFalse(
                    vm.tripActions.value!!
                        .availability.busy,
                )
            }
        }

    private fun request(
        action: TripAction,
        mode: WizardMode,
    ) = runTest(timeout = 60.seconds) {
        withActionsModel(
            TripWizardPreparer { id, received ->
                assertEquals("t", id)
                assertEquals(action, received)
                TripWizardPrepareOutcome.Ready
            },
        ) { vm ->
            val requests = mutableListOf<WizardMode>()
            backgroundScope.launch { vm.wizardRequests.toList(requests) }
            vm.startAction(action)
            runCurrent()
            assertEquals(listOf(mode), requests)
        }
    }

    @Test fun readyEmitsOneWizardRequest() = request(TripAction.Edit, WizardMode.Edit("t"))

    @Test fun duplicateEmitsOneWizardRequest() = request(TripAction.Duplicate, WizardMode.Duplicate("t"))

    @Test fun ignoredDoesNothing() =
        runTest(timeout = 60.seconds) {
            withActionsModel(TripWizardPreparer { _, _ -> TripWizardPrepareOutcome.Ignored }) { vm ->
                val requests = mutableListOf<WizardMode>()
                backgroundScope.launch { vm.wizardRequests.toList(requests) }
                vm.startAction(TripAction.Edit)
                runCurrent()
                assertTrue(requests.isEmpty())
                assertNull(vm.actionFailure.value?.failure)
                assertFalse(
                    vm.tripActions.value!!
                        .availability.busy,
                )
            }
        }

    @Test fun nullPreparerIsIgnored() =
        runTest(timeout = 60.seconds) {
            withActionsModel(null) { vm ->
                vm.startAction(TripAction.Edit)
                runCurrent()
                assertNull(vm.actionFailure.value?.failure)
                assertFalse(
                    vm.tripActions.value!!
                        .availability.busy,
                )
            }
        }

    @Test fun secondTapWhileBusyIsIgnored() =
        runTest(timeout = 60.seconds) {
            val gate = CompletableDeferred<TripWizardPrepareOutcome>()
            var calls = 0
            withActionsModel(
                TripWizardPreparer { _, _ ->
                    calls++
                    gate.await()
                },
            ) { vm ->
                vm.startAction(TripAction.Edit)
                vm.startAction(TripAction.Duplicate)
                runCurrent()
                assertEquals(1, calls)
                gate.complete(TripWizardPrepareOutcome.Ready)
            }
        }

    @Test fun offlineNeverPrepares() =
        runTest(timeout = 60.seconds) {
            var calls = 0
            withActionsModel(
                TripWizardPreparer { _, _ ->
                    calls++
                    TripWizardPrepareOutcome.Ready
                },
                online = false,
            ) { vm ->
                vm.startAction(TripAction.Edit)
                runCurrent()
                assertEquals(0, calls)
            }
        }

    @Test fun notAllowedNeverPrepares() =
        runTest(timeout = 60.seconds) {
            var calls = 0
            withActionsModel(
                TripWizardPreparer {
                    _,
                    _,
                    ->
                    calls++
                    TripWizardPrepareOutcome.Ready
                },
                canEdit = false,
            ) { vm ->
                vm.startAction(TripAction.Edit)
                runCurrent()
                assertEquals(0, calls)
            }
        }

    @Test fun openingActionsClearsTheFailure() =
        runTest(timeout = 60.seconds) {
            withActionsModel(TripWizardPreparer { _, _ -> TripWizardPrepareOutcome.Failed(false) }) { vm ->
                vm.startAction(TripAction.Edit)
                runCurrent()
                assertEquals(TripActionFailure.Edit, vm.actionFailure.value?.failure)
                vm.openTripActions()
                assertNull(vm.actionFailure.value?.failure)
            }
        }
}
