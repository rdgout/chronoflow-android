package net.chronoflow.feature.trip.page

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.plan.CopyCatalogProvider
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.PlanPreparer
import net.chronoflow.core.plan.drawing.DayDrawingBuilder
import net.chronoflow.core.plan.drawing.DayDrawingCache
import net.chronoflow.core.plan.drawing.TextMetrics
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.core.testing.FakeTextMetrics
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.feature.trip.timeline.TimelineTestPlans
import net.chronoflow.feature.trip.timeline.TimelineTracer
import net.chronoflow.feature.trip.timeline.host.timelineAvailableWidthDp
import net.chronoflow.feature.trip.timeline.host.timelineDrawingKey
import java.time.Instant

class TripPageTestRig(
    val fixture: String = "large60",
    clockStart: Instant? = null,
    startAtSummary: Boolean = false,
    val language: Language = Language.En,
    initialTimeline: Boolean = true,
    metrics: suspend () -> TextMetrics = { FakeTextMetrics() },
    initialSnapshot: net.chronoflow.core.model.store.TripPlanSnapshot? = null,
    fetchDetail: suspend (String) -> Unit = {},
    makePath: () -> net.chronoflow.core.sync.SyncPathMonitoring? = { null },
    applyChecks: Boolean = false,
    val savedState: SavedStateHandle = SavedStateHandle(),
) {
    val detail = LargeTimelineFixtures.fixture(fixture).detail
    val source = initialSnapshot?.let { FakeTripPageSource(it) } ?: FakeTripPageSource.fixture(fixture)
    val clock = FakePlanClock(clockStart ?: checkNotNull(planDate(detail.plan.days[0].startUtc)))
    val cache = DayDrawingCache(maxBytes = 64L * 1024 * 1024, maxEntries = 64)
    private val decoder = PlanDecoder(Dispatchers.Unconfined)
    private val catalogs = CopyCatalogProvider(Dispatchers.Unconfined)
    private val preparer = PlanPreparer(decoder, catalogs, Dispatchers.Unconfined)
    private val store = ViewModelStore()
    val vm =
        ViewModelProvider(
            store,
            viewModelFactory {
                initializer {
                    TripPageViewModel(
                        TripPageArgs("fixture-$fixture", startAtSummary = startAtSummary),
                        source,
                        clock,
                        DefaultTripPlanPipeline(
                            decoder,
                            preparer,
                            catalogs,
                            Dispatchers.Unconfined,
                            TimelineTracer.None,
                        ) {},
                        savedState,
                        language,
                        Dispatchers.Unconfined,
                        Dispatchers.Unconfined,
                        metrics,
                        cache,
                        { CopyLineFormatter(catalogs.catalog()) },
                        TimelineTracer.None,
                        initialTimeline = initialTimeline,
                        fetchDetail = fetchDetail,
                        makePath = makePath,
                        enqueueCheck = { change ->
                            if (applyChecks) {
                                val value = source.snapshot.value
                                source.snapshot.value = value.copy(pendingChecks = value.pendingChecks + change)
                            }
                        },
                    )
                }
            },
        )[TripPageViewModel::class.java]
    val model get() = checkNotNull(vm.ui.value.plan)
    var listState: LazyListState? = null
        private set
    var screenState: TripPageScreenState? = null
        private set
    private val events: (TripPageEvent) -> Unit = { event ->
        when (event) {
            is TripPageEvent.Jump -> vm.jump(event.dayId)
            is TripPageEvent.Select -> vm.select(event.dayId, event.offsetDp)
            TripPageEvent.ToggleView -> vm.toggleView()
            TripPageEvent.SaveAnchor -> vm.saveAnchor()
            is TripPageEvent.Block -> vm.selectBlock(event.id)
            is TripPageEvent.Check -> vm.checkFromAccessibility(event.id, event.status)
            else -> Unit
        }
    }

    @Composable fun Content(
        pageState: net.chronoflow.core.plan.trippage.TripPageState? = null,
        onEvent: ((TripPageEvent) -> Unit)? = null,
        canToday: Boolean = false,
    ) {
        val base = rememberTripPageScreenState(vm, canToday = canToday)
        val state =
            if (pageState == null) {
                base
            } else {
                TripPageScreenState(
                    base.ui.copy(state = pageState),
                    base.anchor,
                    base.listState.takeIf { pageState == net.chronoflow.core.plan.trippage.TripPageState.Ready },
                    base.clocks,
                    base.summaryClock,
                    base.repository,
                    base.firstFrame,
                    base.commands,
                    base.prefetch,
                    base.refresh,
                    base.refreshProbe,
                    base.canSettings,
                    base.canLegend,
                    base.canActions,
                    base.timeline,
                )
            }
        listState = state.listState
        screenState = state
        TripPageScreen(state, onEvent ?: events)
    }

    fun dayStart(position: Int): Instant =
        checkNotNull(
            planDate(
                model.prepared.days[position]
                    .day.startUtc,
            ),
        )

    fun close() {
        store.clear()
    }

    fun replace(detail: TripDetail) {
        val snapshot = source.snapshot.value
        val record = snapshot.plan!!
        val bytes = ApiJson.response.encodeToString(TripDetail.serializer(), detail).encodeToByteArray()
        source.snapshot.value =
            snapshot.copy(
                plan =
                    PlanVersionRecord(
                        record.tripId,
                        record.inputHash,
                        bytes,
                        record.receivedAt,
                        record.detailVersion,
                    ),
            )
    }

    fun prewarm(
        density: Density,
        windowDp: Double = 411.0,
        dark: Boolean = false,
        safeLeft: Double = 0.0,
        safeRight: Double = 0.0,
    ) {
        val available = timelineAvailableWidthDp(windowDp, safeLeft, safeRight)
        for (day in model.prepared.days) {
            val key =
                timelineDrawingKey(
                    day.id,
                    model.prepared.revision,
                    available,
                    density,
                    dark,
                    windowDp < PlanMetrics.timelineLabelBreakpoint,
                    language,
                )
            cache.put(
                key,
                day,
                DayDrawingBuilder.build(
                    day,
                    model.layouts.getValue(day.id),
                    key,
                    TimelineTestPlans.palette(dark),
                    FakeTextMetrics(),
                ),
            )
        }
    }
}
