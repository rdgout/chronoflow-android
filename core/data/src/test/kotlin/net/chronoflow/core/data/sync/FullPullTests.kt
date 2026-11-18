package net.chronoflow.core.data.sync

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.sync.support.SyncStore
import net.chronoflow.core.database.store.fetchTrip
import net.chronoflow.core.database.store.saveTrip
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.sync.CheckOffReconciler
import net.chronoflow.core.sync.FullPull
import net.chronoflow.core.sync.PlanRetention
import net.chronoflow.core.testing.sync.ScriptedSource
import net.chronoflow.core.testing.sync.card
import net.chronoflow.core.testing.sync.check
import net.chronoflow.core.testing.sync.detail
import net.chronoflow.core.testing.sync.identity
import net.chronoflow.core.testing.sync.syncDate
import net.chronoflow.core.testing.sync.syncTrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class FullPullTests {
    @Test fun orderMarkerAndDetailVersion() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                val pull = FullPull(store.sync, source, now = { syncDate })
                pull.run(store.epoch)
                assertEquals(listOf("identity", "account", "trips", "detail"), source.calls)
                assertEquals("2027-01-15T08:00:00Z", AccountRepository(store.database).account()?.syncedAt)
                pull.run(store.epoch)
                assertEquals(1, source.calls.count { it == "detail" })
                source.configure(trips = listOf(card(version = "v2")), detail = detail(version = "v2"))
                pull.run(store.epoch)
                assertEquals(2, source.calls.count { it == "detail" })
                assertEquals("v2", TripRepository(store.database).plan(syncTrip, "hash")?.detailVersion)
            } finally {
                store.close()
            }
        }

    @Test fun unverifiedStopsBeforeProtectedPulls() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                source.configure(identity = identity(verified = false))
                val result = FullPull(store.sync, source, now = { syncDate }).run(store.epoch)
                assertTrue(result.verificationRequired)
                assertEquals(listOf("identity"), source.calls)
                assertNull(AccountRepository(store.database).account()?.syncedAt)
                assertEquals(false, AccountRepository(store.database).account()?.hasVerifiedEmail)
            } finally {
                store.close()
            }
        }

    @Test fun accountAndCardsRollbackTogether() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                val invalid = card().let { it.copy(card = it.card.copy(departureUtc = "")) }
                source.configure(trips = listOf(invalid))
                try {
                    FullPull(store.sync, source, now = { syncDate }).run(store.epoch)
                    error("expected rollback")
                } catch (
                    _: DataFailure.Server,
                ) {
                }
                assertNull(AccountRepository(store.database).account()?.syncedAt)
                assertEquals(listOf("policies"), SettingsRepository(store.database).settings().map { it.section })
                assertNull(TripRepository(store.database).trips().first().deletedAt)
            } finally {
                store.close()
            }
        }

    @Test fun markerSurvivesPartialAndOfflineFailure() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                source.configure(failMethod = "detail", methodFailure = DataFailure.Server(500))
                val pull = FullPull(store.sync, source, now = { syncDate })
                assertEquals(1.0, pull.run(store.epoch).retryAfter)
                val marker = AccountRepository(store.database).account()?.syncedAt
                assertNotNull(marker)
                source.configure(failMethod = "trips")
                try {
                    pull.run(store.epoch)
                    error("expected offline")
                } catch (_: DataFailure.Offline) {
                }
                assertEquals(marker, AccountRepository(store.database).account()?.syncedAt)
            } finally {
                store.close()
            }
        }

    @Test fun emptyTombstonesAndNoLocalResurrection() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                val pull = FullPull(store.sync, source, now = { syncDate })
                pull.run(store.epoch)
                source.configure(trips = emptyList())
                pull.run(store.epoch)
                assertNotNull(TripRepository(store.database).trips().first().deletedAt)
                assertNull(TripRepository(store.database).plan(syncTrip, "hash"))
                store.database.write { sql ->
                    sql.saveTrip(requireNotNull(sql.fetchTrip(syncTrip)).copy(deletedLocally = true))
                }
                source.configure(trips = listOf(card()))
                pull.run(store.epoch)
                assertNotNull(TripRepository(store.database).trips().first().deletedAt)
                assertEquals(1, source.calls.count { it == "detail" })
            } finally {
                store.close()
            }
        }

    @Test fun detail404KeepsListedCardAndQueuedCheckOffWithoutRetry() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                val row = store.enqueue(OutboxChange.CheckOff(check()))
                source.configure(failMethod = "detail", methodFailure = DataFailure.Server(404))
                val pull = FullPull(store.sync, source, now = { syncDate })
                assert404Pull(store, pull, row)
                assert404Pull(store, pull, row)
                val prepared = CheckOffReconciler(pull).prepare(row, store.epoch)
                assertEquals(OutboxChange.CheckOff(check()), prepared?.let { OutboxPayload.change(it) })
                assertEquals(listOf(row.id), OutboxRepository(store.database).pending().map { it.id })
                assertTrue(SettingsRepository(store.database).settings().none { it.section == "syncFailure" })
                store.enqueue(OutboxChange.CheckOff(check(status = null)), 1)
                assertEquals(2, OutboxRepository(store.database).pending().size)
            } finally {
                store.close()
            }
        }

    private suspend fun assert404Pull(
        store: SyncStore,
        pull: FullPull,
        row: OutboxRecord,
    ) {
        assertNull(pull.run(store.epoch).retryAfter)
        val trips = TripRepository(store.database).trips()
        assertEquals(1, trips.size)
        assertNull(trips.first().deletedAt)
        assertEquals("2027-01-15T08:00:00Z", AccountRepository(store.database).account()?.syncedAt)
        assertEquals(listOf(row.id), OutboxRepository(store.database).pending().map { it.id })
    }
}
