@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.wizard.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.testing.wizard.GenerationGate
import net.chronoflow.core.testing.wizard.WizardSearchProbe
import net.chronoflow.core.testing.wizard.WizardTestClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

class WizardSearchTests {
    @Test fun debouncesAndPacesRequestsAcrossSheetsBelowSixtyPerMinute() =
        runTest(timeout = 60.seconds) {
            val clock = WizardTestClock()
            val start = clock.now
            val requests = WizardSearchProbe()
            val service =
                AirportSearchService(
                    search = {
                        requests.record(it, clock.now)
                        emptyList()
                    },
                    now = { clock.now },
                    sleep = {
                        assertTrue(it > 0)
                        clock.advance(it)
                    },
                )
            assertTrue(service.results("").isEmpty())
            assertTrue(service.results("A").isEmpty())
            assertTrue(service.results("A".repeat(61)).isEmpty())
            repeat(65) { service.results(" AMS ") }
            val sent = requests.times
            assertTrue(requests.queries.all { it == "AMS" })
            assertEquals(65, sent.size)
            assertEquals(0.35, Duration.between(start, sent.first()).toMillis() / 1000.0, 0.0001)
            assertTrue(sent.zipWithNext().all { (first, second) -> Duration.between(first, second).toMillis() >= 1019 })
            assertTrue(sent.all { date -> sent.count { it >= date && it < date.plusSeconds(60) } <= 60 })
        }

    @Test fun cancelledDebounceNeverMakesRequest() =
        runTest(timeout = 60.seconds) {
            val requests = WizardSearchProbe()
            val gate = GenerationGate()
            val clock = WizardTestClock()
            val service =
                AirportSearchService(
                    search = {
                        requests.record(it, clock.now)
                        emptyList()
                    },
                    now = { clock.now },
                    sleep = {
                        gate.open()
                        awaitCancellation()
                    },
                )
            val task = backgroundScope.async { service.results("AMS") }
            gate.wait()
            task.cancelAndJoin()
            try {
                task.await()
                error("Expected cancellation")
            } catch (_: CancellationException) {
                assertTrue(requests.times.isEmpty())
            }
        }

    @Test fun serverThrottleSetsSharedWait() =
        runTest(timeout = 60.seconds) {
            val clock = WizardTestClock()
            val requests = WizardSearchProbe()
            val service =
                AirportSearchService(
                    search = {
                        requests.record(it, clock.now)
                        if (requests.times.size == 1) throw DataFailure.Throttled(42.0, 429)
                        emptyList()
                    },
                    now = { clock.now },
                    sleep = { clock.advance(it) },
                )
            try {
                service.results("AMS")
                error("Expected throttle")
            } catch (failure: DataFailure.Throttled) {
                assertEquals(DataFailure.Throttled(42.0, 429), failure)
            }
            service.results("HND")
            assertTrue(Duration.between(requests.times[0], requests.times[1]).toMillis() >= 42_000)
        }
}
