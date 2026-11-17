package net.chronoflow.feature.trip.timeline

import android.os.Trace

object TimelineTraceNames {
    const val FIRST_FRAME = "cf.timeline.firstFrame"
    const val BUILD = "cf.timeline.build"
    const val REPLAY = "cf.timeline.replay"
    const val PLACEHOLDER = "cf.timeline.placeholder"
}

/** The seam to `android.os.Trace`, so build and clock logic run in plain JVM tests (the platform class is unmocked there). */
interface TimelineTracer {
    fun <T> section(
        name: String,
        block: () -> T,
    ): T

    fun beginAsync(
        name: String,
        cookie: Int,
    )

    fun endAsync(
        name: String,
        cookie: Int,
    )

    object Android : TimelineTracer {
        override fun <T> section(
            name: String,
            block: () -> T,
        ): T {
            Trace.beginSection(name)
            try {
                return block()
            } finally {
                Trace.endSection()
            }
        }

        override fun beginAsync(
            name: String,
            cookie: Int,
        ) = Trace.beginAsyncSection(name, cookie)

        override fun endAsync(
            name: String,
            cookie: Int,
        ) = Trace.endAsyncSection(name, cookie)
    }

    object None : TimelineTracer {
        override fun <T> section(
            name: String,
            block: () -> T,
        ): T = block()

        override fun beginAsync(
            name: String,
            cookie: Int,
        ) = Unit

        override fun endAsync(
            name: String,
            cookie: Int,
        ) = Unit
    }
}
