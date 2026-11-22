package net.chronoflow.debug

import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockScenarioValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuditEmptyTripSeedTest {
    @Test fun emptyTripsClearsOnlyTheDemoAccountTripData() {
        val original = MockAppSeed.make(auditSeedNow)
        val seed = TestSeed.parse(mapOf(TestSeed.TEST_ID_KEY to AUDIT_SEED_ID, "CF_REFRESH_TEST_EMPTY_TRIPS" to "1"))
        val result = AuditMockSeed.transform(original, seed, auditSeedNow)
        val demo = result.accounts.getValue(MockScenarioValues.demoID)
        assertTrue(demo.trips.isEmpty())
        assertTrue(demo.details.isEmpty())
        assertEquals(original.accounts - MockScenarioValues.demoID, result.accounts - MockScenarioValues.demoID)
        assertEquals(original.accounts.getValue(MockScenarioValues.demoID).snapshot, demo.snapshot)
    }

    @Test fun anUngatedEmptyTripsRequestDoesNothing() {
        val original = MockAppSeed.make(auditSeedNow)
        val seed = TestSeed.parse(mapOf("CF_REFRESH_TEST_EMPTY_TRIPS" to "1"))
        assertEquals(original, AuditMockSeed.transform(original, seed, auditSeedNow))
    }
}
