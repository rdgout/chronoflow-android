@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.core.sync.refresh

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.sync.SyncChanges
import net.chronoflow.core.sync.SyncRefusal
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class RefreshFeedbackTests {
    private fun result(state: SyncState = SyncState.Idle) = SyncResult(state, pendingCount = 0)

    private fun key(result: SyncResult) = outcome(result, true).single().key

    @Test fun statesUseExistingCopy() {
        assertEquals(RefreshFeedbackKey.UpToDate, key(result()))
        assertEquals(RefreshFeedbackKey.NeedsConnection, key(result(SyncState.Offline)))
        assertEquals(RefreshFeedbackKey.SomethingWentWrong, key(result(SyncState.Failed)))
        assertEquals(RefreshFeedbackKey.SomethingWentWrong, key(result(SyncState.Unauthorized)))
        assertEquals(RefreshFeedbackKey.SomethingWentWrong, key(result(SyncState.VerificationRequired)))
    }

    @Test fun addedUpdatedRemovedUseCatalogPlurals() {
        fun rows(count: Int) = outcome(result().copy(changes = SyncChanges(count, count, count, count)), true)
        assertEquals(
            listOf(
                RefreshFeedback(RefreshFeedbackKey.TripsAdded, 1),
                RefreshFeedback(RefreshFeedbackKey.TripsUpdated, 1),
                RefreshFeedback(RefreshFeedbackKey.TripsRemoved, 1),
            ),
            rows(1),
        )
        assertEquals(
            listOf(
                RefreshFeedback(RefreshFeedbackKey.TripsAdded, 3),
                RefreshFeedback(RefreshFeedbackKey.TripsUpdated, 3),
                RefreshFeedback(RefreshFeedbackKey.TripsRemoved, 3),
            ),
            rows(3),
        )
    }

    @Test fun partialSyncDoesNotClaimUpToDate() {
        assertEquals(RefreshFeedbackKey.SomethingWentWrong, key(result().copy(pendingCount = 1)))
        assertEquals(RefreshFeedbackKey.SomethingWentWrong, key(result().copy(retryAfter = 1.0)))
        assertEquals(
            RefreshFeedbackKey.SomethingWentWrong,
            key(result().copy(refusals = listOf(SyncRefusal("id", "check-off", 422, "now", "refused")))),
        )
    }

    @Test fun automaticSyncNeverProducesFeedback() =
        runTest(timeout = 60.seconds) {
            assertTrue(outcome(result(), false).isEmpty())
            assertTrue(outcome(result(SyncState.Offline), false).isEmpty())
            assertTrue(outcome(result(SyncState.Failed), false).isEmpty())
            assertTrue(outcome(result(SyncState.Unauthorized), false).isEmpty())
            assertTrue(outcome(result(SyncState.VerificationRequired), false).isEmpty())
            val model = RefreshFeedbackModel(backgroundScope)
            model.refresh(false) { result() }!!.join()
            assertTrue(
                model.state.value.feedback
                    .isEmpty(),
            )
            model.refresh(false) { throw DataFailure.Offline }!!.join()
            assertTrue(
                model.state.value.feedback
                    .isEmpty(),
            )
        }

    @Test fun refreshWaitsForSyncAndDismissesItsOutcome() =
        runTest(timeout = 60.seconds) {
            val model = RefreshFeedbackModel(backgroundScope)
            val gate = CompletableDeferred<Unit>()
            val job =
                model.refresh {
                    gate.await()
                    result()
                }!!
            assertTrue(model.state.value.refreshing)
            assertTrue(
                model.state.value.feedback
                    .isEmpty(),
            )
            gate.complete(Unit)
            job.join()
            assertFalse(model.state.value.refreshing)
            assertEquals(
                RefreshFeedbackKey.UpToDate,
                model.state.value.feedback
                    .single()
                    .key,
            )
            model.dismiss(model.state.value.presentationId)
            assertTrue(
                model.state.value.feedback
                    .isEmpty(),
            )
        }

    @Test fun anOlderPresentationCannotDismissANewerRefresh() =
        runTest(timeout = 60.seconds) {
            val model = RefreshFeedbackModel(backgroundScope)
            model.refresh { result() }!!.join()
            val previous = model.state.value.presentationId
            model
                .refresh {
                    model.dismiss(previous)
                    result(SyncState.Offline)
                }!!
                .join()
            model.dismiss(previous)
            assertEquals(
                RefreshFeedbackKey.NeedsConnection,
                model.state.value.feedback
                    .single()
                    .key,
            )
            model.dismiss(model.state.value.presentationId)
            assertTrue(
                model.state.value.feedback
                    .isEmpty(),
            )
        }

    @Test fun debugCounterRecordsOnlyAcceptedUserRefreshes() =
        runTest(timeout = 60.seconds) {
            val probe = Probe()
            val model = RefreshFeedbackModel(backgroundScope, probe)
            model.refresh(false) { result() }!!.join()
            assertEquals(0, probe.count)
            val gate = CompletableDeferred<Unit>()
            val job =
                model.refresh {
                    gate.await()
                    result()
                }!!
            assertNull(model.refresh { error("Concurrent refresh") })
            assertEquals(1, probe.count)
            gate.complete(Unit)
            job.join()
            assertEquals("result:idle:1", probe.lastOutcome)
            model.refresh { throw CancellationException() }!!.join()
            assertEquals(2, probe.count)
            assertEquals("cancelled", probe.lastOutcome)
        }

    @Test fun thrownFailuresAndCancellation() =
        runTest(timeout = 60.seconds) {
            val model = RefreshFeedbackModel(backgroundScope)
            model.refresh { throw DataFailure.Offline }!!.join()
            assertEquals(
                RefreshFeedbackKey.NeedsConnection,
                model.state.value.feedback
                    .single()
                    .key,
            )
            model.refresh { throw IllegalStateException() }!!.join()
            assertEquals(
                RefreshFeedbackKey.SomethingWentWrong,
                model.state.value.feedback
                    .single()
                    .key,
            )
            val cancelled = model.refresh { throw CancellationException() }!!
            cancelled.join()
            assertTrue(cancelled.isCancelled)
            assertTrue(
                model.state.value.feedback
                    .isEmpty(),
            )
            assertFalse(model.state.value.refreshing)
            model.refresh { null }!!.join()
            assertTrue(
                model.state.value.feedback
                    .isEmpty(),
            )
        }

    @Test fun finishedSyncIsReportedWhenTheGestureTaskIsCancelled() =
        runTest(timeout = 60.seconds) {
            val model = RefreshFeedbackModel(backgroundScope)
            val caller = launch { model.refresh { result() }!!.join() }
            runCurrent()
            caller.cancel()
            assertEquals(
                RefreshFeedbackKey.UpToDate,
                model.state.value.feedback
                    .single()
                    .key,
            )
        }

    @Test fun syncKeepsRunningWhenTheGestureTaskIsCancelledMidFlight() =
        runTest(timeout = 60.seconds) {
            val model = RefreshFeedbackModel(backgroundScope)
            val gate = CompletableDeferred<Unit>()
            val completed = CompletableDeferred<Unit>()
            var count = 0
            val caller =
                launch {
                    model
                        .refresh {
                            gate.await()
                            count++
                            completed.complete(Unit)
                            result()
                        }!!
                        .join()
                }
            runCurrent()
            caller.cancel()
            gate.complete(Unit)
            completed.await()
            runCurrent()
            assertEquals(1, count)
            assertEquals(
                RefreshFeedbackKey.UpToDate,
                model.state.value.feedback
                    .single()
                    .key,
            )
        }

    private class Probe : RefreshDebugProbe {
        var count = 0
        var lastOutcome = "none"

        override fun accepted() {
            count++
        }

        override fun outcome(value: String) {
            lastOutcome = value
        }

        override fun presented(text: String) = Unit
    }
}
