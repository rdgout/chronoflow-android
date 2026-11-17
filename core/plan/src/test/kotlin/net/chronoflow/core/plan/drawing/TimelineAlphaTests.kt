package net.chronoflow.core.plan.drawing

import net.chronoflow.core.model.contracts.Lane
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.DayLayout
import net.chronoflow.core.plan.PreparedDay
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.testing.FakeTextMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Port of the cache-free half of iOS `TimelineAlphaTests.compactAndTinyTilesHideAllCopyAndInvalidateTheCache`. */
@RunWith(Parameterized::class)
class TimelineAlphaTests(
    private val window: Int,
    private val dark: Boolean,
) {
    class Pair(
        val day: PreparedDay,
        val layout: DayLayout,
    )

    private fun forced(tiny: Boolean): Pair {
        val fixture = DrawingFixtures.prepared("S3")
        val day = fixture.plan.days.first()
        val layout = fixture.layouts.getValue(day.id)
        val source = layout.placed.getValue(Lane.Sleep).first()
        val placed =
            source.copy(
                top = 0.0,
                height = 120.0,
                tiny = tiny,
                col = 0,
                cols = 1,
                continuesBefore = false,
                continuesAfter = false,
            )
        val block = day.blocks.first { it.id == source.block.id }.copy(status = null)
        return Pair(
            day.copy(blocks = listOf(block), ticks = emptyList(), dividerLabels = emptyList()),
            layout.copy(
                height = 120.0,
                placed =
                    mapOf(
                        Lane.Sleep to listOf(placed),
                        Lane.Light to emptyList(),
                        Lane.Caffeine to emptyList(),
                        Lane.Travel to emptyList(),
                    ),
                markers = emptyList(),
                changes = emptyList(),
                dividers = emptyList(),
                ticks = emptyList(),
            ),
        )
    }

    private fun changed(day: PreparedDay): PreparedDay =
        day.copy(
            blocks =
                day.blocks.map {
                    it.copy(
                        title = "Changed title",
                        interval = "00:00 – 23:59",
                        priority = "Changed priority",
                        reason = "Changed reason",
                    )
                },
        )

    private fun draw(
        pair: Pair,
        day: PreparedDay,
        window: Int,
    ): DayDrawing {
        val key = DrawingFixtures.key(day, "r", window, density = 3f, dark = dark)
        return DrawingFixtures.build(day, pair.layout, key, FakeTextMetrics())
    }

    @Test fun compactAndTinyTilesHideAllCopyAndInvalidateTheCache() {
        val pair = forced(tiny = false)
        val original = draw(pair, pair.day, window)
        val other = draw(pair, changed(pair.day), window)
        assertEquals(
            "window $window dark=$dark",
            window < PlanMetrics.timelineLabelBreakpoint,
            opListEquals(original, other),
        )
    }

    @Test fun tinyTilesHideCopyEvenWhenNotCompact() {
        val pair = forced(tiny = true)
        val key = DrawingFixtures.key(pair.day, "r", 669, density = 3f, dark = dark, compact = false)
        val original = DrawingFixtures.build(pair.day, pair.layout, key)
        val changedDay = changed(pair.day)
        val other = DrawingFixtures.build(changedDay, pair.layout, key.copy(dayId = changedDay.id))
        assertTrue(opListEquals(original, other))
    }

    @Test fun accessibilityLabelNamesTitleAndInterval() {
        val pair = forced(tiny = false)
        val block = pair.day.blocks.first()
        assertTrue(block.accessibilityLabel.contains(block.title))
        assertTrue(block.accessibilityLabel.contains(block.interval))
        assertFalse(pair.day.blocks.isEmpty())
        assertSame(Language.En, DrawingFixtures.key(pair.day, "r", window).language)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "W{0} dark={1}")
        fun cases(): List<Array<Any>> =
            listOf(false, true).flatMap { dark ->
                listOf(320, 390, 393, 430, 639, 640, 669).map { arrayOf<Any>(it, dark) }
            }
    }
}
