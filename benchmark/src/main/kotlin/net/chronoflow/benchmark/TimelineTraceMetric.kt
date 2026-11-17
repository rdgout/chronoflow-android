package net.chronoflow.benchmark

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.TraceMetric
import androidx.benchmark.traceprocessor.TraceProcessor
import kotlin.math.ceil

/**
 * Timeline numbers that `TraceSectionMetric` cannot give: the main-thread frame window, builds on the main thread,
 * recompositions, GCs and the P50 and P99 of the main-thread and RenderThread frame durations. With [firstFrame] it
 * reports only the durations of the first-frame breakdown sections, which are async slices on process tracks.
 */
@OptIn(ExperimentalMetricApi::class)
class TimelineTraceMetric(
    private val firstFrame: Boolean = false,
) : TraceMetric() {
    override fun getMeasurements(
        captureInfo: Metric.CaptureInfo,
        traceSession: TraceProcessor.Session,
    ): List<Metric.Measurement> {
        val pkg = captureInfo.targetPackageName
        return if (firstFrame) firstFrameMeasurements(pkg, traceSession) else scrollMeasurements(pkg, traceSession)
    }

    private fun firstFrameMeasurements(
        pkg: String,
        session: TraceProcessor.Session,
    ): List<Metric.Measurement> =
        listOf(
            "timelineDecodeMs" to "cf.timeline.decode",
            "timelinePrepareMs" to "cf.timeline.prepare",
            "timelineTypefaceMs" to "cf.timeline.typeface",
        ).mapNotNull { (metric, section) ->
            val dur =
                session
                    .query(
                        """
                        SELECT slice.dur AS dur FROM slice
                        JOIN process_track ON slice.track_id = process_track.id
                        JOIN process USING(upid)
                        WHERE slice.name = '$section' AND process.name = '$pkg'
                        ORDER BY slice.ts LIMIT 1
                        """.trimIndent(),
                    ).firstOrNull()
                    ?.long("dur")
            dur?.let { Metric.Measurement(metric, it / NS_PER_MS) }
        }

    private fun scrollMeasurements(
        pkg: String,
        session: TraceProcessor.Session,
    ): List<Metric.Measurement> {
        val out = ArrayList<Metric.Measurement>()
        val frames = frameSlices(session, pkg, "($MAIN) AND slice.name LIKE 'Choreographer#doFrame%'")
        if (frames.isNotEmpty()) {
            val start = frames.minOf { it.ts }
            val end = frames.maxOf { it.ts + it.dur }
            out += Metric.Measurement("timelineScrollWindowMs", (end - start) / NS_PER_MS)
            val ms = frames.map { it.dur / NS_PER_MS }.sorted()
            out += Metric.Measurement("timelineMainFrameMsP50", percentile(ms, 50))
            out += Metric.Measurement("timelineMainFrameMsP99", percentile(ms, 99))
        }
        val render = frameSlices(session, pkg, "thread.name = 'RenderThread' AND slice.name LIKE 'DrawFrame%'")
        if (render.isNotEmpty()) {
            out +=
                Metric.Measurement(
                    "timelineRenderThreadMsP99",
                    percentile(render.map { it.dur / NS_PER_MS }.sorted(), 99),
                )
        }
        out +=
            Metric.Measurement(
                "timelineBuildOnMainCount",
                count(session, pkg, "($MAIN) AND slice.name = 'cf.timeline.build'"),
            )
        out += Metric.Measurement("timelineBuildCount", count(session, pkg, "slice.name = 'cf.timeline.build'"))
        out +=
            Metric.Measurement(
                "timelineRecomposeCount",
                count(session, pkg, "($MAIN) AND slice.name = 'Recomposer:recompose'"),
            )
        out +=
            Metric.Measurement(
                "timelineGcCount",
                count(session, pkg, "thread.name = 'HeapTaskDaemon' AND slice.name LIKE '%GC%'"),
            )
        return out
    }

    private class Frame(
        val ts: Long,
        val dur: Long,
    )

    private fun frameSlices(
        session: TraceProcessor.Session,
        pkg: String,
        where: String,
    ): List<Frame> =
        session
            .query(
                """
                SELECT slice.ts AS ts, slice.dur AS dur FROM slice
                JOIN thread_track ON slice.track_id = thread_track.id
                JOIN thread USING(utid)
                JOIN process USING(upid)
                WHERE process.name = '$pkg' AND $where
                """.trimIndent(),
            ).map { Frame(it.long("ts"), it.long("dur")) }
            .toList()

    private fun count(
        session: TraceProcessor.Session,
        pkg: String,
        where: String,
    ): Double =
        session
            .query(
                """
                SELECT COUNT(*) AS n FROM slice
                JOIN thread_track ON slice.track_id = thread_track.id
                JOIN thread USING(utid)
                JOIN process USING(upid)
                WHERE process.name = '$pkg' AND $where
                """.trimIndent(),
            ).first()
            .long("n")
            .toDouble()

    /** Nearest rank over an ascending list. */
    private fun percentile(
        sorted: List<Double>,
        p: Int,
    ): Double = sorted[(ceil(p / 100.0 * sorted.size).toInt() - 1).coerceIn(0, sorted.size - 1)]

    private companion object {
        const val NS_PER_MS = 1_000_000.0
        const val MAIN = "thread.is_main_thread = 1 OR thread.tid = process.pid"
    }
}
