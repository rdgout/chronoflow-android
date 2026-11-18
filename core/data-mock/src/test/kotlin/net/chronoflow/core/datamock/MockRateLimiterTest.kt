package net.chronoflow.core.datamock

import net.chronoflow.core.model.DataFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Android limiter helper coverage (new). */
class MockRateLimiterTest {
    private fun filled(): Map<String, MockRateLimiter.Window> =
        (1..5).fold(emptyMap()) { windows, _ -> MockRateLimiter.hit("forgot", now = 100.0, windows = windows) }

    @Test fun sixthHitIsThrottled() {
        val windows = filled()
        assertEquals(MockRateLimiter.Window(5, 160.0), windows["forgot"])
        val error =
            assertThrows(
                DataFailure.Throttled::class.java,
            ) { MockRateLimiter.hit("forgot", now = 100.0, windows = windows) }
        assertEquals(DataFailure.Throttled(60.0, 429), error)
    }

    @Test fun expiryRestartsTheWindow() {
        assertEquals(
            MockRateLimiter.Window(1, 220.0),
            MockRateLimiter.hit("forgot", now = 160.0, windows = filled())["forgot"],
        )
    }

    @Test fun retryUsesRemainingTime() {
        val error =
            assertThrows(
                DataFailure.Throttled::class.java,
            ) { MockRateLimiter.hit("forgot", now = 110.0, windows = filled()) }
        assertEquals(50.0, error.retryAfterSeconds, 0.0)
    }
}
