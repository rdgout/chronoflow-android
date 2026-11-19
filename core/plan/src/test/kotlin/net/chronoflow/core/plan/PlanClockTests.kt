package net.chronoflow.core.plan

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.testing.FakePlanClock
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class PlanClockTests {
    private val t0 = Instant.ofEpochSecond(0)
    private val t1 = Instant.ofEpochSecond(60)

    @Test fun fixedPlanClockEmitsOnceAndCompletes() =
        runTest(timeout = 60.seconds) {
            FixedPlanClock(t0).values().test {
                assertThat(awaitItem()).isEqualTo(t0)
                awaitComplete()
            }
        }

    @Test fun fakePlanClockGivesTheStartThenEveryAdvance() =
        runTest(timeout = 60.seconds) {
            val clock = FakePlanClock(t0)
            clock.values().test {
                assertThat(awaitItem()).isEqualTo(t0)
                clock.advance(t1)
                assertThat(awaitItem()).isEqualTo(t1)
                clock.advance(t1)
                assertThat(awaitItem()).isEqualTo(t1)
                cancelAndIgnoreRemainingEvents()
            }
            assertThat(clock.now).isEqualTo(t1)
        }

    @Test fun lateCollectorGetsTheCurrentInstant() =
        runTest(timeout = 60.seconds) {
            val clock = FakePlanClock(t0)
            clock.advance(t1)
            clock.values().test {
                assertThat(awaitItem()).isEqualTo(t1)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
