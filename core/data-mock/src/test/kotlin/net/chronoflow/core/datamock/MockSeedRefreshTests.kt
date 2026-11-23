package net.chronoflow.core.datamock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.time.Duration.Companion.seconds

class MockSeedRefreshTests {
    companion object {
        private val refreshFixtures: Unit by lazy {
            warmMockFixtures()
            runTest(timeout = 60.seconds) {
                withContext(Dispatchers.IO) {
                    val root =
                        java.nio.file.Files
                            .createTempDirectory("mock-seed-refresh-warmup")
                            .toFile()
                    try {
                        val now = instant("2026-11-07T12:00:00Z")
                        val old = MockAppSeed.make(now.minusSeconds(10 * 86400L), zone = mockZone)
                        val token = "fixture-warmup-session"
                        val store = FileMockStateStore(root)
                        store.save(
                            old.copy(
                                sessions =
                                    mapOf(
                                        token to
                                            MockSession(
                                                demoID,
                                                mockDeviceID,
                                                seconds(now) + 86400,
                                            ),
                                    ),
                            ),
                        )
                        val source =
                            MockDataSource(store, now = { now }, sleep = {}, bearer = { token }, zone = mockZone)
                        source.fetchIdentity()
                        source.fetchTrips()
                        store.load()
                    } finally {
                        check(root.deleteRecursively())
                    }
                }
            }
        }

        @JvmStatic @BeforeClass
        fun warmFixtures() {
            refreshFixtures
        }
    }

    @Test fun walkthroughOverrideCoversBothSeededAccountsSeen() = walkthroughOverrideCoversBothSeededAccounts(true)

    @Test fun walkthroughOverrideCoversBothSeededAccountsNotSeen() = walkthroughOverrideCoversBothSeededAccounts(false)

    private fun walkthroughOverrideCoversBothSeededAccounts(seen: Boolean) {
        val seed = MockAppSeed.make(instant("2026-11-07T12:00:00Z"), walkthroughSeen = seen, zone = mockZone)
        assertEquals(seen, seed.accounts.getValue(MockScenarioValues.demoID).walkthroughSeen)
        assertEquals(seen, seed.accounts.getValue(MockScenarioValues.twoFactorID).walkthroughSeen)
    }

    @get:Rule val temporary = TemporaryFolder()

    @Test fun restoresTenDayOldSeedWithoutLosingUserStateCurrentAnchor() =
        restoresTenDayOldSeedWithoutLosingUserState(
            false,
        )

    @Test fun restoresTenDayOldSeedWithoutLosingUserStateLegacyAnchor() =
        restoresTenDayOldSeedWithoutLosingUserState(
            true,
        )

