@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.app

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.repository.AccountModel
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.session.AccountReadinessRepository
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.SessionAccount
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.sync.session.AccountReadinessSource
import net.chronoflow.core.sync.session.SessionAccountStore
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.sync.session.StoredDataReadiness
import net.chronoflow.core.sync.session.sessionResult
import net.chronoflow.core.testing.sync.TestGate
import net.chronoflow.core.testing.sync.eventually
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncOther
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SessionLifecycleTests {
    private fun firstFlush(
        verified: Boolean,
        cached: Boolean,
        expected: Gate,
    ) = runTest(timeout = 60.seconds) {
        val io = StandardTestDispatcher(testScheduler)
        val f = AppSessionFixture(io, backgroundScope)
        val open = TestGate()
        val account =
            SessionAccount(syncAccount, "Name", "a@example.test", verified, AccountPolicies("1", "1", "1"), true)
        f.accounts.open(AccountID.of(syncAccount))
        f.accounts.upsert(account)
        val repository = AccountRepository(f.accounts.activeAccountDatabase()!!)
        if (cached) repository.upsert(repository.account()!!.copy(syncedAt = "2026-11-06T00:00:00Z"))
        f.accounts.close()
        f.credentials.data = StoredSession("test", syncAccount).encode()
        val gatedAccounts =
            object : SessionAccountStore by f.accounts {
                override suspend fun open(id: AccountID) {
                    open.block()
                    f.accounts.open(id)
                }
            }
        val controller = SessionController(f.credentials, gatedAccounts, SessionTeardown(sleep = {}), io)
        val readiness =
            StoredDataReadiness(
                controller,
                AccountReadinessSource { id ->
                    AccountReadinessRepository(f.accounts.activeAccountDatabase()!!).values(id)
                },
                io,
            )
        try {
            val session = backgroundScope.async { readiness.session() }
            open.waitUntilBlocked()
            assertFalse(session.isCompleted)
            open.release()
            assertEquals(SessionPresence.SignedIn(syncAccount), session.await())
            assertEquals(expected, readiness.gate())
            assertEquals(cached, readiness.hasStoredData())
        } finally {
            readiness.close()
            controller.close()
            f.close()
        }
    }

    @Test fun nativeFirstFlushAndSuspendedOpenUseProductionStoredDataReadinessCached() =
        firstFlush(
            true,
            true,
            Gate.App,
        )

    @Test fun nativeFirstFlushAndSuspendedOpenUseProductionStoredDataReadinessFresh() =
        firstFlush(
            true,
            false,
            Gate.App,
        )

    @Test fun nativeFirstFlushAndSuspendedOpenUseProductionStoredDataReadinessVerify() =
        firstFlush(
            false,
            false,
            Gate.Verify,
        )

    @Test fun nativeCompositionObservesAndDetachesAccountsUsingOneKeychainStore() =
        runTest(timeout = 60.seconds) {
            val f = AppSessionFixture(StandardTestDispatcher(testScheduler), backgroundScope)
            try {
                assertEquals(AccountModel.LoadState.Unknown, f.model.state.value)
                f.controller.accept(sessionResult())
                assertTrue(
                    eventually { (f.model.state.value as? AccountModel.LoadState.Loaded)?.account?.id == syncAccount },
                )
                assertEquals(
                    f.controller.epoch,
                    f.graphs.current.value
                        ?.epoch,
                )
                f.controller.signOut(true, true)
                assertEquals(AccountModel.LoadState.Unknown, f.model.state.value)
                assertEquals(null, f.graphs.current.value)
                f.controller.accept(sessionResult(syncOther))
                assertTrue(
                    eventually {
                        (f.model.state.value as? AccountModel.LoadState.Loaded)?.account?.id ==
                            syncOther
                    },
                )
                assertEquals(
                    f.controller.epoch,
                    f.graphs.current.value
                        ?.epoch,
                )
                f.controller.signOut(true, true)
                assertEquals(AccountModel.LoadState.Unknown, f.model.state.value)
                assertEquals(null, f.graphs.current.value)
            } finally {
                f.close()
            }
        }
}
