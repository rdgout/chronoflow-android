@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.sync.session

import app.cash.turbine.test
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.testing.sync.eventually
import net.chronoflow.core.testing.sync.syncAccount
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class StoredDataReadinessTests {
    private class Harness(
        scope: TestScope,
        val values: ReadinessValues = ReadinessValues(),
    ) {
        val credentials = MemorySessionStore(StoredSession("test-token", syncAccount).encode())
        val controller =
            SessionController(
                credentials,
                RecordingAccountStore(),
                SessionTeardown(sleep = {
                    awaitCancellation()
                }),
                StandardTestDispatcher(scope.testScheduler),
            )
        val readiness = StoredDataReadiness(controller, values, StandardTestDispatcher(scope.testScheduler))

        fun close() {
            readiness.close()
            controller.close()
        }
    }

    @Test fun cachedOfflineAndFreshMarkerStreamNeverFakesSyncedAt() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this, ReadinessValues(stored = true))
            try {
                assertEquals(SessionPresence.SignedIn(syncAccount), h.readiness.session())
                assertEquals(Gate.App, h.readiness.gate())
                assertTrue(h.readiness.hasStoredData())
                h.values.set(stored = false)
                h.readiness.storedDataUpdates().test {
                    assertEquals(false, awaitItem())
                    h.readiness.allowFirstLoad()
                    assertEquals(h.controller.epoch, h.readiness.firstLoadEpoch)
                    assertFalse(h.readiness.hasStoredData())
                    h.values.set(stored = true)
                    assertEquals(true, awaitItem())
                }
            } finally {
                h.close()
            }
        }

    @Test fun protectedCredentialFailureStaysUnknownAndRetriesOnActivation() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            try {
                val original = h.credentials.data
                h.credentials.readFails = true
                val task = backgroundScope.async { h.readiness.session() }
                assertTrue(eventually { h.readiness.failure != null })
                assertEquals(SessionFailure.CredentialsUnavailable, h.readiness.failure)
                assertNull(h.controller.presence)
                assertArrayEquals(original, h.credentials.data)
                h.credentials.readFails = false
                h.readiness.retry()
                assertEquals(SessionPresence.SignedIn(syncAccount), task.await())
                assertNull(h.readiness.failure)
                assertEquals(SessionPresence.SignedIn(syncAccount), h.readiness.session())
                assertEquals(2, h.credentials.reads)
            } finally {
                h.close()
            }
        }

    @Test fun cancellationOfUnknownSessionPreservesCredentialsAndStartsNoObservation() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            try {
                h.credentials.readFails = true
                val task = backgroundScope.async { h.readiness.session() }
                assertTrue(eventually { h.readiness.failure != null })
                task.cancel()
                task.join()
                assertNull(h.controller.presence)
                assertEquals(0, h.credentials.deletes)
                assertEquals(0, h.values.reads)
            } finally {
                h.close()
            }
        }

    @Test fun unavailableObservationWaitsForRetryAndEmptyReadIsFalse() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this)
            try {
                h.values.fails = true
                h.readiness.session()
                val read = backgroundScope.async { h.readiness.hasStoredData() }
                assertTrue(eventually { h.readiness.failure != null })
                assertEquals(SessionFailure.DatabaseUnavailable, h.readiness.failure)
                h.values.fails = false
                h.readiness.retry()
                assertFalse(read.await())
                h.values.state.value = null
                assertFalse(h.readiness.hasStoredData())
            } finally {
                h.close()
            }
        }

    @Test fun committedVerificationRoutingSurvivesRequestCancellationAndRestore() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this, ReadinessValues(Gate.Verify))
            val epochs = mutableListOf<Long>()
            try {
                h.controller.restore()
                val first = h.controller.activeSession()!!
                val request =
                    backgroundScope.launch {
                        currentCoroutineContext().cancel()
                        h.readiness.routeAcceptedSession({ error("expected verification") }, {
                            epochs += it
                            assertTrue(currentCoroutineContext().isActive)
                        })
                    }
                request.join()
                runCurrent()
                assertEquals(listOf(first.epoch), epochs)
                h.controller.restore()
                val second = h.controller.activeSession()!!
                val again =
                    backgroundScope.launch {
                        currentCoroutineContext().cancel()
                        h.readiness.routeAcceptedSession({ error("expected verification") }, { epochs += it })
                    }
                again.join()
                runCurrent()
                assertEquals(listOf(first.epoch, second.epoch), epochs)
            } finally {
                h.close()
            }
        }

    @Test fun missingPoliciesWaitUntilLoadedOrExplicitFirstLoadAllowance() =
        runTest(timeout = 60.seconds) {
            val h = Harness(this, ReadinessValues(null))
            try {
                h.readiness.session()
                h.readiness.gateUpdates().test {
                    runCurrent()
                    expectNoEvents()
                    h.values.set(Gate.Terms, true)
                    assertEquals(Gate.Terms, awaitItem())
                    h.values.set(null, false)
                    runCurrent()
                    expectNoEvents()
                }
                h.readiness.gateUpdates().test {
                    h.readiness.allowFirstLoad()
                    assertEquals(Gate.App, awaitItem())
                    h.values.set(Gate.Terms, true)
                    assertEquals(Gate.Terms, awaitItem())
                }
            } finally {
                h.close()
            }
        }
}
