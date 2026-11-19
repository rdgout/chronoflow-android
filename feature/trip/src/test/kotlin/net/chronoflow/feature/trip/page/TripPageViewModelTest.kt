@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.trip.page

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.PlanStatus
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.store.CheckMarkRecord
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.plan.CopyCatalogProvider
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.PlanPreparer
import net.chronoflow.core.plan.localDate
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.plan.trippage.PlanPreferences
import net.chronoflow.core.plan.trippage.TripPageAnchor
import net.chronoflow.core.plan.trippage.TripPageState
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.core.testing.FakeTextMetrics
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.core.testing.decode
import net.chronoflow.feature.trip.timeline.TimelineTracer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class TripPageViewModelTest {
    private val stores = mutableListOf<ViewModelStore>()

    @After fun tearDown() {
        stores.forEach { it.clear() }
        Dispatchers.resetMain()
    }

    private class Rig(
        val vm: TripPageViewModel,
        val source: FakeTripPageSource,
        val saved: SavedStateHandle,
        val store: ViewModelStore,
        val decoder: PlanDecoder,
        val preparer: PlanPreparer,
    ) {
        val plan get() = vm.ui.value.plan!!
        val days get() = plan.detail.plan.days

        fun corrupt() {
            replaceBytes("bad".encodeToByteArray())
        }

        fun replace(detail: TripDetail) {
            replaceBytes(ApiJson.response.encodeToString(TripDetail.serializer(), detail).encodeToByteArray())
        }

        private fun replaceBytes(bytes: ByteArray) {
            val snapshot = source.snapshot.value
            val record = snapshot.plan!!
            val next =
                PlanVersionRecord(
                    record.tripId,
                    record.inputHash,
                    bytes,
                    record.receivedAt,
                    record.detailVersion,
                )
            source.snapshot.value = snapshot.copy(plan = next)
        }
    }

    private fun TestScope.rig(
        day: Int? = null,
        saved: SavedStateHandle = SavedStateHandle(),
        now: Instant =
            checkNotNull(
                planDate(
                    LargeTimelineFixtures
                        .fixture("S3")
                        .detail.plan.days[1]
                        .startUtc,
                ),
            ).plusSeconds(3600),
        pipeline: TripPlanPipeline? = null,
        tracer: TimelineTracer = TimelineTracer.None,
    ): Rig {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val source = FakeTripPageSource.fixture("S3")
        val decoder = PlanDecoder(dispatcher)
        val catalogs = CopyCatalogProvider(dispatcher)
        val preparer = PlanPreparer(decoder, catalogs, dispatcher)
        val store = ViewModelStore().also { stores += it }
        val days =
            LargeTimelineFixtures
                .fixture("S3")
                .detail.plan.days
        val vm =
            ViewModelProvider(
                store,
                viewModelFactory {
                    initializer {
                        TripPageViewModel(
                            TripPageArgs("fixture-S3", day?.let { days[it].id }),
                            source,
                            FakePlanClock(now),
                            pipeline ?: DefaultTripPlanPipeline(decoder, preparer, TimelineTracer.None) {},
                            saved,
                            Language.En,
                            dispatcher,
                            dispatcher,
                            { FakeTextMetrics() },
                            formatter = { CopyLineFormatter(catalogs.catalog()) },
                            tracer = tracer,
                        )
                    }
                },
            )[TripPageViewModel::class.java]
        return Rig(vm, source, saved, store, decoder, preparer)
    }

    @Test fun staleResultIsDroppedByTheRevisionGuard() =
        runTest(timeout = 60.seconds) {
            val first = CompletableDeferred<PipelineOutput>()
            val second = CompletableDeferred<PipelineOutput>()
            var builds = 0
            val pipeline = TripPlanPipeline { _, _ -> if (++builds == 1) first.await() else second.await() }
            val rig = rig(pipeline = pipeline)
            val applied = mutableListOf<TripPageUi>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { rig.vm.ui.toList(applied) }
            runCurrent()
            rig.source.settings.value = PlanPreferences(TimeFormat.Value12)
            runCurrent()
            val titleB = TripTitleParts("B", "B", "B", "B")
            second.complete(PipelineOutput(rig.source.snapshot.value.trip, titleB, false, null))
            runCurrent()
            first.complete(PipelineOutput(null, TripTitleParts("A", "A", "A", "A"), false, null))
            runCurrent()
            assertEquals(2, builds)
            assertEquals(titleB, rig.vm.ui.value.title)
            assertEquals(listOf(titleB), applied.mapNotNull { it.title })
        }

    @Test fun decodeFailureKeepsTheLastPlan() =
        runTest(timeout = 60.seconds) {
            val rig = rig()
            runCurrent()
            val first = rig.plan
            rig.corrupt()
            runCurrent()
            assertSame(first, rig.plan)
        }

    @Test fun leaseIsHeldWhileAliveAndReleasedOnClear() =
        runTest(timeout = 60.seconds) {
            val rig = rig()
            assertEquals(1, rig.source.holds)
            assertEquals(0, rig.source.releases)
            rig.store.clear()
            assertEquals(1, rig.source.releases)
            rig.store.clear()
            assertEquals(1, rig.source.releases)
        }

    @Test fun openingAnchorFollowsOpeningDay() =
        runTest(timeout = 60.seconds) {
            val rig = rig()
            runCurrent()
            assertEquals(TripPageAnchor(rig.days[1].id, rig.days[1].labelDate), rig.vm.anchor.value)
            val commands = mutableListOf<ScrollCommand>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { rig.vm.scrollCommands.toList(commands) }
            assertTrue(commands.isEmpty())
        }

    @Test fun requestedDayWins() =
        runTest(timeout = 60.seconds) {
            val rig = rig(day = 3)
            runCurrent()
            assertEquals(rig.days[3].id, rig.vm.anchor.value.dayID)
        }

    @Test fun savedAnchorRestores() =
        runTest(timeout = 60.seconds) {
            val day =
                LargeTimelineFixtures
                    .fixture("S3")
                    .detail.plan.days[2]
            val saved =
                SavedStateHandle(
                    mapOf(
                        "trip.anchor.day" to day.id,
                        "trip.anchor.labelDate" to day.labelDate,
                        "trip.anchor.offset" to 180.0,
                    ),
                )
            val rig = rig(saved = saved)
            runCurrent()
            assertEquals(TripPageAnchor(day.id, day.labelDate, 180.0), rig.vm.openingScroll())
        }

    @Test fun selectChangesAnchorOnlyOnDayChange() =
        runTest(timeout = 60.seconds) {
            val rig = rig()
            runCurrent()
            val changes = mutableListOf<TripPageAnchor>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { rig.vm.anchor.toList(changes) }
            val day = rig.days[2]
            rig.vm.select(day.id, 10.0)
            rig.vm.select(day.id, 50.0)
            rig.vm.saveAnchor()
            assertEquals(2, changes.size)
            assertEquals(50.0, rig.saved.get<Double>("trip.anchor.offset"))
        }

    @Test fun replacementScrollsOnlyWhenDaysChange() =
        runTest(timeout = 60.seconds) {
            val rig = rig()
            runCurrent()
            val commands = mutableListOf<ScrollCommand>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { rig.vm.scrollCommands.toList(commands) }
            val block =
                rig.plan.detail.plan.blocks
                    .first { it.checkKey != null }
            val key = block.checkKey!!
            val mark =
                CheckMarkRecord("fixture-S3", "fixture-S3", key.type.raw, key.localDate, key.ordinal, "done", "now")
            rig.source.snapshot.value =
                rig.source.snapshot.value
                    .copy(checks = listOf(mark))
            runCurrent()
            assertTrue(commands.isEmpty())
            val old = rig.vm.anchor.value
            rig.vm.select(old.dayID!!, 120.0)
            val detail = rig.plan.detail
            val changed = detail.plan.days.map { if (it.id == old.dayID) it.copy(id = "renamed") else it }
            rig.replace(detail.copy(plan = detail.plan.copy(days = changed)))
            runCurrent()
            assertEquals(listOf(ScrollCommand("renamed", 120.0, false)), commands)
        }

    @Test fun jumpSelectsAndScrolls() =
        runTest(timeout = 60.seconds) {
            val rig = rig()
            runCurrent()
            val commands = mutableListOf<ScrollCommand>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { rig.vm.scrollCommands.toList(commands) }
            val id = rig.days[4].id
            rig.vm.jump(id)
            assertEquals(id, rig.vm.anchor.value.dayID)
            assertEquals(listOf(ScrollCommand(id, 0.0, true)), commands)
        }

    @Test fun stateReadyWhenStored() =
        runTest(timeout = 60.seconds) {
            val rig = rig()
            runCurrent()
            assertEquals(TripPageState.Ready, rig.vm.ui.value.state)
        }

    @Test fun noPlanStatusResolvesNoPlan() =
        runTest(timeout = 60.seconds) {
            val rig = rig()
            runCurrent()
            val detail = rig.plan.detail
            rig.replace(detail.copy(plan = detail.plan.copy(status = PlanStatus.NoPlan)))
            runCurrent()
            assertEquals(TripPageState.NoPlan, rig.vm.ui.value.state)
        }

    @Test fun missingPlanEvicts() =
        runTest(timeout = 60.seconds) {
            val rig = rig()
            runCurrent()
            val decoded = rig.plan.detail
            val before = rig.decoder.decodeCount
            rig.source.snapshot.value =
                rig.source.snapshot.value
                    .copy(plan = null)
            runCurrent()
            assertNull(rig.vm.ui.value.plan)
            // A fresh decode proves both caches were evicted, rather than only clearing UI state.
            rig.decoder.detail(FakeTripPageSource.fixture("S3").snapshot.value)
            assertEquals(before + 1, rig.decoder.decodeCount)
            assertEquals(TripPageState.Loading, rig.vm.ui.value.state)
            assertNotNull(decoded)
        }

    @Test fun tombstonedIsNotFound() =
        runTest(timeout = 60.seconds) {
            val rig = rig()
            runCurrent()
            val snapshot = rig.source.snapshot.value
            rig.source.snapshot.value = snapshot.copy(trip = snapshot.trip!!.copy(deletedLocally = true))
            runCurrent()
            assertEquals(TripPageState.NotFound, rig.vm.ui.value.state)
            assertNull(rig.vm.ui.value.plan)
        }

    @Test fun selectIgnoresUnknownDay() =
        runTest(timeout = 60.seconds) {
            val rig = rig()
            runCurrent()
            val anchor = rig.vm.anchor.value
            rig.vm.select("nope", 10.0)
            rig.vm.saveAnchor()
            assertEquals(anchor, rig.vm.anchor.value)
            assertEquals(0.0, rig.saved.get<Double>("trip.anchor.offset"))
        }

    @Test fun firstFrameTraceBeginsInInit() =
        runTest(timeout = 60.seconds) {
            val events = mutableListOf<String>()
            val tracer =
                object : TimelineTracer {
                    override fun <T> section(
                        name: String,
                        block: () -> T,
                    ) = block()

                    override fun beginAsync(
                        name: String,
                        cookie: Int,
                    ) {
                        events += "$name:$cookie"
                    }

                    override fun endAsync(
                        name: String,
                        cookie: Int,
                    ) = Unit
                }
            val rig = rig(tracer = tracer)
            assertEquals(listOf("cf.timeline.firstFrame:0"), events)
            assertEquals(1, rig.source.holds)
        }

    @Test fun cancelledPreparationCannotPublishAfterClear() =
        runTest(timeout = 60.seconds) {
            val gate = CompletableDeferred<PipelineOutput>()
            val rig = rig(pipeline = TripPlanPipeline { _, _ -> gate.await() })
            runCurrent()
            rig.store.clear()
            gate.complete(PipelineOutput(null, TripTitleParts("late", "late", "late", "late"), false, null))
            runCurrent()
            assertNull(rig.vm.ui.value.title)
            assertEquals(1, rig.source.releases)
        }

    @Test fun languageChangeDoesNotScroll() =
        runTest(timeout = 60.seconds) {
            val rig = rig()
            runCurrent()
            val commands = mutableListOf<ScrollCommand>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { rig.vm.scrollCommands.toList(commands) }
            rig.vm.setLanguage(Language.Nl)
            runCurrent()
            assertEquals(Language.Nl, rig.plan.language)
            assertTrue(commands.isEmpty())
        }

    @Test fun clockStopsProjectingAnEvictedPlan() =
        runTest(timeout = 60.seconds) {
            val rig = rig()
            runCurrent()
            backgroundScope.launch { rig.vm.runClock() }
            runCurrent()
            assertEquals(2, rig.vm.summaryClock.values.size)
            rig.source.snapshot.value =
                rig.source.snapshot.value
                    .copy(plan = null)
            runCurrent()
            assertTrue(
                rig.vm.summaryClock.values
                    .isEmpty(),
            )
        }
}
