package net.chronoflow.core.datamock

import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test

class MockSeedRefreshTests {
    companion object {
        @JvmStatic @BeforeClass
        fun warmFixtures() {
            warmMockFixtures()
        }
    }

    @Test fun walkthroughOverrideCoversBothSeededAccountsSeen() = walkthroughOverrideCoversBothSeededAccounts(true)

    @Test fun walkthroughOverrideCoversBothSeededAccountsNotSeen() = walkthroughOverrideCoversBothSeededAccounts(false)

    private fun walkthroughOverrideCoversBothSeededAccounts(seen: Boolean) {
        val seed = MockAppSeed.make(instant("2026-11-07T12:00:00Z"), walkthroughSeen = seen, zone = mockZone)
        assertEquals(seen, seed.accounts.getValue(MockScenarioValues.demoID).walkthroughSeen)
        assertEquals(seen, seed.accounts.getValue(MockScenarioValues.twoFactorID).walkthroughSeen)
    }
}
