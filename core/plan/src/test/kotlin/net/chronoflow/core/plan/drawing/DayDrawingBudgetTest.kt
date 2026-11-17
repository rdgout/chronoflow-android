package net.chronoflow.core.plan.drawing

import net.chronoflow.core.testing.FakeTextMetrics
import net.chronoflow.core.testing.tiers.TimingGate
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Build budget on the host JVM with fake metrics: the largest per-day median of five passes <= 1 ms, the median
 * per-width total <= 3 ms, each drawing <= 512 KiB. The single largest sample is printed, never gated.
 */
@TimingGate
class DayDrawingBudgetTest {
    @Test fun large60At360() = measure(360)

    @Test fun large60At411() = measure(411)

    @Test fun large60At841() = measure(841)

    private fun measure(window: Int) {
        val fixture = DrawingFixtures.prepared("large60")
        val days = fixture.plan.days
        val metrics = FakeTextMetrics()

        fun pass(times: DoubleArray?): Pair<Double, Int> {
            var total = 0.0
            var bytes = 0
            val cache = DayDrawingCache()
            for ((index, day) in days.withIndex()) {
                val key = DrawingFixtures.key(day, fixture.plan.revision, window, density = 2.625f)
                val layout = fixture.layouts.getValue(day.id)
                val start = System.nanoTime()
                val drawing = DrawingFixtures.build(day, layout, key, metrics)
                val elapsed = (System.nanoTime() - start) / 1e6
                total += elapsed
                if (times != null) times[index] = elapsed
                bytes = maxOf(bytes, drawing.estimatedBytes)
                assertTrue("${day.id} is ${drawing.estimatedBytes} B", drawing.estimatedBytes <= 512 * 1024)
                cache.put(key, day, drawing)
                assertTrue(cache.totalBytes <= 4L * 1024 * 1024)
                assertTrue(cache.size <= 8)
            }
            return total to bytes
        }
        repeat(WARMUPS) { pass(null) }
        val perDay = Array(days.size) { DoubleArray(SAMPLES) }
        val totals = DoubleArray(SAMPLES)
        var bytesMax = 0
        for (sample in 0 until SAMPLES) {
            System.gc()
            val times = DoubleArray(days.size)
            val (total, bytes) = pass(times)
            totals[sample] = total
            bytesMax = maxOf(bytesMax, bytes)
            for (i in days.indices) perDay[i][sample] = times[i]
        }
        val medians = perDay.map { median(it) }
        val dayMedianMax = medians.max()
        val dayMax = perDay.maxOf { it.max() }
        val totalMedian = median(totals)
        println(
            String.format(
                Locale.ROOT,
                "DayDrawing metric: W=%d dayMedianMax=%.2f dayMax=%.2f totalMedian=%.1f bytesMax=%d",
                window,
                dayMedianMax,
                dayMax,
                totalMedian,
                bytesMax,
            ),
        )
        assertTrue("day median max $dayMedianMax ms > 1 ms at $window", dayMedianMax <= 1.0)
        assertTrue("total median $totalMedian ms > 3 ms at $window", totalMedian <= 3.0)
    }

    private fun median(values: DoubleArray): Double = values.sorted()[values.size / 2]

    private companion object {
        const val WARMUPS = 3
        const val SAMPLES = 5
    }
}
