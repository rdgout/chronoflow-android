package net.chronoflow.core.wizard.model

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.testing.wizard.wizardAirport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class AirportSearchCacheTests {
    @Test fun hitSkipsPacingAndRequest() =
        runTest(timeout = 60.seconds) {
            val clock = WizardTestClock()
            val waits = mutableListOf<Double>()
            var requests = 0
            val airports = listOf(wizardAirport("AMS", "Amsterdam", "Europe/Amsterdam"))
            val service =
                AirportSearchService(
                    search = {
                        requests++
                        airports
                    },
                    now = { clock.now },
                    sleep = {
                        waits.add(it)
                        clock.advance(it)
                    },
                    cache = AirportSearchCache(),
                )
            assertEquals(airports, service.results("AMS"))
            waits.clear()
            assertEquals(airports, service.results(" ams "))
            assertEquals(1, requests)
            assertEquals(listOf(0.35), waits)
        }

    @Test fun entryExpiresAtExactlyOneHour() {
        val cache = AirportSearchCache()
        val now = WizardTestClock().now
        cache.put("AMS", emptyList(), now)
        assertNotNull(cache.get("ams", now.plusMillis(3_599_999)))
        assertNull(cache.get("ams", now.plusSeconds(3600)))
    }

    @Test fun evictsLeastRecentlyUsedBeyondSixtyFour() {
        val cache = AirportSearchCache()
        val now = WizardTestClock().now
        repeat(64) { cache.put("airport-$it", emptyList(), now) }
        assertNotNull(cache.get("airport-0", now))
        cache.put("airport-64", emptyList(), now)
        assertNull(cache.get("airport-1", now))
        assertNotNull(cache.get("airport-0", now))
        assertNotNull(cache.get("airport-64", now))
    }

    @Test fun failuresAreNotCached() =
        runTest(timeout = 60.seconds) {
            val clock = WizardTestClock()
            var requests = 0
            val service =
                AirportSearchService(
                    search = {
                        requests++
                        if (requests == 1) throw DataFailure.Offline
                        emptyList()
                    },
                    now = { clock.now },
                    sleep = { clock.advance(it) },
                    cache = AirportSearchCache(),
                )
            try {
                service.results("AMS")
                error("Expected offline")
            } catch (failure: DataFailure) {
                assertEquals(DataFailure.Offline, failure)
            }
            assertTrue(service.results("AMS").isEmpty())
            assertEquals(2, requests)
        }

    @Test fun emptyResultsAreCached() =
        runTest(timeout = 60.seconds) {
            val clock = WizardTestClock()
            var requests = 0
            val service =
                AirportSearchService(
                    search = {
                        requests++
                        emptyList()
                    },
                    now = { clock.now },
                    sleep = { clock.advance(it) },
                    cache = AirportSearchCache(),
                )
            assertTrue(service.results("AMS").isEmpty())
            assertTrue(service.results("ams").isEmpty())
            assertEquals(1, requests)
        }
}
