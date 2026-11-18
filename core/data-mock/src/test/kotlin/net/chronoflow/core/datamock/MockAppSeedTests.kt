package net.chronoflow.core.datamock

import net.chronoflow.core.testing.LargeTimelineFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.time.ZoneId

internal val mockZone: ZoneId = ZoneId.of("Europe/Amsterdam")

internal fun warmMockFixtures() {
    MockAppSeed.decodedFixtures
    LargeTimelineFixtures.fixture("large60")
    LargeTimelineFixtures.fixture("large30")
}

/** Port of MockAppSeedTests.swift's DataSourceSelectionTests extension; factory selection stays in 4e. */
class MockAppSeedTests {
    companion object {
        @JvmStatic @BeforeClass
        fun warmFixtures() {
            warmMockFixtures()
        }
    }

    @Test fun cachedFixtureTemplatesRebaseWithoutSharingSeedMutations() {
        val now = instant("2026-11-06T00:00:00Z")
        val first = MockAppSeed.make(now, zone = mockZone)
        val shifted =
            MockAppSeed.make(
                now.plusSeconds(3 * 86400L),
                unverified = true,
                walkthroughSeen = true,
                zone = mockZone,
            )
        val firstAccount = first.accounts.getValue(MockScenarioValues.demoID)
        val shiftedAccount = shifted.accounts.getValue(MockScenarioValues.demoID)
        assertNotEquals(firstAccount.trips.map { it.planStartUtc }, shiftedAccount.trips.map { it.planStartUtc })
        assertTrue(firstAccount.snapshot.hasVerifiedEmail)
        assertFalse(firstAccount.walkthroughSeen)
        assertFalse(shiftedAccount.snapshot.hasVerifiedEmail)
        assertTrue(shiftedAccount.walkthroughSeen)
        val repeated = MockAppSeed.make(now, zone = mockZone)
        assertEquals(first.accounts, repeated.accounts)
        assertNotEquals(first.signingKey, repeated.signingKey)
    }

    /** Port of LargeTimelineFixtureTests.defaultMockHasNoPerformanceTrips. */
    @Test fun defaultMockHasNoPerformanceTrips() {
        val now = instant("2026-11-06T00:00:00Z")
        assertEquals(
            7,
            MockAppSeed
                .make(now, zone = mockZone)
                .accounts
                .getValue(MockScenarioValues.demoID)
                .trips.size,
        )
        assertEquals(
            9,
            MockAppSeed
                .make(
                    now,
                    includeTimelinePerformanceTrips = true,
                    zone = mockZone,
                ).accounts
                .getValue(MockScenarioValues.demoID)
                .trips.size,
        )
    }
}
