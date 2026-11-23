@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.data.session

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.session.SignOutResult
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncOther
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import kotlin.time.Duration.Companion.seconds

class AccountWiperTests {
    private class MemoryGuestSecrets : GuestSecrets {
        private val values = mutableMapOf<String, String>()

        fun set(
            key: String,
            value: String,
        ) {
            values[key] = value
        }

        fun get(key: String) = values[key]

        override suspend fun deleteAll() {
            values.clear()
        }
    }

    private fun member(unauthorized: Boolean) =
        runTest(timeout = 60.seconds) {
            val store = SessionStoreFixture(StandardTestDispatcher(testScheduler))
            try {
                val secrets =
                    MemoryGuestSecrets().apply {
                        set("first", "first-secret")
                        set("second", "second-secret")
                    }
                val recovery = File(store.root, "ChronoFlowMock/guest-wizard.json")
                val preferences = File(store.root, "device-preferences.json")
                withContext(store.io) {
                    checkNotNull(recovery.parentFile).mkdirs()
                    recovery.writeText("guest recovery")
                    preferences.writeText("device preferences")
                }
                store.credentials.data = StoredSession("member-token", syncAccount).encode()
                val wiper = AccountWiper(store.root, StorageNamespace.Mock, store.credentials, secrets, store.io)
                val controller =
                    SessionController(
                        store.credentials,
                        store.accounts,
                        SessionTeardown(
                            sleep = { awaitCancellation() },
                            prepare = wiper::recover,
                            beginWipe = wiper::begin,
                            erase = wiper::erase,
                            finishWipe = wiper::finish,
                        ),
                        store.io,
                    )
                assertEquals(SignOutResult.SignedOut, controller.signOut(false, unauthorized))
                assertNull(secrets.get("first"))
                assertNull(secrets.get("second"))
                withContext(store.io) {
                    assertFalse(recovery.exists())
                    assertTrue(preferences.exists())
                }
                assertNull(store.credentials.data)
                controller.close()
            } finally {
                store.close()
            }
        }

    @Test fun memberSignOutClearsGuestSecretsAndRecoveryVoluntary() = member(false)

    @Test fun memberSignOutClearsGuestSecretsAndRecoveryUnauthorized() = member(true)

    @Test fun pendingWipeRetriesGuestRecoveryDeletion() =
        runTest(timeout = 60.seconds) {
            val store = SessionStoreFixture(StandardTestDispatcher(testScheduler))
            try {
                val recovery = File(store.root, "ChronoFlowMock/guest-wizard.json")
                withContext(store.io) {
                    checkNotNull(recovery.parentFile).mkdirs()
                    recovery.writeText("guest recovery")
                }
                val secrets = MemoryGuestSecrets().apply { set("token", "secret") }
                store.credentials.data = "test-session".encodeToByteArray()
                val wiper =
                    AccountWiper(
                        store.root,
                        StorageNamespace.Mock,
                        store.credentials,
                        secrets,
                        store.io,
                        remove = { if (it == recovery) throw StoreFailure.Unavailable() },
                    )
                val id = AccountID.of(syncAccount)
                wiper.begin(id)
                unavailable { wiper.erase(id) }
                withContext(store.io) { assertTrue(recovery.exists()) }
                AccountWiper(store.root, StorageNamespace.Mock, store.credentials, secrets, store.io).recover()
                assertNull(secrets.get("token"))
                withContext(store.io) {
                    assertFalse(recovery.exists())
                    assertFalse(File(store.root, "ChronoFlowMock/pending-wipe.json").exists())
                }
                assertNull(store.credentials.data)
            } finally {
                store.close()
            }
        }

    @Test fun markerSurvivesPartialFailureAndRestartRemovesDirectoryAndCredentials() =
        runTest(timeout = 60.seconds) {
            val store = SessionStoreFixture(StandardTestDispatcher(testScheduler))
            try {
                val database = AccountDatabase(store.root, StorageNamespace.Mock, JdbcAccountDriverFactory(), store.io)
                database.open(syncAccount)
                val id = AccountID.of(syncAccount)
                val directory = database.directory(id)
                withContext(store.io) {
                    // JDBC closes idle connections; retain a reader while checking real WAL sidecars.
                    java.sql.DriverManager.getConnection("jdbc:sqlite:${database.file(id).absolutePath}").use { reader ->
                        reader.createStatement().use { statement ->
                            statement.executeQuery("SELECT count(*) FROM account").use { assertTrue(it.next()) }
                        }
                        AccountRepository(database).upsert(AccountRecord(syncAccount, "Old", "old@example.test"))
                        val sidecars = directory.list()!!.toList()
                        assertTrue(sidecars.any { it.endsWith("-wal") })
                        assertTrue(sidecars.any { it.endsWith("-shm") })
                    }
                }
                store.credentials.data = "test-session".encodeToByteArray()
                val wiper =
                    AccountWiper(store.root, StorageNamespace.Mock, store.credentials, io = store.io, remove = {
                        throw StoreFailure.Unavailable()
                    })
                wiper.begin(AccountID.of(syncAccount))
                database.close()
                unavailable { wiper.erase(AccountID.of(syncAccount)) }
                val marker = File(store.root, "ChronoFlowMock/pending-wipe.json")
                withContext(store.io) { assertTrue(marker.exists()) }
                val restarted = AccountWiper(store.root, StorageNamespace.Mock, store.credentials, io = store.io)
                restarted.recover()
                withContext(store.io) {
                    assertFalse(directory.exists())
                    assertFalse(marker.exists())
                }
                assertNull(store.credentials.data)
                restarted.recover()
                val next = AccountDatabase(store.root, StorageNamespace.Mock, JdbcAccountDriverFactory(), store.io)
                next.open(syncOther)
                assertNull(AccountRepository(next).account())
                assertTrue(OutboxRepository(next).pending().isEmpty())
                next.close()
            } finally {
                store.close()
            }
        }

    @Test fun corruptMarkerBlocksRecoveryWithoutTouchingAnotherNamespace() =
        runTest(timeout = 60.seconds) {
            val store = SessionStoreFixture(StandardTestDispatcher(testScheduler))
            try {
                store.credentials.data = "test-session".encodeToByteArray()
                val wiper = AccountWiper(store.root, StorageNamespace.Api, store.credentials, io = store.io)
                wiper.begin(AccountID.of(syncAccount))
                val marker = File(store.root, "ChronoFlow/pending-wipe.json")
                withContext(store.io) { marker.writeText("\"../outside\"") }
                try {
                    wiper.recover()
                    fail("expected corrupt marker")
                } catch (
                    failure: SessionFailure,
                ) {
                    assertEquals(SessionFailure.DatabaseUnavailable, failure)
                }
                assertNotNull(store.credentials.data)
                withContext(store.io) { assertTrue(marker.exists()) }
                AccountWiper(store.root, StorageNamespace.Mock, store.credentials, io = store.io).recover()
                assertNotNull(store.credentials.data)
            } finally {
                store.close()
            }
        }
}

private suspend fun unavailable(body: suspend () -> Unit) {
    try {
        body()
        fail("expected unavailable store")
    } catch (_: StoreFailure.Unavailable) {
    }
}
