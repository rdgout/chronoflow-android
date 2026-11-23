package net.chronoflow.debug

import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockScenarioValues
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AuditMockSeedTest(
    private val state: String,
    private val seconds: Int,
) {
    @Test fun verificationDeadlineUsesTheInjectedClock() {
        val original = MockAppSeed.make(auditSeedNow, unverified = true)
        val seed =
            TestSeed.parse(
                mapOf(
                    TestSeed.TEST_ID_KEY to AUDIT_SEED_ID,
                    "CF_AUTH_TEST_AUDIT_SCREEN" to "verify",
                    "CF_AUTH_TEST_AUDIT_STATE" to state,
                ),
            )
        val result = AuditMockSeed.transform(original, seed, auditSeedNow)
        val account = result.accounts.getValue(MockScenarioValues.demoID)
        assertEquals(
            if (seconds ==
                0
            ) {
                0.0
            } else {
                auditSeedNow.epochSecond.toDouble() + seconds
            },
            account.resendAvailableAt,
            0.0,
        )
        assertEquals(original.accounts.getValue(MockScenarioValues.demoID).trips, account.trips)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("waiting", 0),
                arrayOf("countdown", 0),
                arrayOf("throttle", 60),
                arrayOf("failedThrottle", 60),
            )
    }
}
