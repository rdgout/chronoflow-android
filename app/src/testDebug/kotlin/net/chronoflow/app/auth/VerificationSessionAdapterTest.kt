package net.chronoflow.app.auth

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.AppDispatchers
import net.chronoflow.app.AppGraph
import net.chronoflow.app.GraphOverrides
import net.chronoflow.app.SessionTestApplication
import net.chronoflow.app.SoftwareSessionCipher
import net.chronoflow.app.closeAndJoinForTest
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = SessionTestApplication::class)
class VerificationSessionAdapterTest {
    @Test fun confirmingAStaleSessionDoesNotConstructSync() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val graph =
                AppGraph(
                    ApplicationProvider.getApplicationContext(),
                    overrides = GraphOverrides(dispatchers = AppDispatchers(dispatcher, dispatcher, dispatcher)),
                )
            try {
                val sync = syncLazy(graph)
                assertFalse(sync.isInitialized())
                val stale = SessionSnapshot(StoredSession("stale-token", "0199a3c0-0000-7000-8000-000000000000"), 1)
                assertFalse(VerificationSessionAdapter(graph).confirm(stale, "demo@example.com"))
                assertFalse(sync.isInitialized())
            } finally {
                graph.closeAndJoinForTest()
            }
        }

    @Test fun stoppingInitializedSyncClearsItsHeldPlans() =
        runTest(timeout = 60.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val graph =
                AppGraph(
                    ApplicationProvider.getApplicationContext(),
                    overrides =
                        GraphOverrides(
                            sessionCipher = SoftwareSessionCipher(),
                            dispatchers = AppDispatchers(dispatcher, dispatcher, dispatcher),
                        ),
                )
            val sync = graph.sessionSync
            val lease = sync.retention.hold("trip")
            try {
                sync.retention.withHeldPlans { assertEquals(setOf("trip"), it) }
                graph.stopSyncIfStarted()
                sync.retention.withHeldPlans { assertEquals(emptySet<String>(), it) }
            } finally {
                lease.release()
                graph.closeAndJoinForTest()
            }
        }

    private fun syncLazy(graph: AppGraph): Lazy<*> =
        AppGraph::class.java.getDeclaredField("syncLazy").let {
            it.isAccessible = true
            it.get(graph) as Lazy<*>
        }
}
