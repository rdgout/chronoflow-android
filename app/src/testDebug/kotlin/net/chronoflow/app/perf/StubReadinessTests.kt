package net.chronoflow.app.perf

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.StubConfig
import net.chronoflow.app.StubSession
import net.chronoflow.app.StubStored
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.launch.SignedOutReadiness
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.feature.launch.model.LaunchClock
import net.chronoflow.feature.launch.model.ManualLaunchClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StubReadinessTests {
    @Test fun testFalseBecomesTrueAtTheSharedLaunchDeadline() =
        runTest(
            timeout = 60.seconds,
        ) { checkDelayedStream(false) }

    @Test fun testProtocolDispatchUsesTheSameInjectedDeadline() =
        runTest(
            timeout = 60.seconds,
        ) { checkDelayedStream(true) }

    private suspend fun TestScope.checkDelayedStream(throughProtocol: Boolean) {
        val clock = ObservedLaunchClock()
        clock.manual.advance(2.seconds)
        val source =
            StubReadiness(
                StubConfig(session = StubSession.SignedIn, stored = StubStored.No, readyDelay = 3500.milliseconds),
                clock,
            )
        val values = mutableListOf<Boolean>()
        val protocol: LaunchReadiness = source
        backgroundScope.launch {
            (if (throughProtocol) protocol.storedDataUpdates() else source.storedDataUpdates()).toList(values)
        }
        runCurrent()
        assertEquals(listOf(3500.milliseconds), clock.deadlines)
        assertEquals(1, clock.manual.pendingSleeps)
        clock.manual.advance(3499.milliseconds)
        runCurrent()
        assertEquals(1, clock.manual.pendingSleeps)
        clock.manual.advance(3500.milliseconds)
        runCurrent()
        assertEquals(listOf(false, true), values)
        assertEquals(0, clock.manual.pendingSleeps)
        source.allowFirstLoad()
        source.allowFirstLoad()
        assertEquals(1, source.firstLoadCount)
    }

    @Test fun testPassedDeadlineEmitsReadinessWithoutAnotherDelay() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            clock.advance(4.seconds)
            val source = StubReadiness(StubConfig(stored = StubStored.No, readyDelay = 3500.milliseconds), clock)
            assertEquals(listOf(false, true), source.storedDataUpdates().toList())
            assertEquals(0, clock.pendingSleeps)
        }

    @Test fun testCancellingTheStreamCancelsItsClockSleep() =
        runTest(timeout = 60.seconds) {
            val clock = ManualLaunchClock()
            val source = StubReadiness(StubConfig(stored = StubStored.No, readyDelay = 8.seconds), clock)
            val values = mutableListOf<Boolean>()
            val job = backgroundScope.launch { source.storedDataUpdates().toList(values) }
            runCurrent()
            assertEquals(1, clock.pendingSleeps)
            job.cancel()
            runCurrent()
            assertEquals(listOf(false), values)
            assertEquals(0, clock.pendingSleeps)
        }

    @Test fun neverStoredEmitsNothingUntilCancelled() =
        runTest(timeout = 60.seconds) {
            val source = StubReadiness(StubConfig(stored = StubStored.Never), ManualLaunchClock())
            val values = mutableListOf<Boolean>()
            val job = backgroundScope.launch { source.storedDataUpdates().toList(values) }
            runCurrent()
            assertTrue(values.isEmpty())
            job.cancel()
            runCurrent()
            assertTrue(job.isCancelled)
        }

    @Test fun nativeSessionDelegates() =
        runTest(timeout = 60.seconds) {
            val native =
                object : LaunchReadiness by SignedOutReadiness {
                    override suspend fun session() = SessionPresence.SignedIn("native")
                }
            assertEquals(
                SessionPresence.SignedIn("native"),
                StubReadiness(StubConfig(), ManualLaunchClock(), native).session(),
            )
        }
}

private class ObservedLaunchClock : LaunchClock {
    val manual = ManualLaunchClock()
    val deadlines = mutableListOf<Duration>()
    override val now get() = manual.now

    override suspend fun sleepUntil(deadline: Duration) {
        deadlines += deadline
        manual.sleepUntil(deadline)
    }
}