    private fun restoresTenDayOldSeedWithoutLosingUserState(legacyAnchor: Boolean) =
        runTest(timeout = 60.seconds) {
            val now = instant("2026-11-07T12:00:00Z")
            val earlier = now.minusSeconds(10 * 86400L)
            var original = MockAppSeed.make(earlier, zone = mockZone)
            val store = FileMockStateStore(temporary.root)
            val customID = "0199a3c0-0000-7000-8000-000000000fff"
            original =
                original.copy(
                    accounts =
                        original.accounts.mapValues { (_, value) ->
                            var account =
                                value.copy(
                                    snapshot = value.snapshot.with(name = "Kept"),
                                    walkthroughSeen = true,
                                    recoveryCodes = emptySet(),
                                )
                            val custom = account.trips.last()
                            val customDetail = account.details.getValue(custom.card.id)
                            account =
                                account.copy(
                                    trips = account.trips + custom.copy(card = custom.card.copy(id = customID)),
                                    details =
                                        account.details +
                                            (
                                                customID to
                                                    customDetail.copy(
                                                        trip = customDetail.trip.copy(id = customID),
                                                    )
                                            ),
                                )
                            val seededID =
                                account.trips
                                    .first()
                                    .card.id
                            val detail = account.details.getValue(seededID)
                            val keys =
                                detail.plan.blocks
                                    .filter { it.actionable }
                                    .mapNotNull { it.checkKey }
                            val marks =
                                keys.take(2).zip(listOf(CheckStatus.Done, CheckStatus.Skipped)).map { (key, status) ->
                                    CheckMark(key, status, iso(earlier))
                                }
                            account.copy(
                                details =
                                    account.details +
                                        (seededID to detail.copy(checkState = detail.checkState.copy(marks = marks))),
                            )
                        },
                    sessions = mapOf("retained-token" to MockSession(demoID, mockDeviceID, seconds(now) + 86400)),
                )
            withContext(Dispatchers.IO) {
                store.save(original)
                if (legacyAnchor) {
                    store.file.writeText(
                        JsonObject(
                            MockJson.parseToJsonElement(store.file.readText()).jsonObject - "seedAnchor",
                        ).toString(),
                    )
                }
            }

            fun source(): MockDataSource =
                MockDataSource(store, now = {
                    now
                }, sleep = {}, bearer = { "retained-token" }, zone = mockZone)
            val source = source()
            assertEquals("Kept", source.fetchIdentity().name)
            val trips = source.fetchTrips()
            assertEquals(
                setOf(TripStatus.Active, TripStatus.Upcoming, TripStatus.Completed),
                trips
                    .dropLast(1)
                    .map {
                        it.card.progress.status
                    }.toSet(),
            )
            val restored = withContext(Dispatchers.IO) { store.load()!! }
            assertEquals(MockAppSeed.anchor(now, mockZone), restored.seedAnchor)
            assertNotEquals(original.seedAnchor, restored.seedAnchor)
            val fresh = MockAppSeed.make(now, zone = mockZone)
            for ((accountID, before) in original.accounts) {
                val after = restored.accounts.getValue(accountID)
                assertEquals(before.snapshot, after.snapshot)
                assertEquals(before.walkthroughSeen, after.walkthroughSeen)
                assertEquals(before.recoveryCodes, after.recoveryCodes)
                assertEquals(before.trips.last(), after.trips.last())
                assertEquals(before.details[customID], after.details[customID])
                for ((index, trip) in after.trips.dropLast(1).withIndex()) {
                    val expected = fresh.accounts.getValue(accountID).trips[index]
                    assertEquals(expected.card.progress, trip.card.progress)
                    assertEquals(expected.card.canComplete, trip.card.canComplete)
                    assertEquals(expected.keepOffline, trip.keepOffline)
                    val detail = after.details.getValue(trip.card.id)
                    val expectedDetail =
                        fresh.accounts
                            .getValue(accountID)
                            .details
                            .getValue(trip.card.id)
                    assertEquals(expectedDetail.plan, detail.plan)
                    assertEquals(trip.planInputHash, detail.checkState.planInputHash)
                    assertEquals(if (trip.keepOffline) detail.checkState else null, trip.checkState)
                    assertEquals(if (trip.keepOffline) detail.detailVersion else null, trip.detailVersion)
                }
                val id =
                    after.trips
                        .first()
                        .card.id
                val oldDetail = before.details.getValue(id)
                val newDetail = after.details.getValue(id)
                assertEquals(2, newDetail.checkState.marks.size)
                for ((old, new) in oldDetail.checkState.marks.zip(newDetail.checkState.marks)) {
                    val blockIndex = oldDetail.plan.blocks.indexOfFirst { it.checkKey == old.key }
                    assertTrue(blockIndex >= 0)
                    assertEquals(newDetail.plan.blocks[blockIndex].checkKey, new.key)
                    assertEquals(old.status, new.status)
                    assertEquals(old.markedAt, new.markedAt)
                }
            }
            assertEquals(trips, source().fetchTrips())
            assertEquals(restored, withContext(Dispatchers.IO) { store.load() })
        }
}
