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

class PlanRetentionTests {
    @Test fun leaseReferenceCountsAndPendingPreventEviction() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                val retention = PlanRetention()
                val first = retention.hold(syncTrip)
                val second = retention.hold(syncTrip)
                source.configure(trips = listOf(card(keep = false)))
                val pull = FullPull(store.sync, source, retention, now = { syncDate })
                pull.run(store.epoch)
                assertNotNull(TripRepository(store.database).plan(syncTrip, "hash"))
                first.release()
                first.release()
                pull.run(store.epoch)
                assertNotNull(TripRepository(store.database).plan(syncTrip, "hash"))
                store.enqueue(OutboxChange.CheckOff(check()))
                second.release()
                pull.run(store.epoch)
                assertNotNull(TripRepository(store.database).plan(syncTrip, "hash"))
                val row = OutboxRepository(store.database).pending().first()
                OutboxRepository(store.database).remove(row.id)
                pull.run(store.epoch)
                assertNull(TripRepository(store.database).plan(syncTrip, "hash"))
            } finally {
                store.close()
            }
        }

    @Test fun pendingDeletedTripKeepsPlan() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                val pull = FullPull(store.sync, source, now = { syncDate })
                pull.run(store.epoch)
                store.enqueue(OutboxChange.CheckOff(check()))
                source.configure(trips = emptyList())
                pull.run(store.epoch)
                assertNotNull(TripRepository(store.database).plan(syncTrip, "hash"))
            } finally {
                store.close()
            }
        }
}
