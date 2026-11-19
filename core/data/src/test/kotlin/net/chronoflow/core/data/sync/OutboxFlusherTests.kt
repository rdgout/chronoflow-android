package net.chronoflow.core.data.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.sync.support.SyncStore
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.MutationReceipt
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckKey
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckState
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripCheckResult
import net.chronoflow.core.model.contracts.TripDetailResponse
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.sync.CheckOffReconciler
import net.chronoflow.core.sync.FullPull
import net.chronoflow.core.sync.OutboxFailurePolicy
import net.chronoflow.core.sync.OutboxFlusher
import net.chronoflow.core.sync.SyncRefusal
import net.chronoflow.core.sync.SyncRefusalNotice
import net.chronoflow.core.testing.sync.ScriptedSource
import net.chronoflow.core.testing.sync.check
import net.chronoflow.core.testing.sync.detail
import net.chronoflow.core.testing.sync.identity
import net.chronoflow.core.testing.sync.syncDate
import net.chronoflow.core.testing.sync.syncOther
import net.chronoflow.core.testing.sync.syncTrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

class OutboxFlusherTests {
    private fun setting(
        section: SettingsSection,
        key: String,
        value: String,
    ) = OutboxChange.Settings(section, JsonObject(mapOf(key to JsonPrimitive(value))))

    @Test fun refusalNoticesCoverOnlyDrawnChanges() {
        assertEquals(
            SyncRefusalNotice.SleepTimes,
            SyncRefusalNotice.from(setting(SettingsSection.BodyClock, "habitual_bed", "22:00")),
        )
        assertEquals(
            SyncRefusalNotice.StepDone(BlockType.Sleep),
            SyncRefusalNotice.from(OutboxChange.CheckOff(check())),
        )
        assertNull(SyncRefusalNotice.from(setting(SettingsSection.BodyClock, "chronotype", "early")))
        assertNull(
            SyncRefusalNotice.from(
                OutboxChange.Settings(
                    SettingsSection.BodyClock,
                    JsonObject(mapOf("habitual_bed" to JsonPrimitive("22:00"), "chronotype" to JsonPrimitive("early"))),
                ),
            ),
        )
        assertNull(SyncRefusalNotice.from(setting(SettingsSection.Preferences, "time_format", "12")))
        assertNull(
            SyncRefusalNotice.from(
                OutboxChange.Settings(
                    SettingsSection.Reminders,
                    JsonObject(mapOf("light" to JsonPrimitive(false))),
                ),
            ),
        )
        assertNull(SyncRefusalNotice.from(OutboxChange.CheckOff(check(status = CheckStatus.Skipped))))
        assertNull(SyncRefusalNotice.from(OutboxChange.CheckOff(check(status = null))))
        assertNull(SyncRefusalNotice.from(OutboxChange.Profile(ProfileChange("Edited", "demo@example.com"))))
        assertNull(SyncRefusalNotice.from(OutboxChange.Policies("1")))
        val legacy = """{"id":"old","kind":"settings","failedAt":"then","lastError":"validation"}"""
        assertNull(ApiJson.response.decodeFromString(SyncRefusal.serializer(), legacy).notice)
    }

    @Test fun fifoDependencyBlockingAndParkedKinds() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                val first = store.enqueue(setting(SettingsSection.Preferences, "time_format", "12"))
                store.enqueue(setting(SettingsSection.Preferences, "time_format", "24"), 1)
                store.enqueue(OutboxChange.Walkthrough, 2)
                val repository = OutboxRepository(store.database)
                val payload = JsonObject(mapOf("tripId" to JsonPrimitive(syncTrip))).toString()
                repository.upsert(OutboxRecord("parked0", "complete-trip", payload, first.createdAt))
                repository.upsert(OutboxRecord("parked1", "security", payload, first.createdAt))
                repository.upsert(OutboxRecord("parked2", "data-export", payload, first.createdAt))
                source.configure(failures = listOf(DataFailure.Server(500), null))
                val result = OutboxFlusher(store.sync, source, { syncDate }).run(store.epoch)
                assertEquals(2, source.changes.size)
                assertEquals(OutboxPayload.change(first), source.changes.first())
                assertEquals(1.0, result.retryAfter)
                assertEquals(3, result.parked)
                val rows = repository.pending()
                assertEquals(5, rows.size)
                assertEquals(1, rows.first().attempts)
                assertEquals("server", rows.first().lastError)
            } finally {
                store.close()
            }
        }

    @Test fun responseRollbackReplaysAbsoluteWrite() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                store.enqueue(OutboxChange.Profile(ProfileChange("Edited", "demo@example.com")))
                val source = ScriptedSource()
                source.configure(receipts = listOf(MutationReceipt.Identity(identity(syncOther))))
                val first = OutboxFlusher(store.sync, source, { syncDate }).run(store.epoch)
                assertNotNull(first.retryAfter)
                assertEquals(1, OutboxRepository(store.database).pending().size)
                source.configure(receipts = listOf(MutationReceipt.Identity(identity())))
                OutboxFlusher(store.sync, source, { syncDate }).run(store.epoch)
                assertEquals(2, source.changes.size)
                assertTrue(OutboxRepository(store.database).pending().isEmpty())
            } finally {
                store.close()
            }
        }

    @Test fun repeatedProfileRefusalRestoresLastAcceptedName() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                store.enqueue(OutboxChange.Profile(ProfileChange("First", "demo@example.com")))
                store.enqueue(OutboxChange.Profile(ProfileChange("Second", "demo@example.com")), 1)
                source.configure(failures = listOf(DataFailure.Server(422), DataFailure.Server(422)))
                OutboxFlusher(store.sync, source, { syncDate }).run(store.epoch)
                assertEquals("Local", AccountRepository(store.database).account()?.name)
            } finally {
                store.close()
            }
        }
}
