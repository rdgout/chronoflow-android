package net.chronoflow.core.plan

import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.core.testing.tiers.TimingGate
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Port of iOS `TimelinePerformanceTests`: decode, layout and preparation inside the budget on every sample,
 * cold (no cache), after three warm-ups. The limits are the same for every build type.
 */
@TimingGate
class TimelinePreparationBudgetTest {
    private val formatter = CopyLineFormatter(testCatalog)

    @Test fun testS3DecodeLayoutAndPreparation() = benchmark("S3", 40.0)

    @Test fun test30DayDecodeLayoutAndPreparation() = benchmark("large30", 100.0)

    @Test fun test60DayDecodeLayoutAndPreparation() = benchmark("large60", 200.0)

    private fun sample(bytes: ByteArray): Pair<Double, Int> {
        val start = System.nanoTime()
        val detail = ApiJson.response.decodeFromString(TripDetail.serializer(), bytes.decodeToString())
        val layouts = layoutPlan(detail.plan, detail.timeline)
        val prepared = preparePlan(detail, layouts, formatter, testPageCopy, Language.En)
        val sink = prepared.days.sumOf { it.blocks.size }
        return (System.nanoTime() - start) / 1e6 to sink
    }

    private fun benchmark(
        name: String,
        budgetMs: Double,
    ) {
        val bytes = LargeTimelineFixtures.detailBytes(name)
        repeat(WARMUPS) { sample(bytes) }
        val times = ArrayList<Double>(SAMPLES)
        repeat(SAMPLES) {
            System.gc()
            val (elapsed, sink) = sample(bytes)
            assertTrue("$name prepared no blocks", sink > 0)
            times += elapsed
        }
        val mean = times.sum() / times.size
        println(
            "Timeline host: $name mean=${tenths(mean)} max=${tenths(times.max())} " +
                "samples=[${times.joinToString(", ") { tenths(it) }}]",
        )
        val over = times.withIndex().filter { it.value >= budgetMs }
        assertTrue(
            "$name decode + layout + preparation exceeded $budgetMs ms on samples " +
                over.joinToString { "#${it.index + 1}=${tenths(it.value)}" },
            over.isEmpty(),
        )
    }

    private fun tenths(value: Double): String {
        val scaled = Math.round(value * 10)
        return "${scaled / 10}.${scaled % 10}"
    }

    private companion object {
        const val WARMUPS = 3
        const val SAMPLES = 10
    }
}
