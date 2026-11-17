package net.chronoflow.feature.launch.model

import org.junit.Assert.assertEquals
import org.junit.Test

class StatusLineLayoutTests {
    @Test fun statusTextAndDotsKeepTheirBoardSpacingAndPixelAlignedWidth() {
        val l = StatusLineLayout(390.0, 150.1, 12.0, 3.0, 20.0, 2.0)
        assertEquals(LaunchRect(107.75, 2.5, 150.5, 20.0), l.text)
        assertEquals(6.0, l.dots.minX - l.text.maxX, 0.0)
        assertEquals(18.0, l.dots.width, 0.0)
        assertEquals(390.0, l.ink.minX + l.ink.maxX, 0.0)
    }
}
