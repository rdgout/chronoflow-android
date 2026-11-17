package net.chronoflow.core.data.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MinuteClockPolicyTest {
    private fun ms(text: String) = Instant.parse(text).toEpochMilli()

    @Test fun delayLandsFifteenMillisecondsAfterTheNextBoundary() {
        assertEquals(60_015L, delayToNextMinuteMs(ms("2026-10-13T08:00:00Z")))
        assertEquals(30_015L, delayToNextMinuteMs(ms("2026-10-13T08:00:30Z")))
        assertEquals(16L, delayToNextMinuteMs(ms("2026-10-13T08:00:59.999Z")))
    }

    @Test fun delayHoldsBeforeTheEpoch() {
        for (negative in listOf(-1L, -59_999L, -60_000L, -123_456_789L)) {
            val delay = delayToNextMinuteMs(negative)
            assertTrue("delay $delay for $negative", delay > 15 && delay <= 60_015)
            assertEquals(0L, Math.floorMod(negative + delay - 15, 60_000L))
        }
    }

    @Test fun minuteSourcesEmitOncePerMinute() {
        val last = ms("2026-10-13T08:00:30Z")
        val sameMinute = ms("2026-10-13T08:00:59.999Z")
        val nextMinute = ms("2026-10-13T08:01:00.005Z")
        for (event in listOf(ClockEvent.TIME_TICK, ClockEvent.TIMER)) {
            assertFalse(shouldEmit(event, last, sameMinute))
            assertTrue(shouldEmit(event, last, nextMinute))
            assertTrue(shouldEmit(event, null, sameMinute))
        }
    }

    @Test fun changesAndStartAlwaysEmit() {
        val last = ms("2026-10-13T08:00:30Z")
        val sameMinute = ms("2026-10-13T08:00:40Z")
        for (event in listOf(
            ClockEvent.START,
            ClockEvent.TIME_CHANGED,
            ClockEvent.TIMEZONE_CHANGED,
            ClockEvent.DATE_CHANGED,
        )) {
            assertTrue(shouldEmit(event, last, sameMinute))
        }
    }
}
