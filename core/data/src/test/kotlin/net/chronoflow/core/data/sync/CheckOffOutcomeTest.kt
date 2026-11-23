package net.chronoflow.core.data.sync

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.sync.support.SyncStore
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripDetailResponse
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.sync.SyncEngine
import net.chronoflow.core.sync.SyncRefusalNotice
import net.chronoflow.core.sync.SyncScheduler
import net.chronoflow.core.sync.refusal.RefusedChangeModel
import net.chronoflow.core.testing.sync.ScriptedSource
import net.chronoflow.core.testing.sync.check
import net.chronoflow.core.testing.sync.detail
import net.chronoflow.core.testing.sync.syncDate
import net.chronoflow.core.testing.sync.syncTrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class CheckOffOutcomeTest {
    @Test fun throttled429KeepsMarkAndShowsNoMessage() = outcome(DataFailure.Throttled(60.0, 429), "queued")

    @Test fun timeout408KeepsMarkAndShowsNoMessage() = outcome(DataFailure.Server(408), "queued")

    @Test fun stale422PullsOnceRetriesOnceNoMessage() =
        outcome(
            DataFailure.Validation(mapOf("plan_input_hash" to FieldMessage("invalid hash")), 422),
            "stale",
        )

    @Test fun nonStale422RefusesUndoesAndShowsMessage() =
        outcome(
            DataFailure.Validation(mapOf("status" to FieldMessage("invalid status")), 422),
            "refused",
        )

    private fun outcome(
        failure: DataFailure,
        expected: String,
    ) = runTest(timeout = 60.seconds) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = SyncStore(dispatcher)
        val source = ScriptedSource()
        val engine =
            SyncEngine(
                store.sync,
                source,
                store.epoch,
                scheduler = SyncScheduler({ syncDate }, { awaitCancellation() }),
                dispatcher = dispatcher,
            )
        val notices = RefusedChangeModel()
        try {
            val repository = TripRepository(store.database)
            repository.store(
                PlanVersionRecord(
                    syncTrip,
                    "hash",
                    TripDetailResponse.encoding(detail()).data,
                    "2027-01-15T08:00:00Z",
                    "v1",
                ),
                emptyList(),
            )
            store.enqueue(OutboxChange.CheckOff(check()))
            source.configure(failures = if (expected == "stale") listOf(failure, null) else listOf(failure))
            val result = engine.refreshNow()
            notices.receive(result)
            val merged = PlanDecoder(dispatcher).detail(repository.detailValues(syncTrip).first())!!
            when (expected) {
                "queued" -> {
                    assertEquals(1, OutboxRepository(store.database).pending().size)
                    assertEquals(
                        CheckStatus.Done,
                        merged.checkState.marks
                            .single()
                            .status,
                    )
                    assertNull(notices.visible.value)
                }

                "stale" -> {
                    assertEquals(2, source.calls.count { it == "send" })
                    assertEquals(1, source.calls.count { it == "detail" })
                    assertTrue(result.refusals.isEmpty())
                    assertNull(notices.visible.value)
                }

                "refused" -> {
                    assertTrue(merged.checkState.marks.isEmpty())
                    assertEquals(SyncRefusalNotice.StepDone(BlockType.Sleep), notices.visible.value?.notice)
                    assertTrue(OutboxRepository(store.database).pending().isEmpty())
                }
            }
        } finally {
            withContext(NonCancellable) {
                engine.cancelAndWait()
                store.close()
            }
        }
    }
}
