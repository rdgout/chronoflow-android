package net.chronoflow.feature.trip.timeline.host

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.plan.CopyCatalogProvider
import net.chronoflow.core.plan.CopyFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.DayLayout
import net.chronoflow.core.plan.PlanClock
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.plan.PlanPreparer
import net.chronoflow.core.plan.PreparedPlan
import net.chronoflow.core.plan.drawing.DayDrawingCache
import net.chronoflow.core.plan.drawing.DayDrawingKey
import net.chronoflow.core.plan.drawing.TextMetrics
import net.chronoflow.core.plan.generated.TimelinePalette
import net.chronoflow.core.plan.trippage.openingDay
import net.chronoflow.core.plan.trippage.projectDayClocks
import net.chronoflow.feature.trip.timeline.DayClocks
import net.chronoflow.feature.trip.timeline.DrawingRequest
import net.chronoflow.feature.trip.timeline.TimelineDrawingRepository
import net.chronoflow.feature.trip.timeline.TimelineTraceNames
import net.chronoflow.feature.trip.timeline.TimelineTracer
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

@Immutable
class TimelineHostModel(
    val detail: TripDetail,
    val prepared: PreparedPlan,
    val layouts: Map<String, DayLayout>,
    val index: TimelineListIndex,
    val openingDayId: String,
    val timeFormat: TimeFormat,
)

sealed interface TimelineHostState {
    data object Preparing : TimelineHostState

    @Immutable
    class Ready(
        val model: TimelineHostModel,
    ) : TimelineHostState
}

/**
 * Host state of the 1d page: prepares the plan off the main thread (`mapLatest` drops stale work, iOS revision guard),
 * owns the day clocks and the drawing repository. Phase 6b replaces the source and keeps the rest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimelineHostViewModel(
    source: TimelineSource,
    private val clock: PlanClock,
    val language: Language,
    private val compute: CoroutineDispatcher,
    buildDispatcher: CoroutineDispatcher,
    metrics: suspend () -> TextMetrics,
    cache: DayDrawingCache = DayDrawingCache(),
    io: CoroutineDispatcher = Dispatchers.IO,
    private val tracer: TimelineTracer = TimelineTracer.Android,
) : ViewModel() {
    private val firstInput = AtomicBoolean(true)
    private val firstMetrics = AtomicBoolean(true)

    init {
        tracer.beginAsync(TimelineTraceNames.FIRST_FRAME, FirstTimelineFrameTrace.COOKIE)
    }

    private val catalogs = CopyCatalogProvider(io)
    private val decoder = PlanDecoder(worker = compute)
    private val preparer = PlanPreparer(decoder, catalogs, worker = compute)
    val firstFrame = FirstTimelineFrameTrace(tracer)
    val clocks = DayClocks()

    val repository =
        TimelineDrawingRepository(
            cache = cache,
            palette = { dark -> if (dark) TimelinePalette.Dark else TimelinePalette.Light },
            metrics = {
                if (firstMetrics.compareAndSet(true, false)) {
                    tracer.beginAsync(TimelineTraceNames.TYPEFACE, TYPEFACE_COOKIE)
                    try {
                        metrics()
                    } finally {
                        tracer.endAsync(TimelineTraceNames.TYPEFACE, TYPEFACE_COOKIE)
                    }
                } else {
                    metrics()
                }
            },
            buildDispatcher = buildDispatcher,
            scope = viewModelScope,
            tracer = tracer,
        )

    private var formatter: CopyLineFormatter? = null
    private var openingNow: Instant? = null

    val state: StateFlow<TimelineHostState> =
        source
            .inputs()
            .mapLatest { input -> TimelineHostState.Ready(prepare(input)) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, TimelineHostState.Preparing)

    private val _anchorDayId = MutableStateFlow<String?>(null)
    val anchorDayId: StateFlow<String?> = _anchorDayId.asStateFlow()

    /** The first input only: an async section, because the pipeline suspends and changes threads. */
    private suspend fun <T> firstOnly(
        first: Boolean,
        name: String,
        cookie: Int,
        block: suspend () -> T,
    ): T {
        if (!first) return block()
        tracer.beginAsync(name, cookie)
        try {
            return block()
        } finally {
            tracer.endAsync(name, cookie)
        }
    }

    private suspend fun prepare(input: TimelineSourceInput): TimelineHostModel {
        val first = firstInput.getAndSet(false)
        val detail =
            firstOnly(first, TimelineTraceNames.DECODE, DECODE_COOKIE) {
                decoder.detail(
                    PlanVersionRecord(
                        tripId = FIXTURE,
                        inputHash = FIXTURE,
                        detail = input.detailBytes,
                        receivedAt = "",
                    ),
                )
            }
        val (layouts, prepared) =
            firstOnly(first, TimelineTraceNames.PREPARE, PREPARE_COOKIE) {
                val layouts = preparer.layouts(detail, input.hidden)
                layouts to preparer.prepare(detail, input.hidden, language, input.timeFormat, input.marks)
            }
        val now = openingNow ?: withContext(compute) { clock.values().first() }.also { openingNow = it }
        val opening =
            openingDay(detail.plan.days, detail.plan.blocks, null, null, null, now)?.id
                ?: prepared.days.first().id
        return TimelineHostModel(
            detail,
            prepared,
            layouts,
            TimelineListIndex(prepared.days.map { it.id }),
            opening,
            input.timeFormat,
        ).also { clocks.retain(layouts.keys) }
    }

    /** Collected while the page is started; projects the clock onto the days again whenever the model changes. */
    suspend fun runClock() {
        combine(state.filterIsInstance<TimelineHostState.Ready>().map { it.model }, clock.values()) { model, now ->
            model to now
        }.collect { (model, now) ->
            val values =
                withContext(compute) {
                    val lines = formatter ?: CopyLineFormatter(catalogs.catalog()).also { formatter = it }
                    projectDayClocks(model.layouts, now, lines, CopyFormat(language, model.timeFormat))
                }
            clocks.apply(values)
        }
    }

    /** iOS `TripPageModel.select(dayID:)`: the anchor changes only when the day changes. */
    fun select(dayId: String) {
        if (_anchorDayId.value != dayId) _anchorDayId.value = dayId
    }

    /** Main thread: starts the builds of the two days on each side of [dayIndex]. */
    fun prefetchAround(
        dayIndex: Int,
        key: DayDrawingKey,
    ) {
        val model = (state.value as? TimelineHostState.Ready)?.model ?: return
        val days = model.prepared.days
        val requests = ArrayList<DrawingRequest>(PREFETCH_SPAN * 2)
        for (i in dayIndex - PREFETCH_SPAN..dayIndex + PREFETCH_SPAN) {
            if (i == dayIndex || i !in days.indices) continue
            val day = days[i]
            requests.add(DrawingRequest(key.copy(dayId = day.id), day, model.layouts.getValue(day.id)))
        }
        repository.prefetch(requests)
    }

    private companion object {
        const val FIXTURE = "fixture"
        const val PREFETCH_SPAN = 2
        const val DECODE_COOKIE = 1
        const val PREPARE_COOKIE = 2
        const val TYPEFACE_COOKIE = 3
    }
}
