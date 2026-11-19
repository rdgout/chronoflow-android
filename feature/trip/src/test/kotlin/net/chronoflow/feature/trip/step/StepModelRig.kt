@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.trip.step

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.navigation.TripSheet
import net.chronoflow.core.plan.CopyCatalogProvider
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.PlanPreparer
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.core.testing.FakeTextMetrics
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.feature.trip.page.DefaultTripPlanPipeline
import net.chronoflow.feature.trip.page.FakeTripPageSource
import net.chronoflow.feature.trip.page.TripPageArgs
import net.chronoflow.feature.trip.page.TripPageViewModel
import net.chronoflow.feature.trip.timeline.TimelineTracer

class StepModelRig(
    scope: TestScope,
    initial: TripSheet? = null,
    hash: String? = null,
    blockId: String? = null,
    val saved: SavedStateHandle = SavedStateHandle(),
    private val beforeWrite: suspend () -> Unit = {},
) {
    val detail = LargeTimelineFixtures.fixture("S1").detail
    val block = detail.plan.blocks.first { it.actionable && it.checkKey != null }
    val source = FakeTripPageSource.fixture("S1")
    val permit = SessionMutationPermit()
    val writes = mutableListOf<CheckOffChange>()
    val errors = mutableListOf<Throwable>()
    var current = true
    private val dispatcher = StandardTestDispatcher(scope.testScheduler)
    val store = ViewModelStore()
    val vm: TripPageViewModel

    init {
        Dispatchers.setMain(dispatcher)
        val decoder = PlanDecoder(dispatcher)
        val catalogs = CopyCatalogProvider(dispatcher)
        val preparer = PlanPreparer(decoder, catalogs, dispatcher)
        vm =
            ViewModelProvider(
                store,
                viewModelFactory {
                    initializer {
                        TripPageViewModel(
                            TripPageArgs("fixture-S1", blockId = blockId),
                            source,
                            FakePlanClock(checkNotNull(planDate(block.startUtc))),
                            DefaultTripPlanPipeline(decoder, preparer, catalogs, dispatcher, TimelineTracer.None) {},
                            saved,
                            Language.En,
                            dispatcher,
                            dispatcher,
                            { FakeTextMetrics() },
                            formatter = { CopyLineFormatter(catalogs.catalog()) },
                            tracer = TimelineTracer.None,
                            initialSheet = initial,
                            notificationInputHash = hash,
                            isCurrent = { current },
                            enqueueCheck = { change ->
                                beforeWrite()
                                permit.whileValid { writes.add(change) }
                                val snapshot = source.snapshot.value
                                source.snapshot.value = snapshot.copy(pendingChecks = snapshot.pendingChecks + change)
                            },
                            reportCheckFailure = errors::add,
                        )
                    }
                },
            )[TripPageViewModel::class.java]
        scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) { vm.stepSheet.collect() }
    }

    fun replace(detail: TripDetail) {
        val value = source.snapshot.value
        val record = checkNotNull(value.plan)
        source.snapshot.value =
            value.copy(
                plan =
                    PlanVersionRecord(
                        record.tripId,
                        detail.plan.meta.inputHash,
                        ApiJson.response.encodeToString(TripDetail.serializer(), detail).encodeToByteArray(),
                        record.receivedAt,
                        detail.detailVersion,
                    ),
            )
    }
}
