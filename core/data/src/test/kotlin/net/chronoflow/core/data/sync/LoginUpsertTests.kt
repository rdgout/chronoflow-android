package net.chronoflow.core.data.sync

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.store.upsertSessionAccount
import net.chronoflow.core.data.sync.support.SyncStore
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncOther
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class LoginUpsertTests {
    private fun loginTest(block: suspend TestScope.(SyncStore) -> Unit) =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                block(store)
            } finally {
                store.close()
            }
        }

    private fun server(verified: Boolean = true) =
        SessionAccount(
            syncAccount,
            "Remote",
            "demo@example.com",
            verified,
            AccountPolicies("new", "1"),
            false,
        )

    private suspend fun queueAll(store: SyncStore) {
        val repository = AccountRepository(store.database)
        val account = requireNotNull(repository.account())
        repository.upsert(account.copy(syncedAt = "preserve"))
        store.enqueue(OutboxChange.Profile(ProfileChange("Edited", "demo@example.com")))
        store.enqueue(OutboxChange.Walkthrough, 1)
        val policies = ApiJson.response.encodeToString(AccountPolicies.serializer(), AccountPolicies("new", "1"))
        SettingsRepository(store.database).upsert(SettingsRecord("policies", policies))
        store.enqueue(OutboxChange.Policies("new"), 2)
        store.enqueue(
            OutboxChange.Settings(
                SettingsSection.Preferences,
                JsonObject(mapOf("time_format" to JsonPrimitive("12"))),
            ),
            3,
        )
    }

    private suspend fun assertProtected(store: SyncStore) {
        val account = AccountRepository(store.database).account()
        assertEquals("Edited", account?.name)
        assertEquals("demo@example.com", account?.email)
        assertEquals(true, account?.walkthroughSeen)
        assertEquals("preserve", account?.syncedAt)
        val rows = OutboxRepository(store.database).pending()
        assertEquals(4, rows.size)
        val accepted = rows.first { OutboxPayload.change(it) is OutboxChange.Policies }
        val settings =
            SettingsRepository(store.database).settings().associate {
                it.section to
                    ApiJson.response.parseToJsonElement(it.value)
            }
        val policies = settings["policies"] as JsonObject
        assertEquals(JsonPrimitive("new"), policies["acceptedTermsVersion"])
        assertEquals(JsonPrimitive(accepted.createdAt), policies["acceptedTermsAt"])
        assertEquals(JsonPrimitive("12"), (settings["preferences"] as JsonObject)["timeFormat"])
    }

    @Test fun sameAccountLoginKeepsQueuedEdits() =
        loginTest { store ->
            queueAll(store)
            val trips = TripRepository(store.database).trips()
            val rows = OutboxRepository(store.database).pending()
            store.database.upsertSessionAccount(server())
            assertProtected(store)
            assertEquals(true, AccountRepository(store.database).account()?.hasVerifiedEmail)
            assertEquals(trips, TripRepository(store.database).trips())
            assertEquals(rows, OutboxRepository(store.database).pending())
        }

    @Test fun loginWithoutPendingRowsTakesServerValues() =
        loginTest { store ->
            val repository = AccountRepository(store.database)
            repository.upsert(requireNotNull(repository.account()).copy(walkthroughSeen = true))
            store.database.upsertSessionAccount(server())
            val account = repository.account()
            assertEquals("Remote", account?.name)
            assertEquals(false, account?.walkthroughSeen)
            val policies = SettingsRepository(store.database).settings().first { it.section == "policies" }
            val value = ApiJson.response.parseToJsonElement(policies.value) as JsonObject
            assertEquals(JsonPrimitive("new"), value["termsVersion"])
            assertFalse("acceptedTermsVersion" in value)
            assertTrue(OutboxRepository(store.database).pending().isEmpty())
        }

    @Test fun flushedEditsNoLongerProtect() =
        loginTest { store ->
            queueAll(store)
            store.database.upsertSessionAccount(server())
            assertProtected(store)
            val repository = OutboxRepository(store.database)
            repository.pending().forEach { repository.remove(it.id) }
            store.database.upsertSessionAccount(server())
            val account = AccountRepository(store.database).account()
            assertEquals("Remote", account?.name)
            assertEquals(false, account?.walkthroughSeen)
            val policies = SettingsRepository(store.database).settings().first { it.section == "policies" }
            val value = ApiJson.response.parseToJsonElement(policies.value) as JsonObject
            assertFalse("acceptedTermsVersion" in value)
        }

    @Test fun loginUpsertRejectsAnotherAccount() =
        loginTest { store ->
            queueAll(store)
            val before = AccountRepository(store.database).account()
            val settings = SettingsRepository(store.database).settings()
            val rows = OutboxRepository(store.database).pending()
            try {
                store.database.upsertSessionAccount(server().copy(id = syncOther))
                error("expected account mismatch")
            } catch (
                _: StoreFailure.InvalidAccountID,
            ) {
            }
            assertEquals(before, AccountRepository(store.database).account())
            assertEquals(settings, SettingsRepository(store.database).settings())
            assertEquals(rows, OutboxRepository(store.database).pending())
        }

    @Test fun unverifiedLoginUpdatesTheFlagOnly() =
        loginTest { store ->
            queueAll(store)
            store.database.upsertSessionAccount(server(verified = false))
            assertEquals(false, AccountRepository(store.database).account()?.hasVerifiedEmail)
            assertProtected(store)
        }
}
