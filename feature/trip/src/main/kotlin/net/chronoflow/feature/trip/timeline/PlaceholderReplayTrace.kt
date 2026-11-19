package net.chronoflow.feature.trip.timeline

import net.chronoflow.core.plan.drawing.TimelineTraceNames
import java.util.concurrent.atomic.AtomicInteger

/** A cached layer can keep showing its placeholder without drawing again, so measure until its first replay. */
internal class PlaceholderReplayTrace(
    private val tracer: TimelineTracer = TimelineTracer.Android,
) {
    private val cookie = nextCookie.getAndIncrement()
    private var started = false
    private var replayed = false

    fun onPlaceholder() {
        if (!started && !replayed) {
            started = true
            tracer.beginAsync(TimelineTraceNames.PLACEHOLDER_TO_REPLAY, cookie)
        }
    }

    fun onReplay() {
        if (!replayed) {
            replayed = true
            if (started) tracer.endAsync(TimelineTraceNames.PLACEHOLDER_TO_REPLAY, cookie)
        }
    }

    private companion object {
        val nextCookie = AtomicInteger()
    }
}
