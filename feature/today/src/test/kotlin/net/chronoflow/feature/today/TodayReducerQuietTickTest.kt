package net.chronoflow.feature.today

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TodayReducerQuietTickTest {
    @Test fun quietMinuteKeepsTheStructuralStateEqual() =
        runTest(timeout = 60.seconds) {
            val input = todayInput(todayFormatter(StandardTestDispatcher(testScheduler)))
            assertEquals(TodayReducer.reduce(input), TodayReducer.reduce(input.copy(now = input.now.plusSeconds(60))))
        }

    @Test fun nextBlockStartChangesTheStructuralState() =
        runTest(timeout = 60.seconds) {
            val input = todayInput(todayFormatter(StandardTestDispatcher(testScheduler)))
            val before = TodayReducer.reduce(input)
            val boundary = TodayReducer.reduce(input.copy(now = input.now.plusSeconds(3600)))
            assertNotEquals(before, boundary)
        }
}
