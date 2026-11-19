package net.chronoflow.feature.trip.wide

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TripPageFrameTest(
    private val window: Double,
    private val content: Double,
    private val gutter: Double,
    private val compact: Boolean,
    private val list: Double?,
    private val timeline: Double,
) {
    @Test fun frameUsesTheWindowForPolicyAndContentForColumns() {
        val frame = TripPageFrame.resolve(window, content)
        assertEquals(list != null, frame.layout.isWide)
        assertEquals(gutter, frame.gutterDp, 0.0)
        assertEquals(compact, frame.compact)
        assertEquals(list, frame.listWidthDp)
        assertEquals(timeline, frame.timelineWidthDp, 0.0)
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "window={0},content={1}")
        fun cases(): List<Array<Any?>> = listOf(
            arrayOf(280.0, 280.0, 16.0, true, null, 248.0),
            arrayOf(411.0, 411.0, 16.0, true, null, 379.0),
            arrayOf(599.0, 599.0, 16.0, true, null, 567.0),
            arrayOf(600.0, 600.0, 20.0, true, 280.0, 264.0),
            arrayOf(639.0, 639.0, 20.0, true, 280.0, 303.0),
            arrayOf(640.0, 640.0, 20.0, false, 280.0, 304.0),
            arrayOf(669.0, 669.0, 20.0, false, 280.0, 333.0),
            arrayOf(841.0, 761.0, 20.0, false, 280.0, 425.0),
            arrayOf(899.0, 899.0, 20.0, false, 280.0, 563.0),
            arrayOf(900.0, 900.0, 20.0, false, 320.0, 524.0),
            arrayOf(914.0, 786.0, 20.0, false, 320.0, 410.0),
            arrayOf(280.0, 10.0, 16.0, true, null, 0.0),
            arrayOf(900.0, 10.0, 20.0, false, 320.0, 0.0),
        )
    }
}
