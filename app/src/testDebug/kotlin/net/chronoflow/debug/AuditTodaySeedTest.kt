package net.chronoflow.debug

import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockScenarioValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import java.time.ZoneId

@RunWith(Parameterized::class)
class AuditTodaySeedTest(
    private val state: String,
) {
    @Test fun aliasKeepsTheExistingTripStateAfterMockRefresh() {
        val original = MockAppSeed.make(auditSeedNow)
        val seed =
            TestSeed.parse(
                mapOf(
                    TestSeed.TEST_ID_KEY to AUDIT_SEED_ID,
                    "CF_AUTH_TEST_AUDIT_SCREEN" to "today",
                    "CF_AUTH_TEST_AUDIT_STATE" to state,
                ),
            )
        val transformed = AuditMockSeed.transform(original, seed, auditSeedNow)
        val refreshed = MockAppSeed.refresh(transformed, auditSeedNow, ZoneId.systemDefault())
        val account = refreshed.accounts.getValue(MockScenarioValues.demoID)
        assertEquals(original.accounts.getValue(MockScenarioValues.demoID).snapshot, account.snapshot)
        assertEquals(original.accounts - MockScenarioValues.demoID, transformed.accounts - MockScenarioValues.demoID)
        assertTrue(account.trips.isNotEmpty())
        assertTrue(
            account.trips.all {
                if (state == "future") {
                    Instant.parse(it.planStartUtc) > auditSeedNow
                } else {
                    Instant.parse(it.planEndUtc) < auditSeedNow
                }
            },
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = listOf(arrayOf("future"), arrayOf("completed"))
    }
}
