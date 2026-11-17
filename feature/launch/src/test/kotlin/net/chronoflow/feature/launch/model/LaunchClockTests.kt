package net.chronoflow.feature.launch.model

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Logic
class LaunchClockTests {
    @Test fun mediaClockUsesInjectedMonotonicTime() {
        var nanos = 0L
        val clock = MediaLaunchClock(100_000_000) { nanos }
        assertEquals(0.milliseconds, clock.now)
        nanos = 800_000_000
        assertEquals(700.milliseconds, clock.now)
    }

    @Test fun manualClockResumesOnlyDueSleepers() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            var resumed = false
            launch {
                clock.sleepUntil(700.milliseconds)
                resumed = true
            }
            runCurrent()
            assertEquals(1, clock.pendingSleeps)
            clock.advance(699.milliseconds)
            runCurrent()
            assertFalse(resumed)
            clock.advance(700.milliseconds)
            runCurrent()
            assertTrue(resumed)
            clock.sleepUntil(600.milliseconds)
            assertEquals(0, clock.pendingSleeps)
        }

    @Test fun cancelledSleeperIsRemoved() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            val job = launch { clock.sleepUntil(1.seconds) }
            runCurrent()
            assertEquals(1, clock.pendingSleeps)
            job.cancel()
            runCurrent()
            assertEquals(0, clock.pendingSleeps)
        }
}
