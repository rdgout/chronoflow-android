package net.chronoflow.feature.trip.timeline

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.chronoflow.core.plan.drawing.DayDrawingCache
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.testing.tiers.PerfBudget
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.Locale

/** large60 at window 411 with the real `TextPaint`: bytes are gated, times are reported (OQ-1c-7). */
@Layout
@PerfBudget
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TextPaintDayDrawingBudgetTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun large60BuildsWithinTheByteBudget() {
        val fixture = TimelineTestPlans.prepared("large60")
        val metrics = TextPaintMetrics(TimelineTestFonts.typefaces(context))
        val cache = DayDrawingCache()
        var total = 0.0
        var worst = 0.0
        var bytesMax = 0
        repeat(2) { pass ->
            var passTotal = 0.0
            for (day in fixture.plan.days) {
                val key = TimelineTestPlans.key(day, fixture.plan.revision, 411)
                val start = System.nanoTime()
                val drawing = TimelineTestPlans.build(day, fixture.layouts.getValue(day.id), key, metrics)
                val elapsed = (System.nanoTime() - start) / 1e6
                passTotal += elapsed
                if (pass == 1) worst = maxOf(worst, elapsed)
                bytesMax = maxOf(bytesMax, drawing.estimatedBytes)
                assertTrue("${day.id} is ${drawing.estimatedBytes} B", drawing.estimatedBytes <= 512 * 1024)
                cache.put(key, day, drawing)
                assertTrue(cache.totalBytes <= 4L * 1024 * 1024 && cache.size <= 8)
            }
            if (pass == 1) total = passTotal
        }
        println(
            String.format(
                Locale.ROOT,
                "DayDrawing metric: robolectric W=411 dayMax=%.2f total=%.1f bytesMax=%d",
                worst,
                total,
                bytesMax,
            ),
        )
    }
}
