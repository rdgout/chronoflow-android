@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.trips

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.plan.FixedPlanClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripsTests {
    @get:Rule internal val main = MainDispatcherRule()

    @Test fun fixedClockFinishesWithoutBlockingInitialLoadOrLaterRepositoryUpdates() =
        runTest(timeout = 60.seconds) {
            val deps = FakeTripsDependencies(testScheduler).apply { clock = FixedPlanClock(tripsNow()) }
            val fixture = ModelFixture(deps)
            try {
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { fixture.model.state.collect {} }
                deps.records.value = listOf(tripRecord())
                runCurrent()
                assertEquals(listOf("t1"), fixture.model.cards().map { it.id })
                deps.records.value = listOf(tripRecord("t2"))
                runCurrent()
                assertEquals(listOf("t2"), fixture.model.cards().map { it.id })
            } finally {
                fixture.close()
            }
        }

    @Test fun tombstonedTripsDisappearAndEmptyFirstLoadFinishes() =
        runTest(timeout = 60.seconds) {
            val deps = FakeTripsDependencies(testScheduler)
            val fixture = ModelFixture(deps)
            try {
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { fixture.model.state.collect {} }
                runCurrent()
                assertEquals(TripsUiState.Empty, fixture.model.state.value)
                deps.records.value = listOf(tripRecord())
                runCurrent()
                assertTrue(fixture.model.state.value is TripsUiState.Content)
                deps.records.value = listOf(tripRecord().copy(deletedLocally = true))
                runCurrent()
                assertEquals(TripsUiState.Empty, fixture.model.state.value)
                deps.records.value = listOf(tripRecord().copy(deletedAt = "now"))
                runCurrent()
                assertEquals(TripsUiState.Empty, fixture.model.state.value)
            } finally {
                fixture.close()
            }
        }
}
