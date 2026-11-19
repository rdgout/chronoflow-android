@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app

import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.data.repository.AccountModel
import net.chronoflow.core.sync.session.sessionResult
import net.chronoflow.core.testing.sync.TestGate
import net.chronoflow.core.testing.sync.eventually
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SessionGraphsTest {
    @Test fun oneGraphPerEpochAndNewEpochCancelsThePreviousScope() =
        runTest(timeout = 60.seconds) {
            val f = AppSessionFixture(StandardTestDispatcher(testScheduler), backgroundScope)
            try {
                f.controller.accept(sessionResult())
                assertTrue(
                    eventually {
                        f.graphs.current.value != null &&
                            f.model.state.value is AccountModel.LoadState.Loaded
                    },
                )
                val first = f.graphs.current.value!!
                f.controller.accept(sessionResult())
                assertTrue(
                    eventually {
                        f.graphs.current.value
                            ?.epoch == f.controller.epoch
                    },
                )
                val second = f.graphs.current.value!!
                assertNotSame(first, second)
                assertTrue(first.scope.coroutineContext[Job]!!.isCancelled)
                assertEquals(f.controller.epoch, second.snapshot.epoch)
                f.controller.signOut(true, true)
                assertTrue(eventually { f.graphs.current.value == null })
                assertEquals(AccountModel.LoadState.Unknown, f.model.state.value)
            } finally {
                f.close()
            }
        }

    @Test fun dropClearsTheGraphAndDetachesTheAccountModel() =
        runTest(timeout = 60.seconds) {
            val f = AppSessionFixture(StandardTestDispatcher(testScheduler), backgroundScope)
            try {
                f.controller.accept(sessionResult())
                assertTrue(eventually { f.model.state.value is AccountModel.LoadState.Loaded })
                val graph = f.graphs.current.value!!
                f.graphs.drop()
                assertNull(f.graphs.current.value)
                assertEquals(AccountModel.LoadState.Unknown, f.model.state.value)
                assertTrue(graph.scope.coroutineContext[Job]!!.isCancelled)
            } finally {
                f.close()
            }
        }

    @Test fun anAlreadyStaleUpdateAttachesNothing() =
        runTest(timeout = 60.seconds) {
            val f = AppSessionFixture(StandardTestDispatcher(testScheduler), backgroundScope)
            val write = TestGate()
            try {
                f.graphs.close()
                f.controller.accept(sessionResult())
                f.credentials.writeGate = write
                val replacement = backgroundScope.async { f.controller.accept(sessionResult()) }
                write.waitUntilBlocked()
                f.graphs.start()
                runCurrent()
                assertNull(f.graphs.current.value)
                assertEquals(AccountModel.LoadState.Unknown, f.model.state.value)
                write.release()
                replacement.await()
            } finally {
                f.close()
            }
        }
}
