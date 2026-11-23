package net.chronoflow.core.datamock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.api.TwoFactorInput
import net.chronoflow.core.testing.LargeTimelineFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds

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

    @get:Rule val temporary = TemporaryFolder()

    private fun appMock(store: MockStateStore = MemoryMockStateStore()): MockDataSource =
        MockDataSource(store = store, now = MockClock()::now, sleep = {}, zone = mockZone, io = Dispatchers.IO)

    @Test fun defaultAppMockSignsInDemoWithRNSettingsAndTrips() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val source = appMock(store)
            assertNull(store.load())
            val session = login(source)
            assertEquals(demoID, session.account.id)
            assertEquals("Demo Traveller", session.account.name)
            assertTrue(session.account.hasVerifiedEmail)
            assertFalse(session.account.walkthroughSeen)
            val snapshot = source.fetchAccount()
            assertTrue(snapshot.settings.sleepAnswered)
            assertEquals(JsonPrimitive("18_64"), snapshot.settings.bodyClock["ageBand"])
            assertEquals(JsonPrimitive(true), snapshot.settings.preferences["showOptionalItems"])
            assertEquals(JsonPrimitive(15), snapshot.settings.reminders["leadMin"])
            assertEquals(JsonPrimitive(3), snapshot.settings.planCredits["credits"])
            assertEquals("2026-10-01", snapshot.settings.policies.acceptedTermsVersion)
            assertEquals(JsonPrimitive("ready"), snapshot.readiness["variant"])
            assertEquals(JsonPrimitive(false), snapshot.security["twoFactorEnabled"])
            assertEquals(JsonPrimitive(2), snapshot.security["passkeyCount"])
            assertEquals(listOf("iPhone 16", "MacBook Pro"), list(source).map { it.name })
            val trips = source.fetchTrips()
            assertEquals(7, trips.size)
            assertEquals(listOf("HND", "JFK", "SYD", "LHR", "DXB", "AMS", "AMS"), trips.map { it.card.to.iata })
            for (trip in trips) {
                val detail = source.fetchTripDetail(trip.card.id).detail
                assertEquals(trip.planInputHash, detail.checkState.planInputHash)
                assertTrue(detail.checkState.marks.isEmpty())
                assertTrue(detail.plan.blocks.isNotEmpty())
            }
            assertTrue(store.load()!!.mail.isEmpty())
            assertEquals(2, store.load()!!.accounts.size)
        }

    @Test fun defaultAppMockChallengesTwoFactorAndAcceptsRNProofCode() =
        defaultAppMockChallengesTwoFactorAndAcceptsRNProof(false)

    @Test fun defaultAppMockChallengesTwoFactorAndAcceptsRNProofRecovery() =
        defaultAppMockChallengesTwoFactorAndAcceptsRNProof(true)

    private fun defaultAppMockChallengesTwoFactorAndAcceptsRNProof(recovery: Boolean) =
        runTest(timeout = 60.seconds) {
            val source = appMock()
            val challenge =
                (
                    source.signIn(
                        passwordInput(email = MockScenarioValues.twoFactorEmail),
                    ) as SignInResult.TwoFactor
                ).challengeId
            assertEquals(DataFailure.Unauthorized(401), failure { source.fetchIdentity() })
            val session =
                source.completeTwoFactor(
                    TwoFactorInput(
                        challenge,
                        if (recovery) {
                            TwoFactorInput.Proof.RecoveryCode(
                                MockScenarioValues.recoveryCode,
                            )
                        } else {
                            TwoFactorInput.Proof.Code(MockScenarioValues.twoFactorCode)
                        },
                    ),
                )
            assertEquals(twoFactorID, session.account.id)
            assertTrue(session.account.hasVerifiedEmail)
            assertEquals(7, source.fetchTrips().size)
            val snapshot = source.fetchAccount()
            assertEquals(JsonPrimitive(true), snapshot.security["twoFactorEnabled"])
            assertNotNull(snapshot.security["twoFactorConfirmedAt"].string)
            assertEquals(JsonPrimitive(0), snapshot.security["passkeyCount"])
            assertEquals(
                AccountSecurityResult.Passkeys(emptyList()),
                source.accountSecurity(AccountSecurityChange.ListPasskeys),
            )
        }

    @Test fun authAcceptanceScenariosPersistAndStartOfflineBeforeAnyRequestVerified() =
        authAcceptanceScenariosPersistAndStartOfflineBeforeAnyRequest(false)

    @Test fun authAcceptanceScenariosPersistAndStartOfflineBeforeAnyRequestUnverified() =
        authAcceptanceScenariosPersistAndStartOfflineBeforeAnyRequest(true)

    private fun authAcceptanceScenariosPersistAndStartOfflineBeforeAnyRequest(unverified: Boolean) =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val seed = MockAppSeed.make(clock.now(), unverified, walkthroughSeen = true, zone = mockZone)
            val store = MemoryMockStateStore()
            val source =
                MockDataSource(store, seed, now = clock::now, sleep = {}, zone = mockZone, io = Dispatchers.Unconfined)
            val session = login(source)
            assertEquals(!unverified, session.account.hasVerifiedEmail)
            assertTrue(session.account.walkthroughSeen)
            val offline =
                MockDataSource(store, offline = true, now = clock::now, sleep = {
                }, bearer = { session.token }, zone = mockZone, io = Dispatchers.Unconfined)
            assertEquals(DataFailure.Offline, failure { offline.fetchIdentity() })
            assertEquals(DataFailure.Offline, failure { offline.signOut() })
            offline.setOffline(false)
            assertEquals(!unverified, offline.fetchIdentity().hasVerifiedEmail)
            offline.signOut()
            assertEquals(DataFailure.Unauthorized(401), failure { offline.fetchIdentity() })
            assertEquals(2, store.load()!!.accounts.size)
        }

    @Test fun defaultAppMockReseedsEmptyAndOldStatesAndPreservesCurrentChanges() =
        runTest(timeout = 60.seconds) {
            suspend fun reseed(old: MockServerState) {
                val store = MemoryMockStateStore(old)
                login(appMock(store))
                assertEquals(MockServerState.currentSchemaVersion, store.load()!!.schemaVersion)
                assertEquals(2, store.load()!!.accounts.size)
            }
            reseed(MockServerState())
            reseed(scenarioState().copy(schemaVersion = 0))
            val store = MemoryMockStateStore()
            val source = appMock(store)
            val session = login(source)
            source.send(OutboxChange.Profile(ProfileChange("Kept", MockScenarioValues.demoEmail)))
            source.send(OutboxChange.Walkthrough)
            val restarted =
                MockDataSource(store, now = MockClock()::now, sleep = {
                }, bearer = { session.token }, zone = mockZone, io = Dispatchers.Unconfined)
            assertEquals("Kept", restarted.fetchIdentity().name)
            assertTrue(restarted.fetchIdentity().walkthroughSeen)
        }

    @Test fun defaultAppMockKeepsRecoveryConsumptionAcrossRestart() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val first = appMock(store)
            val challenge =
                (
                    first.signIn(
                        passwordInput(email = MockScenarioValues.twoFactorEmail),
                    ) as SignInResult.TwoFactor
                ).challengeId
            first.completeTwoFactor(
                TwoFactorInput(challenge, TwoFactorInput.Proof.RecoveryCode(MockScenarioValues.recoveryCode)),
            )
            val restarted = appMock(store)
            val next =
                (
                    restarted.signIn(
                        passwordInput(email = MockScenarioValues.twoFactorEmail),
                    ) as SignInResult.TwoFactor
                ).challengeId
            failure {
                restarted.completeTwoFactor(
                    TwoFactorInput(next, TwoFactorInput.Proof.RecoveryCode(MockScenarioValues.recoveryCode)),
                )
            }
        }

    @Test fun defaultAppMockReseedsIncompatibleFilesEmptyObject() = defaultAppMockReseedsIncompatibleFiles("{}")

    @Test fun defaultAppMockReseedsIncompatibleFilesCorrupt() = defaultAppMockReseedsIncompatibleFiles("corrupt")

    @Test fun defaultAppMockReseedsIncompatibleFilesLegacy() = defaultAppMockReseedsIncompatibleFiles("legacy")

    private fun defaultAppMockReseedsIncompatibleFiles(payload: String) =
        runTest(timeout = 60.seconds) {
            val store = FileMockStateStore(temporary.root)
            val data =
                if (payload == "legacy") {
                    val seed = MockAppSeed.make(MockClock().now(), zone = mockZone)
                    JsonObject(
                        MockJson.encodeToJsonElement(MockServerState.serializer(), seed).jsonObject - "schemaVersion",
                    ).toString()
                } else {
                    payload
                }
            withContext(Dispatchers.IO) {
                store.file.parentFile.mkdirs()
                store.file.writeText(data)
            }
            login(appMock(store))
            assertEquals(2, withContext(Dispatchers.IO) { store.load()!!.accounts.size })
        }

    @Test fun explicitEmptyAndCustomSeedsBypassAppReseeding() =
        runTest(timeout = 60.seconds) {
            val emptyStore = MemoryMockStateStore()
            val empty =
                MockDataSource(emptyStore, MockServerState(), now = MockClock()::now, sleep = {
                }, zone = mockZone, io = Dispatchers.Unconfined)
            failure { login(empty) }
            assertTrue(emptyStore.load()!!.accounts.isEmpty())
            val custom = scenarioState().copy(accounts = scenarioState().accounts - twoFactorID)
            val store = MemoryMockStateStore()
            val source =
                MockDataSource(
                    store,
                    custom,
                    now = MockClock()::now,
                    sleep = {},
                    zone = mockZone,
                    io = Dispatchers.Unconfined,
                )
            login(source)
            assertEquals(1, store.load()!!.accounts.size)
            assertEquals(1, source.fetchTrips().size)
        }

    @Test fun releaseDefaultMockHasNoDemoSeed() =
        runTest(timeout = 60.seconds) {
            val store = MemoryMockStateStore()
            val source =
                MockDataSource(store, now = MockClock()::now, sleep = {
                }, debugAppSeed = false, zone = mockZone, io = Dispatchers.Unconfined)
            failure { login(source) }
            assertTrue(store.load()!!.accounts.isEmpty())
        }
}
