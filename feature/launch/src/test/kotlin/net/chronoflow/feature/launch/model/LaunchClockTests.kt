package net.chronoflow.feature.launch.model

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LaunchClockTests {
    @Test fun mediaClockUsesInjectedMonotonicTime() {
        var nanos = 0L
        val clock = MediaLaunchClock(100_000_000) { nanos }
        assertEquals(0.milliseconds, clock.now)
        nanos = 800_000_000
        assertEquals(700.milliseconds, clock.now)
    }

    @Test fun mediaClockRechecksDeadlineAfterMillisecondTimerResumesEarly() =
        runTest(timeout = 60.seconds) {
            var nanos = 100_426_832L
            val clock = MediaLaunchClock(0) { nanos }
            var resumed = false
            val job =
                launch {
                    clock.sleepUntil(3500.milliseconds)
                    resumed = true
                }
            runCurrent()
            // Handler deadlines use whole uptime milliseconds, unlike the monotonic clock's nanoseconds.
            nanos = 3_499_573_168L
            advanceTimeBy(3400)
            runCurrent()
            assertFalse(resumed)
            assertEquals(3_499_573_168.nanoseconds, clock.now)
            nanos = 3_500_573_168L
            advanceTimeBy(1)
            runCurrent()
            assertTrue(resumed)
            job.join()
        }

    @Test fun mediaClockRemainsCancellableAfterAnEarlyTimer() =
        runTest(timeout = 60.seconds) {
            var nanos = 100_426_832L
            val clock = MediaLaunchClock(0) { nanos }
            var resumed = false
            val job =
                launch {
                    clock.sleepUntil(3500.milliseconds)
                    resumed = true
                }
            runCurrent()
            nanos = 3_499_573_168L
            advanceTimeBy(3400)
            runCurrent()
            job.cancel()
            job.join()
            nanos = 3_600_000_000L
            advanceTimeBy(100)
            runCurrent()
            assertFalse(resumed)
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
