@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.wizard.model

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

class WizardSaveFailureTest {
    @Test fun retrySecondsUseCeilingAtSubMillisecondPrecision() {
        val at = Instant.ofEpochSecond(1_800_000_000)
        assertEquals(1, WizardSaveFailure(WizardSaveFailure.Kind.throttle, at.plusNanos(1)).seconds(at))
        assertEquals(2, WizardSaveFailure(WizardSaveFailure.Kind.throttle, at.plusSeconds(1).plusNanos(1)).seconds(at))
        assertEquals(0, WizardSaveFailure(WizardSaveFailure.Kind.throttle, at).seconds(at))
        assertEquals(0, WizardSaveFailure(WizardSaveFailure.Kind.throttle, at.minusNanos(1)).seconds(at))
    }

    @Test fun aPositiveFractionalMillisecondSchedulesASuspension() =
        runTest(timeout = 60.seconds) {
            val fixture = WizardModelFixture(backgroundScope, testScheduler, ready = true)
            val model = fixture.model
            model.state =
                model.state.copy(
                    saveFailure = WizardSaveFailure(WizardSaveFailure.Kind.throttle, fixture.clock.now.plusNanos(1)),
                )
            model.restartRetryCountdown()
            assertEquals(1, model.retrySeconds)
            runCurrent()
            assertEquals(listOf(1L), fixture.checking.deadlines)
            fixture.clock.advance(0.001)
            fixture.checking.advance(1)
            assertEquals(0, model.retrySeconds)
            fixture.close()
        }
}
