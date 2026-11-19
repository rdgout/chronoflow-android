package net.chronoflow.core.ui.trip

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class TripCardEyebrowTest {
    private val start = Instant.parse("2026-11-08T10:00:00Z")

    @Test fun thirtyMinutesBeforeStart() {
        assertEquals(NextLabel.In(30), nextEyebrow(start, start.minusSeconds(1800)))
    }

    @Test fun oneMillisecondBeforeStartRoundsUp() {
        assertEquals(NextLabel.In(1), nextEyebrow(start, start.minusMillis(1)))
    }

    @Test fun atStartIsNow() {
        assertEquals(NextLabel.Now, nextEyebrow(start, start))
    }
}
