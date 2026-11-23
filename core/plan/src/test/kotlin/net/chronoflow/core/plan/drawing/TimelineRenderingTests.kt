package net.chronoflow.core.plan.drawing

import net.chronoflow.core.model.contracts.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Port of `TimelineRenderingTests.vectorDrawingsStayWithinTraversalBudgetAndInvalidateCorrectly`, one case per width. */
@RunWith(Parameterized::class)
class TimelineRenderingTests(
    private val content: Int,
) {
    @Test fun vectorDrawingsStayWithinTraversalBudgetAndInvalidateCorrectly() {
        val fixture = DrawingFixtures.prepared("large60", Language.En)
        val cache = DayDrawingCache()
        var largest = 0

        fun key(
            day: net.chronoflow.core.plan.PreparedDay,
            dark: Boolean = false,
        ) = DrawingFixtures.key(day, fixture.plan.revision, content + 32, density = 3f, dark = dark)

        for (day in fixture.plan.days) {
            val layout = fixture.layouts.getValue(day.id)
            val drawing = cache.getOrBuild(key(day), day) { DrawingFixtures.build(day, layout, key(day)) }
            assertTrue("${day.id} height", Math.abs(drawing.heightPx - layout.height * 3) <= 1)
            assertEquals(layout.dividers.size, drawing.dividerFrames.size / 4)
            assertTrue("${day.id} is ${drawing.estimatedBytes} B", drawing.estimatedBytes < 512 * 1024)
            assertTrue(cache.totalBytes <= 4L * 1024 * 1024)
            largest = maxOf(largest, drawing.estimatedBytes)
        }
        val last = fixture.plan.days.last()
        val layout = fixture.layouts.getValue(last.id)
        val before = cache.buildCount
        val first = cache.getOrBuild(key(last), last) { DrawingFixtures.build(last, layout, key(last)) }
        val again = cache.getOrBuild(key(last), last) { DrawingFixtures.build(last, layout, key(last)) }
        assertSame(first, again)
        val dark =
            cache.getOrBuild(
                key(last, dark = true),
                last,
            ) { DrawingFixtures.build(last, layout, key(last, true)) }
        assertNotSame(first, dark)
        assertEquals(1, cache.buildCount - before)
        println("DayDrawing metric: largest $largest bytes")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "content {0}")
        fun cases(): List<Int> = listOf(358, 637)
    }
}
