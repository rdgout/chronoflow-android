package net.chronoflow.feature.trip.timeline

import androidx.compose.ui.unit.Density
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.plan.drawing.DayDrawingBuilder
import net.chronoflow.core.plan.drawing.DayDrawingCache
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.core.testing.FakeTextMetrics
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.feature.trip.timeline.host.TimelineHostModel
import net.chronoflow.feature.trip.timeline.host.TimelineHostState
import net.chronoflow.feature.trip.timeline.host.TimelineHostViewModel
import net.chronoflow.feature.trip.timeline.host.TimelineSource
import net.chronoflow.feature.trip.timeline.host.TimelineSourceInput
import net.chronoflow.feature.trip.timeline.host.timelineAvailableWidthDp
import net.chronoflow.feature.trip.timeline.host.timelineDrawingKey
import java.time.Instant

/**
 * A host ViewModel on a fixture with every dispatcher unconfined, so it is `Ready` right after construction. The 1e
 * benchmarks' invalidation checks reuse it.
 */
class TimelineTestHost(
    val fixture: String = "large60",
    val language: Language = Language.En,
    clockStart: Instant? = null,
    val cache: DayDrawingCache = DayDrawingCache(maxBytes = 64L * 1024 * 1024, maxEntries = 64),
) {
    private val detail = LargeTimelineFixtures.fixture(fixture).detail
    val bytes: ByteArray = LargeTimelineFixtures.detailBytes(fixture)
    val inputs =
        MutableStateFlow(TimelineSourceInput(bytes, emptyList(), TimeFormat.Value24, emptyList()))
    val clock =
        FakePlanClock(
            clockStart ?: checkNotNull(
                planDate(
                    detail.plan.days
                        .first()
                        .startUtc,
                ),
            ),
        )
    val viewModel =
        TimelineHostViewModel(
            source =
                object : TimelineSource {
                    override fun inputs(): Flow<TimelineSourceInput> = inputs
                },
            clock = clock,
            language = language,
            compute = Dispatchers.Unconfined,
            buildDispatcher = Dispatchers.Unconfined,
            metrics = { FakeTextMetrics() },
            cache = cache,
            io = Dispatchers.Unconfined,
            tracer = TimelineTracer.None,
        )

    val model: TimelineHostModel get() = (viewModel.state.value as TimelineHostState.Ready).model

    fun dayStart(position: Int): Instant =
        checkNotNull(
            planDate(
                model.prepared.days[position]
                    .day.startUtc,
            ),
        )

    /** Puts every day's drawing into the cache under the key the page computes for [density] at a phone window of [windowDp]. */
    fun prewarm(
        density: Density,
        windowDp: Double = 411.0,
    ) {
        val prepared = model.prepared
        val available = timelineAvailableWidthDp(windowDp, 0.0, 0.0)
        for (day in prepared.days) {
            val key =
                timelineDrawingKey(
                    day.id,
                    prepared.revision,
                    available,
                    density,
                    dark = false,
                    compact = windowDp < PlanMetrics.timelineLabelBreakpoint,
                    language = language,
                )
            cache.put(
                key,
                day,
                DayDrawingBuilder.build(
                    day,
                    model.layouts.getValue(day.id),
                    key,
                    TimelineTestPlans.palette(false),
                    FakeTextMetrics(),
                ),
            )
        }
    }
}
