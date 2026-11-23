@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.trips

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class DecodeRecoveryTests {
    @get:Rule internal val main = MainDispatcherRule()

    @Test fun badCardIsSkippedWhileOtherTripsKeepUpdating() =
        runTest(timeout = 60.seconds) {
            val deps = FakeTripsDependencies(testScheduler)
            val fixture = ModelFixture(deps)
            try {
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { fixture.model.state.collect {} }
                deps.records.value = listOf(tripRecord("bad").copy(card = "{}"), tripRecord("healthy"))
                runCurrent()
                assertEquals(listOf("healthy"), fixture.model.cards().map { it.id })
                deps.records.value = listOf(tripRecord("bad").copy(card = "{}"), tripRecord("updated"))
                runCurrent()
                assertEquals(listOf("updated"), fixture.model.cards().map { it.id })
                deps.records.value = listOf(tripRecord("bad"), tripRecord("updated"))
                runCurrent()
                assertEquals(
                    setOf("bad", "updated"),
                    fixture.model
                        .cards()
                        .map { it.id }
                        .toSet(),
                )
                deps.records.value = listOf(tripRecord("bad").copy(card = "{}"), tripRecord("updated"))
                runCurrent()
                assertEquals(listOf("updated"), fixture.model.cards().map { it.id })
            } finally {
                fixture.close()
            }
        }
}
