@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.wizard.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.testing.wizard.WizardTestClock
import net.chronoflow.core.testing.wizard.wizardAirport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds

class AirportSearchSessionTests {
    private class Fixture(
        scope: CoroutineScope,
        scheduler: TestCoroutineScheduler,
        search: suspend (String) -> List<ApiAirportSuggestion> = { emptyList() },
    ) {
        val queries = mutableListOf<String>()
        private val base = WizardTestClock().now
        val now = { base.plusMillis(scheduler.currentTime) }
        val session =
            AirportSearchSession(
                AirportSearchService({
                    queries.add(it)
                    search(it)
                }, now, { delay((it * 1000).roundToLong()) }),
                scope,
                now,
            )
    }

    @Test fun shortQueryIsHintWithoutRequest() =
        runTest(timeout = 60.seconds) {
            val fixture = Fixture(backgroundScope, testScheduler)
            fixture.session.setQuery("D")
            runCurrent()
            advanceTimeBy(1000)
            assertEquals(AirportSearchState.Hint, fixture.session.state.value)
            assertTrue(fixture.queries.isEmpty())
        }

    @Test fun typingWithinDebounceSendsOnlyLastQuery() =
        runTest(timeout = 60.seconds) {
            val fixture = Fixture(backgroundScope, testScheduler)
            fixture.session.setQuery("A")
            advanceTimeBy(100)
            fixture.session.setQuery("AM")
            advanceTimeBy(100)
            fixture.session.setQuery("AMS")
            advanceTimeBy(349)
            assertTrue(fixture.queries.isEmpty())
            advanceTimeBy(1)
            runCurrent()
            assertEquals(listOf("AMS"), fixture.queries)
        }

    @Test fun emptyResultIsEmptyState() =
        runTest(timeout = 60.seconds) {
            val fixture = Fixture(backgroundScope, testScheduler)
            fixture.session.setQuery("AMS")
            advanceTimeBy(350)
            runCurrent()
            assertEquals(AirportSearchState.Empty("AMS"), fixture.session.state.value)
        }

    @Test fun overSixtyCodePointsIsEmptyWithoutRequest() =
        runTest(timeout = 60.seconds) {
            val fixture = Fixture(backgroundScope, testScheduler)
            val query = "😀".repeat(61)
            fixture.session.setQuery(query)
            runCurrent()
            assertEquals(AirportSearchState.Empty(query), fixture.session.state.value)
            assertTrue(fixture.queries.isEmpty())
        }

    @Test fun failureThenRetrySearchesAgain() =
        runTest(timeout = 60.seconds) {
            var attempts = 0
            val fixture =
                Fixture(backgroundScope, testScheduler) {
                    if (++attempts == 1) throw DataFailure.Offline
                    emptyList()
                }
            fixture.session.setQuery("AMS")
            advanceTimeBy(350)
            runCurrent()
            assertEquals(AirportSearchState.Failed("AMS"), fixture.session.state.value)
            fixture.session.retry()
            advanceTimeBy(1020)
            runCurrent()
            assertEquals(AirportSearchState.Empty("AMS"), fixture.session.state.value)
            assertEquals(listOf("AMS", "AMS"), fixture.queries)
        }

    @Test fun throttledCarriesRetryAtAndIgnoresEarlyRetry() =
        runTest(timeout = 60.seconds) {
            var attempts = 0
            val fixture =
                Fixture(backgroundScope, testScheduler) {
                    if (++attempts == 1) throw DataFailure.Throttled(42.0, 429)
                    emptyList()
                }
            fixture.session.setQuery("AMS")
            advanceTimeBy(350)
            runCurrent()
            val failure = AirportSearchState.Throttled("AMS", fixture.now().plusSeconds(42))
            assertEquals(failure, fixture.session.state.value)
            fixture.session.retry()
            runCurrent()
            advanceTimeBy(41_999)
            fixture.session.retry()
            runCurrent()
            assertEquals(1, attempts)
            assertEquals(failure, fixture.session.state.value)
            advanceTimeBy(1)
            fixture.session.retry()
            advanceTimeBy(350)
            runCurrent()
            assertEquals(2, attempts)
            assertEquals(AirportSearchState.Empty("AMS"), fixture.session.state.value)
        }

    @Test fun searchingKeepsStaleResults() =
        runTest(timeout = 60.seconds) {
            val airports = listOf(wizardAirport("AMS", "Amsterdam", "Europe/Amsterdam"))
            val fixture = Fixture(backgroundScope, testScheduler) { airports }
            fixture.session.setQuery("AMS")
            advanceTimeBy(350)
            runCurrent()
            assertEquals(AirportSearchState.Results("AMS", airports), fixture.session.state.value)
            fixture.session.setQuery("HND")
            runCurrent()
            assertEquals(AirportSearchState.Searching("HND", airports), fixture.session.state.value)
        }

    @Test fun codePointLengthCountsSurrogatePairsOnce() =
        runTest(timeout = 60.seconds) {
            val fixture = Fixture(backgroundScope, testScheduler)
            fixture.session.setQuery("😀😀")
            advanceTimeBy(350)
            runCurrent()
            assertEquals(listOf("😀😀"), fixture.queries)
        }

    @Test fun sixtyCodePointsMayExceedSixtyUtf16Units() =
        runTest(timeout = 60.seconds) {
            val fixture = Fixture(backgroundScope, testScheduler)
            val query = "😀".repeat(60)
            fixture.session.setQuery(query)
            advanceTimeBy(350)
            runCurrent()
            assertEquals(listOf(query), fixture.queries)
        }

}
