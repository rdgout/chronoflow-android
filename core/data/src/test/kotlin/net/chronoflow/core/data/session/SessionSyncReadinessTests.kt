@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.data.session

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.sync.SessionSync
import net.chronoflow.core.sync.SyncScheduler
import net.chronoflow.core.sync.session.AccountReadinessSource
import net.chronoflow.core.sync.session.StoredDataReadiness
import net.chronoflow.core.testing.sync.ManualSyncScheduler
import net.chronoflow.core.testing.sync.ScriptedSource
import net.chronoflow.core.testing.sync.TestGate
import net.chronoflow.core.testing.sync.card
import net.chronoflow.core.testing.sync.eventually
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncDate
import net.chronoflow.core.testing.sync.syncTrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SessionSyncReadinessTests {
    @Test fun routedTripUsesTheSessionStoreAndKeepsItsPlanAcrossSyncRestarts() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            val store = SessionStoreFixture(io)
            val source = ScriptedSource().apply { configure(trips = listOf(card(keep = false))) }
            val clock = ManualSyncScheduler()
            val session = StoredSession("test", syncAccount)
            val sync =
                SessionSync(
                    store.accounts,
                    SyncScheduler({ syncDate }, clock::sleep),
                    source = { source },
                    isCurrent = { s, e -> s == session && e == 1L },
                    unauthorized = {},
                    verification = {},
                    dispatcher = io,
                )
            try {
                store.accounts.open(
                    net.chronoflow.core.model.AccountID
                        .of(syncAccount),
                )
                val database = store.accounts.activeAccountDatabase()!!
                sync.start(session, 1)
                sync.refreshNow()
                val repository = TripRepository(database)
                assertNull(repository.plan(syncTrip, "hash"))
                val lease = sync.retention.hold(syncTrip)
                sync.fetchDetail(syncTrip, session, 1)
                assertNotNull(repository.plan(syncTrip, "hash"))
                sync.setEnabled(false)
                sync.setEnabled(true)
                sync.resume(session, 1)
                sync.refreshNow()
                assertNotNull(repository.plan(syncTrip, "hash"))
                try {
                    sync.fetchDetail(syncTrip, session, 2)
                    fail("expected stale epoch cancellation")
                } catch (
                    _: CancellationException,
                ) {
                }
                lease.release()
                sync.refreshNow()
                assertNull(repository.plan(syncTrip, "hash"))
            } finally {
                sync.stop()
                store.close()
            }
        }

    @Test fun storedSessionResolvesWhileProductionSyncSourceStartupIsSuspended() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            val store = SessionStoreFixture(io)
            val source = ScriptedSource()
            val startup = TestGate()
            val clock = ManualSyncScheduler()
            val sync =
                SessionSync(
                    store.accounts,
                    SyncScheduler({ syncDate }, clock::sleep),
                    source = {
                        startup.block()
                        source
                    },
                    unauthorized = {},
                    verification = {},
                    dispatcher = io,
                )
            store.controller.attach(stopSync = sync::stop, requestSync = { sync.requestStart(store.controller, it) })
            store.credentials.data = StoredSession("test", syncAccount).encode()
            val readiness = StoredDataReadiness(store.controller, AccountReadinessSource { emptyFlow() }, io)
            var resolved: SessionPresence? = null
            try {
                val session = backgroundScope.async { readiness.session().also { resolved = it } }
                startup.waitUntilBlocked()
                assertTrue(eventually { resolved != null })
                assertEquals(SessionPresence.SignedIn(syncAccount), resolved)
                assertEquals(emptyList<String>(), source.calls)
                startup.release()
                assertEquals(SessionPresence.SignedIn(syncAccount), session.await())
            } finally {
                readiness.close()
                sync.stop()
                store.close()
            }
        }
}
