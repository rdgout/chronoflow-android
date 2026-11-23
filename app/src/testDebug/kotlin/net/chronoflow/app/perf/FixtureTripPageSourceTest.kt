@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.perf

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.navigation.TripSheet
import net.chronoflow.core.plan.CopyCatalogProvider
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.FixedPlanClock
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.PlanPreparer
import net.chronoflow.core.plan.step.StepActions
import net.chronoflow.core.plan.step.StepBadge
import net.chronoflow.core.plan.step.StepSheetState
import net.chronoflow.core.plan.trippage.TripPageState
import net.chronoflow.core.testing.FakeTextMetrics
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.feature.trip.page.DefaultTripPlanPipeline
import net.chronoflow.feature.trip.page.TripPageArgs
import net.chronoflow.feature.trip.page.TripPageViewModel
import net.chronoflow.feature.trip.timeline.TimelineTracer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class FixtureTripPageSourceTest {
    @After fun resetDispatcher() {
        Dispatchers.resetMain()
    }

    @Test fun profileOpeningBlockProducesTheLiveStepSheet() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val source = FixtureTripPageSource("large60")
            val decoder = PlanDecoder(dispatcher)
            val catalogs = CopyCatalogProvider(dispatcher)
            val preparer = PlanPreparer(decoder, catalogs, dispatcher)
            val store = ViewModelStore()
            try {
                val vm =
                    ViewModelProvider(
                        store,
                        viewModelFactory {
                            initializer {
                                TripPageViewModel(
                                    args = TripPageArgs("fixture-large60"),
                                    source = source,
                                    enqueueCheck = source::enqueueCheck,
                                    pipeline =
                                        DefaultTripPlanPipeline(
                                            decoder,
                                            preparer,
                                            catalogs,
                                            dispatcher,
                                            TimelineTracer.None,
                                        ) {},
                                    savedState = SavedStateHandle(),
                                    clock = FixedPlanClock(Instant.parse("2026-10-13T08:00:00Z")),
                                    language = Language.En,
                                    compute = dispatcher,
                                    buildDispatcher = dispatcher,
                                    metrics = { FakeTextMetrics() },
                                    formatter = { CopyLineFormatter(catalogs.catalog()) },
                                    tracer = TimelineTracer.None,
                                )
                            }
                        },
                    )[TripPageViewModel::class.java]
                val ready = vm.ui.first { it.state == TripPageState.Ready }
                val block =
                    checkNotNull(ready.plan)
                        .detail.plan.blocks
                        .first { it.id == OPENING_BLOCK }
                assertTrue(block.actionable)
                checkNotNull(block.checkKey)
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.stepSheet.collect() }
                val steps = vm.stepSheet.filterIsInstance<StepSheetState.Step>()
                vm.selectBlock(OPENING_BLOCK)
                assertEquals(TripSheet.Step(OPENING_BLOCK), vm.sheet.value)
                steps.first { it.actions == StepActions.Open }
                vm.setCheck(OPENING_BLOCK, CheckStatus.Done)
                assertEquals(StepBadge.Done, steps.first { it.actions == StepActions.Marked }.badge)
                vm.checkBusy.first { !it }
                vm.setCheck(OPENING_BLOCK, null)
                steps.first { it.actions == StepActions.Open }
            } finally {
                store.clear()
            }
        }

    @Test fun profileCheckAndUndoReachTheRealDecoder() =
        runTest(timeout = 60.seconds) {
            val detail = LargeTimelineFixtures.fixture("large60").detail
            val source = FixtureTripPageSource("large60", LargeTimelineFixtures.detailBytes("large60"))
            val block = detail.plan.blocks.first { it.actionable && it.checkKey != null }
            val key = checkNotNull(block.checkKey)
            val change =
                CheckOffChange(
                    "fixture-large60",
                    detail.plan.meta.inputHash,
                    key.type,
                    key.localDate,
                    key.ordinal,
                    CheckStatus.Done,
                )
            val decoder = PlanDecoder(StandardTestDispatcher(testScheduler))
            source.enqueueCheck(change)
            val done = checkNotNull(decoder.detail(source.snapshots("fixture-large60").first()))
            assertEquals(
                CheckStatus.Done,
                done.checkState.marks
                    .first { it.key == key }
                    .status,
            )
            source.enqueueCheck(change.copy(status = null))
            val undone = checkNotNull(decoder.detail(source.snapshots("fixture-large60").first()))
            assertTrue(undone.checkState.marks.none { it.key == key })
        }

    private companion object {
        const val OPENING_BLOCK = "caffeine-no_caffeine-29866935-caffeine-day-0"
    }
}
