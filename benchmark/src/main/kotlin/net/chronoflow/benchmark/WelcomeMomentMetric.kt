package net.chronoflow.benchmark

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.TraceMetric
import androidx.benchmark.traceprocessor.TraceProcessor

@OptIn(ExperimentalMetricApi::class)
class WelcomeMomentMetric : TraceMetric() {
    override fun getMeasurements(
        captureInfo: Metric.CaptureInfo,
        traceSession: TraceProcessor.Session,
    ): List<Metric.Measurement> {
        val pkg = captureInfo.targetPackageName.replace("'", "''")
        val ends =
            traceSession
                .query(
                    """
                    SELECT slice.name AS name, slice.ts + slice.dur AS end_ns
                    FROM slice JOIN process_track ON slice.track_id = process_track.id
                    JOIN process USING(upid)
                    WHERE process.name = '$pkg' AND slice.name IN ('first-commit', 'exit', 'intro')
                        AND slice.dur >= 0
                    ORDER BY slice.ts
                    """.trimIndent(),
                ).toList()
                .groupBy { it.string("name") }
                .mapValues { (_, slices) -> slices.first().long("end_ns") }
        val firstCommit = ends["first-commit"] ?: return emptyList()
        return listOf("welcomeInteractiveMs" to "exit", "introLastItemMs" to "intro").mapNotNull { (metric, slice) ->
            ends[slice]?.let { Metric.Measurement(metric, (it - firstCommit) / NS_PER_MS) }
        }
    }

    companion object {
        private const val NS_PER_MS = 1_000_000.0
    }
}
