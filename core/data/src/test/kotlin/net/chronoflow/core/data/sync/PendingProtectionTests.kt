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

class PendingProtectionTests {
    @Test fun protectLocalValuesEvenWhenQueuedDuringPull() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                source.configure(hook = { method ->
                    if (method == "trips") {
                        store.enqueue(OutboxChange.Settings(SettingsSection.BodyClock, obj("habitual_bed", "22:00")))
                        store.enqueue(OutboxChange.Settings(SettingsSection.Preferences, obj("time_format", "12")), 1)
                        store.enqueue(OutboxChange.Profile(ProfileChange("Edited", "demo@example.com")), 2)
                        store.enqueue(OutboxChange.Walkthrough, 3)
                        val policies = AccountPolicies("new", "1")
                        val encoded = ApiJson.response.encodeToString(AccountPolicies.serializer(), policies)
                        SettingsRepository(store.database).upsert(SettingsRecord("policies", encoded))
                        store.enqueue(OutboxChange.Policies("new"), 4)
                        store.enqueue(OutboxChange.CheckOff(check()), 5)
                    }
                })
                FullPull(store.sync, source, now = { syncDate }).run(store.epoch)
                val account = AccountRepository(store.database).account()
                assertEquals("Edited", account?.name)
                assertEquals(true, account?.walkthroughSeen)
                val sections = SettingsRepository(store.database).settings()
                val settings = sections.associate { it.section to ApiJson.response.parseToJsonElement(it.value) }
                assertFalse("readiness" in settings)
                assertEquals(JsonPrimitive("22:00"), (settings["bodyClock"] as JsonObject)["habitualBed"])
                assertEquals(JsonPrimitive("12"), (settings["preferences"] as JsonObject)["timeFormat"])
                assertEquals(JsonPrimitive("new"), (settings["policies"] as JsonObject)["acceptedTermsVersion"])
                assertEquals("done", TripRepository(store.database).checks(syncTrip, "hash").first().status)
            } finally {
                store.close()
            }
        }

    @Test fun queuedCompletionPreservedFromLegacyRow() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                OutboxRepository(
                    store.database,
                ).upsert(OutboxRecord("parked", "complete-trip", obj("tripId", syncTrip).toString(), "1"))
                FullPull(store.sync, ScriptedSource(), now = { syncDate }).run(store.epoch)
                val trip = TripRepository(store.database).trips().first()
                val card = ApiJson.response.parseToJsonElement(trip.card) as JsonObject
                assertEquals(JsonPrimitive("completed"), (card["progress"] as JsonObject)["status"])
            } finally {
                store.close()
            }
        }

    private fun obj(
        key: String,
        value: String,
    ) = JsonObject(mapOf(key to JsonPrimitive(value)))
}
