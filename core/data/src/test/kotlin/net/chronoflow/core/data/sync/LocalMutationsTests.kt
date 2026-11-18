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
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.OutboxKind
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.store.LocalMutationFailure
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.testing.sync.check
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncTrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.SQLException
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class LocalMutationsTests {
    @Test fun durableRestartAndRollback() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val row = store.enqueue(OutboxChange.CheckOff(check()))
                assertEquals(7, UUID.fromString(row.id).version())
                store.database.close()
                store.database.open(syncAccount)
                assertEquals(listOf(row), OutboxRepository(store.database).pending())
                assertEquals("done", TripRepository(store.database).checks(syncTrip, "hash").first().status)
                try {
                    store.enqueue(OutboxChange.CheckOff(check(status = CheckStatus.Skipped)))
                    error("expected duplicate id")
                } catch (
                    _: SQLException,
                ) {
                }
                assertEquals("done", TripRepository(store.database).checks(syncTrip, "hash").first().status)
            } finally {
                store.close()
            }
        }

    @Test fun supportedValuesAndSensitiveInputs() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                store.enqueue(
                    OutboxChange.Settings(
                        SettingsSection.Preferences,
                        JsonObject(
                            mapOf("time_format" to JsonPrimitive("12")),
                        ),
                    ),
                )
                store.enqueue(OutboxChange.Profile(ProfileChange("Edited", "demo@example.com")), 1)
                store.enqueue(OutboxChange.Walkthrough, 2)
                SettingsRepository(
                    store.database,
                ).upsert(
                    SettingsRecord(
                        "policies",
                        ApiJson.response.encodeToString(AccountPolicies.serializer(), AccountPolicies("1", "1")),
                    ),
                )
                store.enqueue(OutboxChange.Policies("1"), 3)
                assertEquals("Edited", AccountRepository(store.database).account()?.name)
                assertEquals(true, AccountRepository(store.database).account()?.walkthroughSeen)
                val settings = SettingsRepository(store.database).settings()
                assertEquals(
                    JsonPrimitive("12"),
                    (
                        ApiJson.response.parseToJsonElement(
                            settings
                                .first {
                                    it.section ==
                                        "preferences"
                                }.value,
                        ) as JsonObject
                    )["timeFormat"],
                )
                invalid(store, "password")
                invalid(store, "token")
                invalid(store, "recovery_code")
                invalid(store, "signature")
                invalid(store, "current_password")
                try {
                    store.enqueue(
                        OutboxChange.Settings(
                            SettingsSection.BodyClock,
                            JsonObject(
                                mapOf("habitual_bed" to JsonObject(mapOf("token" to JsonPrimitive("secret")))),
                            ),
                        ),
                    )
                    error("expected invalid payload")
                } catch (_: LocalMutationFailure.InvalidPayload) {
                }
                try {
                    store.enqueue(OutboxChange.Profile(ProfileChange("Other", "different@example.com")), 4)
                    error("expected online only")
                } catch (
                    _: LocalMutationFailure.OnlineOnly,
                ) {
                }
                try {
                    store.enqueue(OutboxChange.Unsupported(OutboxKind.Security, JsonObject(emptyMap())))
                    error("expected unsupported")
                } catch (
                    _: LocalMutationFailure.Unsupported,
                ) {
                }
                assertEquals(4, OutboxRepository(store.database).pending().size)
            } finally {
                store.close()
            }
        }

    // (new) A malformed durable row must not block accepting policies or finding a valid pending match.
    @Test fun malformedPendingPoliciesRowIsSkipped() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val policies =
                    ApiJson.response.encodeToString(AccountPolicies.serializer(), AccountPolicies("new", "1"))
                SettingsRepository(store.database).upsert(SettingsRecord("policies", policies))
                val outbox = OutboxRepository(store.database)
                val malformed =
                    OutboxRecord("bad", "settings", """{"section":"policies"}""", "2026-01-01T00:00:00.000Z")
                outbox.upsert(malformed)
                val row = store.enqueue(OutboxChange.Policies("new"), 1)
                assertEquals(listOf(malformed, row), outbox.pending())
                val accepted =
                    ApiJson.response.parseToJsonElement(
                        SettingsRepository(store.database).settings().single().value,
                    ) as JsonObject
                assertEquals(JsonPrimitive("new"), accepted["acceptedTermsVersion"])
                assertEquals(JsonPrimitive(row.createdAt), accepted["acceptedTermsAt"])
                assertEquals(row, store.enqueue(OutboxChange.Policies("new"), 2))
                assertEquals(listOf(malformed, row), outbox.pending())
            } finally {
                store.close()
            }
        }

    private suspend fun invalid(
        store: SyncStore,
        key: String,
    ) {
        try {
            store.enqueue(
                OutboxChange.Settings(
                    SettingsSection.Preferences,
                    JsonObject(
                        mapOf(key to JsonPrimitive("secret")),
                    ),
                ),
            )
            error("expected invalid payload")
        } catch (
            failure: LocalMutationFailure.InvalidPayload,
        ) {
            assertTrue(failure.message == null)
        }
    }
}
