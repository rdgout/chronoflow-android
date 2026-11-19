package net.chronoflow.feature.launch.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LaunchIntroTests {
    @Test fun offsetsProgressAndAbsoluteClockDelays() {
        val clock = ManualLaunchClock()
        val intro = LaunchIntro(2300.milliseconds, clock)
        assertEquals(listOf(0, 100, 150, 300, 400, 450).map { it.milliseconds }, (0..5).map(LaunchIntro::offset))
        for (index in 0..5) {
            val start = intro.start + LaunchIntro.offset(index)
            assertEquals(0.0, intro.progress(index, start - 1.milliseconds), 0.0)
            assertEquals(0.0, intro.progress(index, start), 0.0)
            assertEquals(CubicBezier.intro.value(0.5), intro.progress(index, start + 225.milliseconds), 1e-12)
            assertEquals(1.0, intro.progress(index, start + 450.milliseconds), 0.0)
            assertEquals(1.0, intro.progress(index, 3200.milliseconds), 0.0)
        }
        clock.advance(2400.milliseconds)
        assertEquals(-0.1, intro.delay(0), 1e-12)
        assertEquals(0.35, intro.delay(5), 1e-12)
    }

    @Test fun introIsClaimedOnceAndFreezingKeepsTheSharedEpoch() {
        val clock = ManualLaunchClock()
        val intro = LaunchIntro(2300.milliseconds, clock, 2450.milliseconds)
        assertTrue(intro.claim())
        assertFalse(intro.claim())
        assertTrue(intro.hasPlayed)
        assertEquals(2450.milliseconds, intro.frozenTime)
        clock.advance(10.seconds)
        assertEquals(0.0, intro.progress(2, requireNotNull(intro.frozenTime)), 0.0)
        assertEquals(1.0, LaunchIntro(8700.milliseconds, clock).progress(5, 9600.milliseconds), 0.0)
    }
}
