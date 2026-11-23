package net.chronoflow.core.data.session

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.sync.SessionSync
import net.chronoflow.core.sync.SyncScheduler
import net.chronoflow.core.testing.sync.ManualSyncScheduler
import net.chronoflow.core.testing.sync.ScriptedSource
import net.chronoflow.core.testing.sync.eventually
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.sync.syncDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/** Android requestStart schedules work without making the restore caller wait for it. */
class SessionSyncStartTest {
    @Test fun requestStartIgnoresStaleEpochAndDebouncesTheCurrentEpoch() =
        runTest(timeout = 60.seconds) {
            val io = StandardTestDispatcher(testScheduler)
            val store = SessionStoreFixture(io)
            val source = ScriptedSource()
            val clock = ManualSyncScheduler()
            val sync =
                SessionSync(
                    store.accounts,
                    SyncScheduler({ syncDate }, clock::sleep),
                    source = { source },
                    unauthorized = {},
                    verification = {},
                    dispatcher = io,
                )
            try {
                store.credentials.data = StoredSession("test", syncAccount).encode()
                store.controller.restore()
                sync.requestStart(store.controller, store.controller.epoch - 1)
                assertTrue(source.calls.isEmpty())
                assertEquals(0, clock.pendingCount)
                sync.requestStart(store.controller, store.controller.epoch)
                clock.waitForSleep()
                assertTrue(source.calls.isEmpty())
                assertEquals(listOf(1.0), clock.requestedDelays)
                clock.advance()
                assertTrue(eventually { source.calls.contains("detail") })
                assertEquals("identity", source.calls.first())
                assertEquals(1, source.calls.count { it == "identity" })
            } finally {
                sync.stop()
                store.close()
            }
        }
}
