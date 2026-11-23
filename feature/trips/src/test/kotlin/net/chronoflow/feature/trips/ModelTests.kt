@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.trips

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class ModelTests {
    @get:Rule internal val main = MainDispatcherRule()

    @Test fun stopReleasesModelAndRestartObserves() =
        runTest(timeout = 60.seconds) {
            val deps = FakeTripsDependencies(testScheduler)
            val fixture = ModelFixture(deps)
            try {
                val first =
                    backgroundScope.launch(
                        UnconfinedTestDispatcher(testScheduler),
                    ) { fixture.model.state.collect {} }
                runCurrent()
                assertEquals(1, deps.records.subscriptionCount.value)
                first.cancel()
                advanceTimeBy(5_000)
                runCurrent()
                assertEquals(0, deps.records.subscriptionCount.value)
                deps.records.value = listOf(tripRecord("fresh"))
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { fixture.model.state.collect {} }
                runCurrent()
                assertEquals(1, deps.records.subscriptionCount.value)
                assertEquals(listOf("fresh"), fixture.model.cards().map { it.id })
            } finally {
                fixture.close()
            }
        }
}
