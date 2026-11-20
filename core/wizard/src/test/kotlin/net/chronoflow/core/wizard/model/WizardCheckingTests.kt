@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.wizard.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.wizard.generation.TripGenerationReviewResult
import net.chronoflow.core.wizard.generationInput
import net.chronoflow.core.wizard.generationReview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class WizardCheckingTests {
    @Test fun ticksMinimumAndFinalPauseUseInjectedClock() =
        runTest(timeout = 60.seconds) {
            val clock = CheckingTestClock(testScheduler)
            val result = TripGenerationReviewResult(ApiTripReviewInput(generationInput()), generationReview())
            val ticks = mutableListOf<Int>()
            var completed = false
            val task =
                backgroundScope.launch {
                    WizardChecking.run(clock.clock, { result }) { event ->
                        if (event is WizardChecking.Event.Tick) ticks.add(event.count)
                        if (event is WizardChecking.Event.Complete) completed = true
                    }
                }
            runCurrent()
            assertEquals(2, clock.waiterCount)
            clock.advance(399)
            assertTrue(ticks.isEmpty())
            clock.advance(400)
            assertEquals(listOf(1), ticks)
            clock.advance(800)
            assertEquals(listOf(1, 2), ticks)
            clock.advance(1199)
            assertFalse(completed)
            assertEquals(listOf(1, 2), ticks)
            clock.advance(1200)
            assertEquals(listOf(1, 2, 3), ticks)
            assertTrue(clock.deadlines.contains(1500))
            assertFalse(completed)
            clock.advance(1500)
            task.join()
            assertTrue(completed)
            assertEquals(0, clock.waiterCount)
            clock.advance(100_000)
            assertEquals(listOf(1, 2, 3), ticks)
        }

    @Test fun slowAtTenSecondsAndTimeoutIsServerFailureAtThirty() =
        runTest(timeout = 60.seconds) {
            val clock = CheckingTestClock(testScheduler)
            var slow = false
            var failure: DataFailure? = null
            val task =
                backgroundScope.launch {
                    WizardChecking.run(clock.clock, {
                        clock.clock.sleepUntilMillis(100_000)
                        throw DataFailure.Offline
                    }) { event ->
                        if (event == WizardChecking.Event.Slow) slow = true
                        if (event is WizardChecking.Event.Failed) failure = event.failure
                    }
                }
            runCurrent()
            assertEquals(2, clock.waiterCount)
            clock.advance(9999)
            assertFalse(slow)
            assertNull(failure)
            clock.advance(10_000)
            assertTrue(slow)
            assertTrue(clock.deadlines.contains(30_000))
            clock.advance(29_999)
            assertNull(failure)
            clock.advance(30_000)
            task.join()
            assertEquals(DataFailure.Server(null), failure)
            assertEquals(0, clock.waiterCount)
        }

    @Test fun cancellationDropsLateResponseAndTimers() =
        runTest(timeout = 60.seconds) {
            val clock = CheckingTestClock(testScheduler)
            var events = 0
            val task =
                backgroundScope.launch {
                    WizardChecking.run(clock.clock, {
                        clock.clock.sleepUntilMillis(100_000)
                        throw DataFailure.Offline
                    }) { events++ }
                }
            runCurrent()
            assertEquals(2, clock.waiterCount)
            task.cancelAndJoin()
            clock.advance(100_000)
            assertEquals(0, events)
            assertEquals(0, clock.waiterCount)
            assertTrue(task.isCancelled)
        }

    @Test fun requestCancellationWhileCheckingIsActiveShowsServerFailure() =
        runTest(timeout = 60.seconds) {
            val clock = CheckingTestClock(testScheduler)
            var failure: DataFailure? = null
            WizardChecking.run(clock.clock, { throw CancellationException() }) { event ->
                if (event is WizardChecking.Event.Failed) {
                    failure =
                        event.failure
                }
            }
            assertEquals(DataFailure.Server(null), failure)
            assertEquals(0L, clock.now)
            assertEquals(0, clock.waiterCount)
        }

    @Test fun cancellationDropsAForeignFailureFromARequest() =
        runTest(timeout = 60.seconds) {
            val clock = CheckingTestClock(testScheduler)
            val gate = CompletableDeferred<Unit>()
            val events = mutableListOf<WizardChecking.Event>()
            val task =
                backgroundScope.launch {
                    WizardChecking.run(clock.clock, {
                        withContext(NonCancellable) { gate.await() }
                        throw DataFailure.Offline
                    }, events::add)
                }
            runCurrent()
            task.cancel()
            gate.complete(Unit)
            runCurrent()
            task.join()
            assertTrue(task.isCancelled)
            assertTrue(events.isEmpty())
            assertEquals(0, clock.waiterCount)
        }
}
