package net.chronoflow.core.data.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.sync.support.SyncStore
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.MutationReceipt
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckKey
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckState
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripCheckResult
import net.chronoflow.core.model.contracts.TripDetailResponse
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.sync.CheckOffReconciler
import net.chronoflow.core.sync.FullPull
import net.chronoflow.core.sync.OutboxFailurePolicy
import net.chronoflow.core.sync.OutboxFlusher
import net.chronoflow.core.sync.SyncRefusal
import net.chronoflow.core.sync.SyncRefusalNotice
import net.chronoflow.core.testing.sync.ScriptedSource
import net.chronoflow.core.testing.sync.check
import net.chronoflow.core.testing.sync.detail
import net.chronoflow.core.testing.sync.identity
import net.chronoflow.core.testing.sync.syncDate
import net.chronoflow.core.testing.sync.syncOther
import net.chronoflow.core.testing.sync.syncTrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

class CheckOffReconcilerTests {
    @Test fun stableRekeyOrDisappearedStepTrue() = stableRekeyOrDisappearedStep(true)

    @Test fun stableRekeyOrDisappearedStepFalse() = stableRekeyOrDisappearedStep(false)

    private fun stableRekeyOrDisappearedStep(exists: Boolean) =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                val row = store.enqueue(OutboxChange.CheckOff(check("old")))
                source.configure(detail = detail(blocks = exists))
                val reconciler = CheckOffReconciler(FullPull(store.sync, source, now = { syncDate }))
                val result = reconciler.prepare(row, store.epoch)
                if (exists) {
                    assertEquals(OutboxChange.CheckOff(check()), result?.let { OutboxPayload.change(it) })
                    assertEquals("done", TripRepository(store.database).checks(syncTrip, "hash").first().status)
                } else {
                    assertNull(result)
                    assertTrue(OutboxRepository(store.database).pending().isEmpty())
                }
            } finally {
                store.close()
            }
        }

    private fun receipt(markedAt: String): MutationReceipt {
        val key = CheckKey(BlockType.Sleep, "2026-11-10", 1)
        val mark = CheckMark(key, CheckStatus.Done, markedAt)
        return MutationReceipt.Checks(TripCheckResult(CheckState("hash", listOf(mark), emptyList()), "v2"))
    }

    @Test fun newerLocalClearSurvivesOldHashRekeyAndReceipt() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                val old = store.enqueue(OutboxChange.CheckOff(check("old")))
                store.enqueue(OutboxChange.CheckOff(check(status = null)), 1)
                val reconciler = CheckOffReconciler(FullPull(store.sync, source, now = { syncDate }))
                reconciler.prepare(old, store.epoch)
                assertTrue(TripRepository(store.database).checks(syncTrip, "hash").isEmpty())
                source.configure(receipts = listOf(receipt("later")), hook = { method ->
                    if (method == "send") store.epoch.invalidate()
                })
                try {
                    OutboxFlusher(store.sync, source, { syncDate }).run(store.epoch)
                    error("expected cancellation")
                } catch (
                    _: CancellationException,
                ) {
                }
                assertTrue(TripRepository(store.database).checks(syncTrip, "hash").isEmpty())
            } finally {
                store.close()
            }
        }

    @Test fun receiptCannotReplaceNewerMarkQueuedWhileSending() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                store.enqueue(OutboxChange.CheckOff(check()))
                source.configure(receipts = listOf(receipt("remote")), hook = {
                    if (it == "send") store.enqueue(OutboxChange.CheckOff(check(status = null)), 1)
                })
                OutboxFlusher(store.sync, source, { syncDate }).run(store.epoch)
                assertTrue(TripRepository(store.database).checks(syncTrip, "hash").isEmpty())
                assertEquals(1, OutboxRepository(store.database).pending().size)
            } finally {
                store.close()
            }
        }

    @Test fun staleResponseRefreshesOnceAndThenDefersConflict409() {
        staleResponseRefreshesOnceAndThenDefers(DataFailure.Conflict(409))
    }

    @Test fun staleResponseRefreshesOnceAndThenDefersPlanInputHash422() =
        staleResponseRefreshesOnceAndThenDefers(
            DataFailure.Validation(mapOf("plan_input_hash" to FieldMessage("invalid hash")), 422),
        )

    @Test fun staleResponseRefreshesOnceAndThenDefersForm422() =
        staleResponseRefreshesOnceAndThenDefers(
            DataFailure.Validation(
                mapOf("form" to FieldMessage("This plan has changed. Refresh it before marking this step.")),
                422,
            ),
        )

    private fun staleResponseRefreshesOnceAndThenDefers(failure: DataFailure) =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                store.enqueue(OutboxChange.CheckOff(check()))
                source.configure(failures = listOf(failure, failure))
                val flusher = OutboxFlusher(store.sync, source, { syncDate })
                val result = flusher.run(store.epoch)
                assertEquals(1.0, result.retryAfter)
                assertTrue(result.refusals.isEmpty())
                assertEquals(listOf("send", "detail", "send"), source.calls)
                val pending = OutboxRepository(store.database).pending()
                assertEquals(1, pending.size)
                assertEquals(1, pending.first().attempts)
                assertEquals(if (failure.status == 409) "conflict" else "validation", pending.first().lastError)
                assertTrue(SettingsRepository(store.database).settings().none { it.section == "syncFailure" })
                source.configure(failures = listOf(failure, failure))
                val retry = flusher.run(store.epoch)
                assertEquals(2.0, retry.retryAfter)
                assertTrue(retry.refusals.isEmpty())
                assertEquals(listOf("send", "detail", "send", "send", "detail", "send"), source.calls)
                assertTrue(SettingsRepository(store.database).settings().none { it.section == "syncFailure" })
                assertEquals(2, OutboxRepository(store.database).pending().first().attempts)
            } finally {
                store.close()
            }
        }

    @Test fun nonStaleValidationRefusesCheckOff() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                val bytes = TripDetailResponse.encoding(detail()).data
                TripRepository(
                    store.database,
                ).store(PlanVersionRecord(syncTrip, "hash", bytes, "2027-01-15T08:00:00Z", "v1"), emptyList())
                val row = store.enqueue(OutboxChange.CheckOff(check()))
                source.configure(
                    failures = listOf(DataFailure.Validation(mapOf("status" to FieldMessage("invalid status")), 422)),
                )
                val result = OutboxFlusher(store.sync, source, { syncDate }).run(store.epoch)
                assertEquals(listOf("send"), source.calls)
                assertEquals(listOf(row.id), result.refusals.map { it.id })
                assertEquals(422, result.refusals.first().status)
                assertEquals(SyncRefusalNotice.StepDone(BlockType.Sleep), result.refusals.first().notice)
                assertNull(TripRepository(store.database).plan(syncTrip, "hash")?.detailVersion)
                FullPull(store.sync, source, now = { syncDate }).run(store.epoch)
                assertTrue(TripRepository(store.database).checks(syncTrip, "hash").isEmpty())
                assertTrue("detail" in source.calls)
                assertNull(result.retryAfter)
                assertTrue(OutboxRepository(store.database).pending().isEmpty())
                assertTrue(SettingsRepository(store.database).settings().any { it.section == "syncFailure" })
            } finally {
                store.close()
            }
        }

    @Test fun malformedDetailCannotProveStepDisappeared() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                store.enqueue(OutboxChange.CheckOff(check("old")))
                source.configure(failMethod = "detail", methodFailure = DataFailure.Server(200))
                val result = OutboxFlusher(store.sync, source, { syncDate }).run(store.epoch)
                assertNotNull(result.retryAfter)
                assertEquals(1, OutboxRepository(store.database).pending().size)
                assertTrue(source.changes.isEmpty())
            } finally {
                store.close()
            }
        }

    @Test fun detail404ResolvesRefusalWithoutSpin() =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                store.enqueue(OutboxChange.CheckOff(check("old")))
                source.configure(failMethod = "detail", methodFailure = DataFailure.Server(404))
                val result = OutboxFlusher(store.sync, source, { syncDate }).run(store.epoch)
                assertEquals(1, result.refusals.size)
                assertNull(result.retryAfter)
                assertTrue(OutboxRepository(store.database).pending().isEmpty())
            } finally {
                store.close()
            }
        }

    @Test fun transientCheckOffFailuresKeepTheOptimisticMarkQueuedOffline() =
        transientCheckOffFailuresKeepTheOptimisticMarkQueued(DataFailure.Offline)

    @Test fun transientCheckOffFailuresKeepTheOptimisticMarkQueuedThrottled() =
        transientCheckOffFailuresKeepTheOptimisticMarkQueued(DataFailure.Throttled(60.0, 429))

    @Test fun transientCheckOffFailuresKeepTheOptimisticMarkQueuedServer408() =
        transientCheckOffFailuresKeepTheOptimisticMarkQueued(DataFailure.Server(408))

    private fun transientCheckOffFailuresKeepTheOptimisticMarkQueued(failure: DataFailure) =
        runTest(timeout = 60.seconds) {
            val store = SyncStore(StandardTestDispatcher(testScheduler))
            try {
                val source = ScriptedSource()
                store.enqueue(OutboxChange.CheckOff(check()))
                source.configure(failures = listOf(failure))
                val result = OutboxFlusher(store.sync, source, { syncDate }).run(store.epoch)
                assertTrue(result.refusals.isEmpty())
                assertEquals(1, OutboxRepository(store.database).pending().size)
                assertEquals("done", TripRepository(store.database).checks(syncTrip, "hash").first().status)
            } finally {
                store.close()
            }
        }
}
