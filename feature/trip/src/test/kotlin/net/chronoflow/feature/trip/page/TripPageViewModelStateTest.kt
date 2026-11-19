@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.trip.page

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.store.TripPlanSnapshot
import net.chronoflow.core.plan.CopyCatalogProvider
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.PlanPreparer
import net.chronoflow.core.plan.trippage.TripPageState
import net.chronoflow.core.sync.SyncEvent
import net.chronoflow.core.sync.SyncPathMonitoring
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.SyncState
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.core.testing.FakeTextMetrics
import net.chronoflow.feature.trip.state.FakePath
import net.chronoflow.feature.trip.timeline.TimelineTracer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class TripPageViewModelStateTest {
    private val stores = mutableListOf<ViewModelStore>()

    @After fun tearDown() {
        stores.forEach { it.clear() }
        Dispatchers.resetMain()
    }

    private class Rig(
        val vm: TripPageViewModel,
        val source: FakeTripPageSource,
        val saved: SavedStateHandle,
    )

    private fun TestScope.rig(
        source: FakeTripPageSource = FakeTripPageSource.fixture("S3"),
        awaiting: Boolean = false,
        events: MutableSharedFlow<SyncEvent> = MutableSharedFlow(replay = 1),
        path: SyncPathMonitoring? = null,
        timeline: Boolean = true,
        startFetch: Boolean = true,
        fetch: suspend (String) -> Unit = {},
    ): Rig {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val decoder = PlanDecoder(dispatcher)
        val catalogs = CopyCatalogProvider(dispatcher)
        val preparer = PlanPreparer(decoder, catalogs, dispatcher)
        val saved = SavedStateHandle()
        val store = ViewModelStore().also { stores += it }
        val vm =
            ViewModelProvider(
                store,
                viewModelFactory {
                    initializer {
                        TripPageViewModel(
                            TripPageArgs("fixture-S3", awaitingSync = awaiting),
                            source,
                            FakePlanClock(Instant.EPOCH),
                            DefaultTripPlanPipeline(decoder, preparer, catalogs, dispatcher, TimelineTracer.None) {},
                            saved,
                            Language.En,
                            dispatcher,
                            dispatcher,
                            { FakeTextMetrics() },
                            formatter = { CopyLineFormatter(catalogs.catalog()) },
                            tracer = TimelineTracer.None,
                            fetchDetail = fetch,
                            makePath = { path },
                            syncEvents = events,
                            epoch = 4,
                            initiallySyncCompleted = false,
                            initialTimeline = timeline,
                        )
                    }
                },
            )[TripPageViewModel::class.java]
        if (startFetch) backgroundScope.launch { vm.runFetch() }
        return Rig(vm, source, saved)
    }

    private fun remote(): FakeTripPageSource =
        FakeTripPageSource.fixture("S3").also {
            it.snapshot.value =
                it.snapshot.value.copy(
                    trip =
                        it.snapshot.value.trip!!
                            .copy(keepOffline = false),
                    plan = null,
                )
        }

    private fun finished(
        epoch: Long = 4,
        state: SyncState = SyncState.Idle,
    ) = SyncEvent.Finished(
        net.chronoflow.core.model.AccountID
            .of("00000000-0000-4000-8000-000000000001"),
        epoch,
        SyncResult(state, 0),
    )

    @Test fun remoteTripFetchesOnceAndBecomesReady() =
        runTest(timeout = 60.seconds) {
            val source = remote()
            val stored =
                FakeTripPageSource
                    .fixture("S3")
                    .snapshot.value.plan
            var requests = 0
            val rig = rig(source, fetch = { requests++ })
            runCurrent()
            assertEquals(TripPageState.RemoteLoading, rig.vm.ui.value.state)
            assertEquals(1, requests)
            source.snapshot.value = source.snapshot.value.copy(plan = stored)
            runCurrent()
            assertEquals(TripPageState.Ready, rig.vm.ui.value.state)
        }

    @Test fun offlinePathShowsRemoteOfflineWithoutRequest() =
        runTest(timeout = 60.seconds) {
            val path = FakePath()
            path.values.trySend(false)
            var requests = 0
            val rig = rig(remote(), path = path, fetch = { requests++ })
            runCurrent()
            assertEquals(TripPageState.RemoteOffline, rig.vm.ui.value.state)
            assertEquals(0, requests)
            path.values.send(true)
            runCurrent()
            assertEquals(1, requests)
        }

    @Test fun notFoundAndServerFromFetch() =
        runTest(timeout = 60.seconds) {
            var status = 404
            val gate = CompletableDeferred<Unit>()
            val rig = rig(remote(), fetch = { if (status == 0) gate.await() else throw DataFailure.Server(status) })
            runCurrent()
            assertEquals(TripPageState.NotFound, rig.vm.ui.value.state)
            status = 500
            rig.vm.retry()
            runCurrent()
            assertEquals(TripPageState.Server, rig.vm.ui.value.state)
            status = 0
            rig.vm.retry()
            runCurrent()
            assertEquals(TripPageState.RemoteLoading, rig.vm.ui.value.state)
        }

    @Test fun finishedSyncOfOtherEpochIsIgnored() =
        runTest(timeout = 60.seconds) {
            val events = MutableSharedFlow<SyncEvent>(replay = 1)
            val rig = rig(FakeTripPageSource(TripPlanSnapshot(plan = null)), events = events)
            runCurrent()
            events.emit(finished(epoch = 3))
            runCurrent()
            assertEquals(TripPageState.Loading, rig.vm.ui.value.state)
            events.emit(finished(state = SyncState.Offline))
            runCurrent()
            assertEquals(TripPageState.Loading, rig.vm.ui.value.state)
            events.emit(finished())
            runCurrent()
            assertEquals(TripPageState.NotFound, rig.vm.ui.value.state)
        }

    @Test fun replayedFinishedEventCounts() =
        runTest(timeout = 60.seconds) {
            val events = MutableSharedFlow<SyncEvent>(replay = 1)
            events.emit(finished())
            val rig = rig(FakeTripPageSource(TripPlanSnapshot(plan = null)), events = events)
            runCurrent()
            assertEquals(TripPageState.NotFound, rig.vm.ui.value.state)
        }

    @Test fun awaitingSyncClearsWhenRecordAppears() =
        runTest(timeout = 60.seconds) {
            val events = MutableSharedFlow<SyncEvent>(replay = 1)
            events.emit(finished())
            val source = FakeTripPageSource(TripPlanSnapshot(plan = null))
            val rig = rig(source, awaiting = true, events = events)
            runCurrent()
            assertEquals(TripPageState.Loading, rig.vm.ui.value.state)
            source.snapshot.value = FakeTripPageSource.fixture("S3").snapshot.value
            runCurrent()
            assertEquals(TripPageState.Ready, rig.vm.ui.value.state)
            source.snapshot.value = TripPlanSnapshot(plan = null)
            runCurrent()
            assertEquals(TripPageState.NotFound, rig.vm.ui.value.state)
        }

    @Test fun viewModeDefaultsToTimelineAndToggles() =
        runTest(timeout = 60.seconds) {
            val rig = rig()
            assertTrue(rig.vm.timeline.value)
            rig.vm.toggleView()
            assertFalse(rig.vm.timeline.value)
            assertTrue(rig().vm.timeline.value)
            assertTrue(rig.saved.keys().isEmpty())
        }

    @Test fun listSeedStartsInListMode() =
        runTest(timeout = 60.seconds) {
            assertFalse(rig(timeline = false).vm.timeline.value)
        }

    @Test fun retryWhileOfflineDoesNotRequestAndAnOfflineFailureCanRetry() =
        runTest(timeout = 60.seconds) {
            val path = FakePath()
            path.values.trySend(false)
            var requests = 0
            val rig =
                rig(remote(), path = path, fetch = {
                    requests++
                    if (requests == 1) throw DataFailure.Offline
                })
            runCurrent()
            rig.vm.retry()
            runCurrent()
            assertEquals(0, requests)
            path.values.send(true)
            runCurrent()
            assertEquals(1, requests)
            assertEquals(TripPageState.RemoteOffline, rig.vm.ui.value.state)
            rig.vm.retry()
            runCurrent()
            assertEquals(2, requests)
            assertEquals(TripPageState.RemoteLoading, rig.vm.ui.value.state)
        }

    @Test fun tombstoneWhileReadyBecomesNotFound() =
        runTest(timeout = 60.seconds) {
            val rig = rig()
            runCurrent()
            assertEquals(TripPageState.Ready, rig.vm.ui.value.state)
            rig.source.snapshot.value =
                rig.source.snapshot.value
                    .let { it.copy(trip = it.trip!!.copy(deletedLocally = true)) }
            runCurrent()
            assertEquals(TripPageState.NotFound, rig.vm.ui.value.state)
        }

    @Test fun fetchLifecycleWaitsForThePageToStart() =
        runTest(timeout = 60.seconds) {
            val path = FakePath()
            var requests = 0
            val rig = rig(remote(), path = path, startFetch = false, fetch = { requests++ })
            runCurrent()
            assertEquals(0, requests)
            assertEquals(0, path.collectors)
            assertEquals(TripPageState.RemoteLoading, rig.vm.ui.value.state)
        }

    @Test fun returningToThePageRefetchesTheMissingPlanAndRestartsItsPath() =
        runTest(timeout = 60.seconds) {
            val path = FakePath()
            var requests = 0
            val rig =
                rig(remote(), path = path, startFetch = false, fetch = {
                    requests++
                    if (requests == 1) CompletableDeferred<Unit>().await()
                })
            val first = backgroundScope.launch { rig.vm.runFetch() }
            runCurrent()
            assertEquals(1, requests)
            assertEquals(1, path.collectors)
            assertEquals(TripPageState.RemoteLoading, rig.vm.ui.value.state)
            first.cancelAndJoin()
            runCurrent()
            assertTrue(path.stopped)
            assertEquals(0, path.collectors)
            path.values.trySend(false)
            runCurrent()
            assertEquals(1, requests)
            val second = backgroundScope.launch { rig.vm.runFetch() }
            runCurrent()
            assertEquals(TripPageState.RemoteOffline, rig.vm.ui.value.state)
            assertEquals(1, requests)
            assertEquals(1, path.collectors)
            path.values.send(true)
            runCurrent()
            assertEquals(2, requests)
            assertEquals(TripPageState.RemoteLoading, rig.vm.ui.value.state)
            second.cancelAndJoin()
            runCurrent()
            assertTrue(path.stopped)
            assertEquals(0, path.collectors)
        }
}
