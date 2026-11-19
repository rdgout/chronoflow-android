@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.data.session

import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.repository.AccountModel
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.database.testing.JdbcAccountDriverFactory
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.model.session.readStoredSession
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.sync.session.sessionResult
import net.chronoflow.core.testing.sync.TestGate
import net.chronoflow.core.testing.sync.eventually
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncOther
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SessionAccountDatabaseTests {
    @Test fun verificationPreservesStoredFieldsAndRejectsWrongEmailOrSupersededSession() =
        runTest(timeout = 60.seconds) {
            val store = SessionStoreFixture(StandardTestDispatcher(testScheduler))
            try {
                val account =
                    SessionAccount(syncAccount, "Name", "a@example.com", false, AccountPolicies("1", "1"), false)
                store.controller.accept(SessionResult("fixture", account))
                val snapshot = store.controller.activeSession()!!
                assertFalse(store.controller.confirmEmailVerification(snapshot, "changed@example.com"))
                assertTrue(store.controller.confirmEmailVerification(snapshot, account.email))
                val record = AccountRepository(store.accounts.activeAccountDatabase()!!).account()!!
                assertTrue(record.hasVerifiedEmail)
                assertEquals(account.name, record.name)
                assertFalse(record.walkthroughSeen)
                assertEquals("fixture", store.credentials.readStoredSession()?.token)
                store.controller.signOut(true)
                assertFalse(store.controller.confirmEmailVerification(snapshot, account.email))
                store.controller.accept(
                    SessionResult("replacement", account.copy(id = syncOther, name = "B", email = "b@example.com")),
                )
                store.controller.signOut(snapshot, true)
                assertEquals(
                    "replacement",
                    store.controller
                        .activeSession()
                        ?.session
                        ?.token,
                )
                store.controller.signOut(true)
            } finally {
                store.close()
            }
        }

    @Test fun closeRejectsAnAttachmentResolvedBeforeDetachAndHidesTheClosingDatabase() =
        runTest(timeout = 60.seconds) {
            val store = SessionStoreFixture(StandardTestDispatcher(testScheduler))
            val model = AccountModel(backgroundScope)
            val gate = TestGate()
            var armed = false
            val accounts =
                SessionAccountDatabase(
                    { store.root },
                    StorageNamespace.Mock,
                    JdbcAccountDriverFactory(),
                    store.io,
                    detach = {
                        model.detach()
                        if (armed) {
                            armed = false
                            gate.block()
                        }
                    },
                )
            try {
                accounts.open(AccountID.of(syncAccount))
                val first = accounts.activeAccountDatabase()!!
                val repository = AccountRepository(first)
                val a = AccountRecord(syncAccount, "A", "a")
                repository.upsert(a)
                model.attach(repository, 1)
                assertTrue(eventually { model.state.value == AccountModel.LoadState.Loaded(a) })
                val token = model.attachmentToken
                armed = true
                val close = backgroundScope.async { accounts.close() }
                gate.waitUntilBlocked()
                assertEquals(AccountModel.LoadState.Unknown, model.state.value)
                assertNull(accounts.activeAccountDatabase())
                assertEquals(syncAccount, first.accountID?.rawValue)
                model.attach(repository, 99, token)
                assertEquals(1L, model.epoch)
                assertEquals(AccountModel.LoadState.Unknown, model.state.value)
                assertEquals(0, first.observationCount)
                close.cancel()
                gate.release()
                close.join()
                accounts.open(AccountID.of(syncOther))
                val second = accounts.activeAccountDatabase()!!
                val b = AccountRecord(syncOther, "B", "b")
                AccountRepository(second).upsert(b)
                model.attach(AccountRepository(second), 2, model.attachmentToken)
                assertTrue(eventually { model.state.value == AccountModel.LoadState.Loaded(b) })
                model.attach(repository, 99, token)
                assertEquals(2L, model.epoch)
                assertEquals(AccountModel.LoadState.Loaded(b), model.state.value)
                accounts.close()
                assertEquals(AccountModel.LoadState.Unknown, model.state.value)
            } finally {
                accounts.close()
                store.close()
            }
        }

    private fun failedAcceptance(nextID: String) =
        runTest(timeout = 60.seconds) {
            val store = SessionStoreFixture(StandardTestDispatcher(testScheduler))
            try {
                store.controller.accept(SessionResult("original", sessionResult().account))
                val first = store.accounts.activeAccountDatabase()!!
                val published = store.controller.updates.value!!
                store.credentials.writeFails = true
                try {
                    store.controller.accept(
                        SessionResult(
                            "replacement",
                            sessionResult(nextID).account.copy(name = "New", email = "new@example.test"),
                        ),
                    )
                    fail("expected credential write failure")
                } catch (
                    failure: SessionFailure,
                ) {
                    assertEquals(SessionFailure.CredentialWrite, failure)
                }
                val persisted = store.credentials.readStoredSession()
                assertEquals(StoredSession("original", syncAccount), persisted)
                val active = store.accounts.activeAccountDatabase()!!
                assertEquals(nextID, AccountRepository(active).account()?.id)
                if (nextID == syncAccount) {
                    assertSame(first, active)
                    assertEquals(published.presence, store.controller.presence)
                    assertEquals(published.epoch, store.controller.epoch)
                    assertEquals(persisted, store.controller.activeSession()?.session)
                } else {
                    try {
                        AccountRepository(first).account()
                        fail("expected closed database")
                    } catch (
                        failure: StoreFailure,
                    ) {
                        assertEquals(StoreFailure.Closed, failure)
                    }
                    assertNull(store.controller.activeSession())
                    assertEquals(SessionPresence.SignedOut, store.controller.presence)
                    assertEquals(
                        published.epoch + 1,
                        store.controller.updates.value!!
                            .epoch,
                    )
                    assertEquals(
                        SessionPresence.SignedOut,
                        store.controller.updates.value!!
                            .presence,
                    )
                }
            } finally {
                store.close()
            }
        }

    @Test fun failedAcceptanceAfterSignInMatchesThePublishedStateAndDatabaseSameAccount() =
        failedAcceptance(
            syncAccount,
        )

    @Test fun failedAcceptanceAfterSignInMatchesThePublishedStateAndDatabaseOtherAccount() = failedAcceptance(syncOther)

    private fun isolation(namespace: StorageNamespace) =
        runTest(timeout = 60.seconds) {
            val store = SessionStoreFixture(StandardTestDispatcher(testScheduler))
            val accounts = SessionAccountDatabase({ store.root }, namespace, JdbcAccountDriverFactory(), store.io)
            val controller = SessionController(store.credentials, accounts, SessionTeardown(sleep = {}), store.io)
            try {
                val policies = AccountPolicies("1", "2")
                val a = SessionAccount(syncAccount, "A", "a@example.test", false, policies, false)
                controller.accept(SessionResult("test", a))
                val first = accounts.activeAccountDatabase()!!
                val repository = AccountRepository(first)
                assertNull(repository.account()?.syncedAt)
                repository.upsert(repository.account()!!.copy(syncedAt = "marker"))
                controller.accept(SessionResult("new", a))
                assertEquals("marker", repository.account()?.syncedAt)
                assertEquals(
                    policies,
                    ApiJson.response.decodeFromString(
                        AccountPolicies.serializer(),
                        SettingsRepository(first)
                            .settings()
                            .first {
                                it.section ==
                                    "policies"
                            }.value,
                    ),
                )
                controller.accept(
                    SessionResult(
                        "other",
                        a.copy(
                            id = syncOther,
                            name = "B",
                            email = "b@example.test",
                            hasVerifiedEmail = true,
                            walkthroughSeen = true,
                        ),
                    ),
                )
                val active = accounts.activeAccountDatabase()!!
                assertEquals(syncOther, AccountRepository(active).account()?.id)
                assertNull(AccountRepository(active).account()?.syncedAt)
                try {
                    repository.account()
                    fail("expected closed database")
                } catch (
                    failure: StoreFailure,
                ) {
                    assertEquals(StoreFailure.Closed, failure)
                }
                try {
                    accounts.upsert(a)
                    fail("expected invalid account")
                } catch (
                    failure: StoreFailure,
                ) {
                    assertEquals(StoreFailure.InvalidAccountID, failure)
                }
                accounts.close()
                assertEquals(SessionPresence.SignedIn(syncOther), controller.restore())
            } finally {
                controller.close()
                accounts.close()
                store.close()
            }
        }

    @Test fun acceptancePreservesSyncedMarkerAndAccountAndSourceIsolationMock() = isolation(StorageNamespace.Mock)

    @Test fun acceptancePreservesSyncedMarkerAndAccountAndSourceIsolationApi() = isolation(StorageNamespace.Api)

    /** Android adapter follows the concrete account database throughout its lifetime. */
    @Test fun activeSyncDatabaseFollowsTheOpenAccount() =
        runTest(timeout = 60.seconds) {
            val store = SessionStoreFixture(StandardTestDispatcher(testScheduler))
            try {
                assertNull(store.accounts.activeDatabase())
                store.accounts.open(AccountID.of(syncAccount))
                assertNotNull(store.accounts.activeDatabase())
                store.accounts.close()
                assertNull(store.accounts.activeDatabase())
            } finally {
                store.close()
            }
        }
}
