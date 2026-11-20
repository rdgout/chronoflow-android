@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.wizard.model

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.testing.wizard.InMemoryWizardDraftStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class WizardRecoveryWriterTests {
    private fun draft(revision: Int) =
        MemberWizardDraft(state = MemberWizardState(instance = "instance", revision = revision))

    @Test fun burstOfPersistsWritesLatestRevisionLast() =
        runTest(timeout = 60.seconds) {
            val store = InMemoryWizardDraftStore()
            val gate = CompletableDeferred<Unit>()
            var active = 0
            var maximum = 0
            store.beforePersist = {
                active++
                maximum = maxOf(maximum, active)
                try {
                    if (it.state.revision == 1) gate.await()
                } finally {
                    active--
                }
            }
            val writer = WizardRecoveryWriter(store, backgroundScope) { error("Unexpected persistence failure") }
            writer.enqueue(draft(1))
            runCurrent()
            writer.enqueue(draft(2))
            writer.enqueue(draft(3))
            writer.enqueue(draft(4))
            gate.complete(Unit)
            writer.flush()
            assertEquals(listOf(1, 4), store.persisted.map { it.state.revision })
            assertEquals(1, maximum)
        }

    @Test fun flushWaitsForQueuedWrite() =
        runTest(timeout = 60.seconds) {
            val store = InMemoryWizardDraftStore()
            val gate = CompletableDeferred<Unit>()
            store.beforePersist = { gate.await() }
            val writer = WizardRecoveryWriter(store, backgroundScope) { error("Unexpected persistence failure") }
            writer.enqueue(draft(1))
            var flushed = false
            val flush =
                backgroundScope.launch {
                    writer.flush()
                    flushed = true
                }
            runCurrent()
            assertFalse(flushed)
            gate.complete(Unit)
            runCurrent()
            flush.join()
            assertTrue(flushed)
            assertEquals(1, store.last?.state?.revision)
        }

    @Test fun rejectedWriteIsSilent() =
        runTest(timeout = 60.seconds) {
            val store = InMemoryWizardDraftStore()
            store.failure = RecoveryRejected()
            var failures = 0
            val writer = WizardRecoveryWriter(store, backgroundScope) { failures++ }
            writer.enqueue(draft(1))
            writer.flush()
            assertEquals(0, failures)
            assertTrue(store.persisted.isEmpty())
        }

    @Test fun foreignCancellationIsSilentAndKeepsWriting() =
        runTest(timeout = 60.seconds) {
            val store = InMemoryWizardDraftStore()
            store.failure = StaleSessionException()
            var failures = 0
            val writer = WizardRecoveryWriter(store, backgroundScope) { failures++ }
            writer.enqueue(draft(1))
            writer.flush()
            assertEquals(0, failures)
            store.failure = null
            writer.enqueue(draft(2))
            writer.flush()
            assertEquals(0, failures)
            assertEquals(2, store.last?.state?.revision)
        }

    @Test fun storeFailureReportsPersistenceFailed() =
        runTest(timeout = 60.seconds) {
            val store = InMemoryWizardDraftStore()
            store.failure = StoreFailure.Unavailable()
            var failed = false
            val writer = WizardRecoveryWriter(store, backgroundScope) { failed = true }
            writer.enqueue(draft(1))
            writer.flush()
            assertTrue(failed)
        }

    @Test fun closeDrainsPendingLatestAndIgnoresLaterEnqueues() =
        runTest(timeout = 60.seconds) {
            val owner = SupervisorJob(backgroundScope.coroutineContext.job)
            val scope = CoroutineScope(backgroundScope.coroutineContext + owner)
            val store = InMemoryWizardDraftStore()
            val gate = CompletableDeferred<Unit>()
            store.beforePersist = { gate.await() }
            val writer = WizardRecoveryWriter(store, scope) { error("Unexpected persistence failure") }
            try {
                writer.enqueue(draft(1))
                runCurrent()
                writer.enqueue(draft(2))
                writer.close()
                writer.close()
                writer.enqueue(draft(3))
                runCurrent()
                assertTrue(store.persisted.isEmpty())
                assertTrue(owner.children.any { it.isActive })
                gate.complete(Unit)
                writer.flush()
                runCurrent()
                assertEquals(listOf(1, 2), store.persisted.map { it.state.revision })
                assertTrue(owner.isActive)
                assertFalse(owner.children.any { it.isActive })
            } finally {
                gate.complete(Unit)
                owner.cancelAndJoin()
            }
        }

    @Test fun flushReturnsWhenWriterScopeIsCancelled() =
        runTest(timeout = 60.seconds) {
            val owner = SupervisorJob(backgroundScope.coroutineContext.job)
            val scope = CoroutineScope(backgroundScope.coroutineContext + owner)
            val store = InMemoryWizardDraftStore()
            val gate = CompletableDeferred<Unit>()
            store.beforePersist = { gate.await() }
            val writer = WizardRecoveryWriter(store, scope) { error("Unexpected persistence failure") }
            try {
                writer.enqueue(draft(1))
                runCurrent()
                owner.cancelAndJoin()
                writer.flush()
                assertTrue(store.persisted.isEmpty())
                assertFalse(gate.isCompleted)
            } finally {
                owner.cancelAndJoin()
            }
        }
}
