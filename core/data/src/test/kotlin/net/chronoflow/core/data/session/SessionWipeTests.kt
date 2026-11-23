@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.data.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.database.store.saveAccount
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.session.SignOutResult
import net.chronoflow.core.model.session.StorageNamespace
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.sync.SyncLifecycle
import net.chronoflow.core.sync.SyncScheduler
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.sync.session.sessionResult
import net.chronoflow.core.testing.sync.ManualSyncScheduler
import net.chronoflow.core.testing.sync.ScriptedSource
import net.chronoflow.core.testing.sync.TestGate
import net.chronoflow.core.testing.sync.eventually
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SessionWipeTests {
    @Test fun forcedWipeDrainsLateResponseBeforeCloseAndCannotRecreateFiles() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            val store = SessionStoreFixture(io)
            val source = ScriptedSource()
            val clock = ManualSyncScheduler()
            var lifecycle: SyncLifecycle? = null
            var stopping = false
            val wiper = AccountWiper(store.root, StorageNamespace.Mock, store.credentials, io = io)
            val controller =
                SessionController(
                    store.credentials,
                    store.accounts,
                    SessionTeardown(
                        sleep = {},
                        beginWipe = wiper::begin,
                        erase = wiper::erase,
                        finishWipe = wiper::finish,
                        logout = { fail("forced 401 called logout") },
                    ),
                    io,
                )
            controller.attach(stopSync = {
                stopping = lifecycle != null
                lifecycle?.stop()
                lifecycle = null
            }, requestSync = { epoch ->
                lifecycle =
                    SyncLifecycle(
                        store.accounts.activeDatabase()!!,
                        source,
                        SyncEpoch(AccountID.of(syncAccount), epoch),
                        SyncScheduler({ syncDate }, clock::sleep),
                        dispatcher = io,
                    )
                lifecycle!!.start()
            })
            try {
                controller.accept(sessionResult())
                val database = store.accounts.activeAccountDatabase()!!
                val observer = backgroundScope.launch { AccountRepository(database).values().collect {} }
                val gate = TestGate()
                source.configure(hook = { if (it == "identity") gate.block() })
                val refresh =
                    backgroundScope.async {
                        try {
                            lifecycle!!.refreshNow()
                            fail("expected cancelled refresh")
                        } catch (
                            _: CancellationException,
                        ) {
                        }
                    }
                gate.waitUntilBlocked()
                val wipe = backgroundScope.async { controller.signOut(true, true) }
                assertTrue(eventually { stopping })
                gate.release()
                assertEquals(SignOutResult.SignedOut, wipe.await())
                refresh.await()
                observer.join()
                assertNull(store.credentials.data)
                val directory = database.directory(AccountID.of(syncAccount))
                withContext(io) { assertFalse(directory.exists()) }
                try {
                    database.write { it.saveAccount(AccountRecord(syncAccount, "late", "late")) }
                    fail("expected closed store")
                } catch (
                    failure: StoreFailure,
                ) {
                    assertEquals(StoreFailure.Closed, failure)
                }
                withContext(io) { assertFalse(directory.exists()) }
            } finally {
                lifecycle?.stop()
                controller.close()
                store.close()
            }
        }
}
