@file:Suppress("ktlint:standard:max-line-length")
@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.wizard

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.WizardAirportSheet
import net.chronoflow.feature.wizard.ui.AirportRowUi
import net.chronoflow.feature.wizard.ui.AirportSearchUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.seconds

class AirportSearchStateTest {
    @get:Rule val main: TestWatcher = WizardMainDispatcherRule()

    private suspend fun TestScope.searching(
        results: suspend (String) -> List<ApiAirportSuggestion> = { listOf(AMS, DXB, NRT) },
        block: suspend TestScope.(WizardFixture) -> Unit,
    ) = withWizard(configure = {
        draft = routeDraft()
        val base = wall.now
        now = { base.plusMillis(testScheduler.currentTime) }
        airportSearch =
            AirportSearchService({
                queries.add(it)
                results(it)
            }, now, { delay((it * 1000).roundToLong()) })
    }) { fixture ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { fixture.model.searchState.collect {} }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { fixture.model.retryEnabled.collect {} }
        runCurrent()
        fixture.model.openAirport(WizardAirportSheet(0, WizardAirportSheet.Side.from))
        runCurrent()
        block(fixture)
    }

    @Test fun searchChangesDoNotEmitRouteOrChromeState() =
        runTest(timeout = 60.seconds) {
            searching { f ->
                var headers = 0
                var routes = 0
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { f.model.chrome.collect { headers++ } }
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { f.model.where.collect { routes++ } }
                runCurrent()
                headers = 0
                routes = 0
                f.model.onQuery("AmsterdamX")
                advanceTimeBy(350)
                runCurrent()
                assertEquals(0, headers)
                assertEquals(0, routes)
            }
        }

    @Test fun shortQueriesShowTheHint() =
        runTest(timeout = 60.seconds) {
            searching { f ->
                f.model.onQuery("")
                runCurrent()
                assertEquals(AirportSearchUi.Hint, f.model.searchState.value)
                f.model.onQuery("A")
                runCurrent()
                assertEquals(AirportSearchUi.Hint, f.model.searchState.value)
                f.model.onQuery(" a ")
                runCurrent()
                assertEquals(AirportSearchUi.Hint, f.model.searchState.value)
            }
        }

    @Test fun searchingThenResults() =
        runTest(timeout = 60.seconds) {
            searching { f ->
                f.model.onQuery("Am")
                runCurrent()
                assertEquals(AirportSearchUi.Searching, f.model.searchState.value)
                advanceTimeBy(350)
                runCurrent()
                assertEquals(
                    AirportSearchUi.Results(listOf(AMS, DXB, NRT).map(AirportRowUi::from)),
                    f.model.searchState.value,
                )
            }
        }

    @Test fun emptyAndFilteredOutAreEmpty() =
        runTest(timeout = 60.seconds) {
            var results = emptyList<ApiAirportSuggestion>()
            searching(results = { results }) { f ->
                f.model.openAirport(WizardAirportSheet(0, WizardAirportSheet.Side.stop))
                f.model.onQuery("Am")
                advanceTimeBy(350)
                runCurrent()
                assertEquals(AirportSearchUi.Empty, f.model.searchState.value)
                results = listOf(AMS, NRT)
                f.model.onQuery("Du")
                advanceTimeBy(1020)
                runCurrent()
                assertEquals(AirportSearchUi.Empty, f.model.searchState.value)
                results = listOf(AMS, DXB, NRT)
                f.model.onQuery("Dub")
                advanceTimeBy(1020)
                runCurrent()
                assertEquals(AirportSearchUi.Results(listOf(AirportRowUi.from(DXB))), f.model.searchState.value)
            }
        }

    @Test fun failureMapsToFailedWithoutRetryAt() =
        runTest(timeout = 60.seconds) {
            searching(results = { throw DataFailure.Offline }) { f ->
                f.model.onQuery("Am")
                advanceTimeBy(350)
                runCurrent()
                assertEquals(AirportSearchUi.Failed(null), f.model.searchState.value)
                f.model.retrySearch()
                advanceTimeBy(1020)
                runCurrent()
                assertEquals(listOf("Am", "Am"), f.deps.queries)
            }
        }

    @Test fun throttleMapsToFailedWithRetryAt() =
        runTest(timeout = 60.seconds) {
            searching(results = { throw DataFailure.Throttled(60.0, 429) }) { f ->
                f.model.onQuery("Am")
                advanceTimeBy(350)
                runCurrent()
                assertEquals(AirportSearchUi.Failed(f.deps.now().plusSeconds(60)), f.model.searchState.value)
            }
        }

    @Test fun openingAndClosingTheSheetResetToHint() =
        runTest(timeout = 60.seconds) {
            searching { f ->
                f.model.onQuery("Am")
                advanceTimeBy(350)
                runCurrent()
                assertTrue(f.model.searchState.value is AirportSearchUi.Results)
                f.model.cancelAirport()
                runCurrent()
                assertEquals(AirportSearchUi.Hint, f.model.searchState.value)
                f.model.openAirport(WizardAirportSheet(0, WizardAirportSheet.Side.to))
                runCurrent()
                assertEquals(AirportSearchUi.Hint, f.model.searchState.value)
            }
        }

    @Test fun throttledRetryEnablesAtRetryAt() =
        runTest(timeout = 60.seconds) {
            searching(results = { throw DataFailure.Throttled(60.0, 429) }) { f ->
                f.model.onQuery("Am")
                advanceTimeBy(350)
                runCurrent()
                assertFalse(f.model.retryEnabled.value)
                advanceTimeBy(59_999)
                runCurrent()
                assertFalse(f.model.retryEnabled.value)
                advanceTimeBy(1)
                runCurrent()
                assertTrue(f.model.retryEnabled.value)
            }
        }
}
