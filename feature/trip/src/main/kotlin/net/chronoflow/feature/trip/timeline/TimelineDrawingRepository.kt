package net.chronoflow.feature.trip.timeline

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import net.chronoflow.core.plan.DayLayout
import net.chronoflow.core.plan.PreparedDay
import net.chronoflow.core.plan.drawing.DayDrawing
import net.chronoflow.core.plan.drawing.DayDrawingBuilder
import net.chronoflow.core.plan.drawing.DayDrawingCache
import net.chronoflow.core.plan.drawing.DayDrawingKey
import net.chronoflow.core.plan.drawing.TextMetrics
import net.chronoflow.core.plan.generated.TimelinePalette

data class DrawingRequest(
    val key: DayDrawingKey,
    val day: PreparedDay,
    val layout: DayLayout,
)

/**
 * Builds day drawings on [buildDispatcher] (`Dispatchers.Default.limitedParallelism(2)`, created once by the owner) and
 * hands them out through the 1c cache. One build per (key, day) runs at a time: callers join it. A caller's
 * cancellation does not cancel a shared build, which still lands in the cache.
 */
@Stable
class TimelineDrawingRepository(
    private val cache: DayDrawingCache,
    private val palette: (dark: Boolean) -> TimelinePalette,
    /** A new [TextMetrics] per build job (a `Paint` is not thread-safe); production first awaits the typeface load. */
    private val metrics: suspend () -> TextMetrics,
    private val buildDispatcher: CoroutineDispatcher,
    scope: CoroutineScope,
    private val tracer: TimelineTracer = TimelineTracer.Android,
) {
    private class Entry(
        val day: PreparedDay,
        val build: Deferred<DayDrawing>,
    )

    // A failing build must not cancel the owner's scope; the awaiting caller sees the exception.
    private val buildScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
    private val inFlight = HashMap<DayDrawingKey, Entry>()

    /** Never builds. */
    fun peek(
        key: DayDrawingKey,
        day: PreparedDay,
    ): DayDrawing? = cache.peek(key, day)

    suspend fun drawing(
        key: DayDrawingKey,
        day: PreparedDay,
        layout: DayLayout,
    ): DayDrawing {
        cache.get(key, day)?.let { return it }
        return start(key, day, layout).await()
    }

    /** Starts builds without awaiting them; cache hits and builds already running are skipped. */
    fun prefetch(requests: List<DrawingRequest>) {
        for (request in requests) {
            if (cache.peek(request.key, request.day) == null) start(request.key, request.day, request.layout)
        }
    }

    private fun start(
        key: DayDrawingKey,
        day: PreparedDay,
        layout: DayLayout,
    ): Deferred<DayDrawing> {
        val started: Deferred<DayDrawing>
        synchronized(inFlight) {
            val running = inFlight[key]
            if (running != null && (running.day === day || running.day == day)) return running.build
            started = buildScope.async(buildDispatcher, start = CoroutineStart.LAZY) { build(key, day, layout) }
            inFlight[key] = Entry(day, started)
            started.invokeOnCompletion {
                synchronized(inFlight) { if (inFlight[key]?.build === started) inFlight.remove(key) }
            }
        }
        started.start()
        return started
    }

    private suspend fun build(
        key: DayDrawingKey,
        day: PreparedDay,
        layout: DayLayout,
    ): DayDrawing {
        val textMetrics = metrics()
        return cache.getOrBuild(key, day) {
            tracer.section(TimelineTraceNames.BUILD) {
                DayDrawingBuilder.build(day, layout, key, palette(key.dark), textMetrics)
            }
        }
    }
}
