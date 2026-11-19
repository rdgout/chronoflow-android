package net.chronoflow.feature.trip.timeline

import net.chronoflow.core.plan.drawing.TimelineTraceNames
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceholderReplayTraceTest {
    private val events = mutableListOf<Pair<String, Int>>()
    private val tracer =
        object : TimelineTracer {
            override fun <T> section(
                name: String,
                block: () -> T,
            ): T = block()

            override fun beginAsync(
                name: String,
                cookie: Int,
            ) {
                events += "begin:$name" to cookie
            }

            override fun endAsync(
                name: String,
                cookie: Int,
            ) {
                events += "end:$name" to cookie
            }
        }

    @Test fun cachedPlaceholderMeasuresUntilFirstReplayOnly() {
        val trace = PlaceholderReplayTrace(tracer)
        trace.onPlaceholder()
        trace.onPlaceholder()
        assertEquals(1, events.size)
        trace.onReplay()
        trace.onReplay()
        trace.onPlaceholder()
        assertEquals(
            listOf(
                "begin:${TimelineTraceNames.PLACEHOLDER_TO_REPLAY}",
                "end:${TimelineTraceNames.PLACEHOLDER_TO_REPLAY}",
            ),
            events.map { it.first },
        )
        assertEquals(events[0].second, events[1].second)
    }

    @Test fun prewarmedDrawingHasNoPlaceholderInterval() {
        val trace = PlaceholderReplayTrace(tracer)
        trace.onReplay()
        trace.onPlaceholder()
        assertEquals(0, events.size)
    }

    @Test fun differentDayLifetimesCannotCloseEachOthersIntervals() {
        PlaceholderReplayTrace(tracer).onPlaceholder()
        PlaceholderReplayTrace(tracer).onPlaceholder()
        assertEquals(2, events.map { it.second }.distinct().size)
    }
}
