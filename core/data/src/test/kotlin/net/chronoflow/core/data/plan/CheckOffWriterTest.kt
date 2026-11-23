package net.chronoflow.core.data.plan

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.sync.support.SyncStore
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripDetailResponse
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.core.testing.sync.detail
import net.chronoflow.core.testing.sync.syncTrip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class CheckOffWriterTest {
    @Test fun setCheckInsertsOneOutboxRowAndTheLocalMark() = write(CheckStatus.Done)

    @Test fun undoStoresNullStatus() = write(null)

    @Test fun nonActionableBlockWritesNothing() = write(CheckStatus.Done, actionable = false)

    @Test fun missingBlockWritesNothing() = write(CheckStatus.Done, blockId = "absent")

    @Test fun missingCheckKeyWritesNothing() = write(CheckStatus.Done, checkKey = false)

    private fun write(
        status: CheckStatus?,
        actionable: Boolean = true,
        blockId: String = "sleep",
        checkKey: Boolean = true,
    ) = runTest(timeout = 60.seconds) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val store = SyncStore(dispatcher)
        try {
            val template = detail()
            val original = template.plan.blocks.single()
            val block = original.copy(actionable = actionable, checkKey = original.checkKey.takeIf { checkKey })
            val detail = template.copy(plan = template.plan.copy(blocks = listOf(block)))
            val repository = TripRepository(store.database)
            repository.store(
                PlanVersionRecord(
                    syncTrip,
                    "hash",
                    TripDetailResponse.encoding(detail).data,
                    "2026-11-10T10:00:00Z",
                    "v1",
                ),
                emptyList(),
            )
            val outbox = OutboxRepository(store.database)
            CheckOffWriter(outbox, null).setCheck(syncTrip, detail, blockId, status)
            val pending = outbox.pending()
            if (!actionable || blockId != "sleep" || !checkKey) {
                assertTrue(pending.isEmpty())
                return@runTest
            }
            val change = (OutboxPayload.change(pending.single()) as OutboxChange.CheckOff).change
            assertEquals(syncTrip, change.tripID)
            assertEquals("hash", change.planInputHash)
            assertEquals(block.checkKey!!.type, change.type)
            assertEquals(block.checkKey!!.localDate, change.localDate)
            assertEquals(block.checkKey!!.ordinal, change.ordinal)
            assertEquals(status, change.status)
            val storedMarks = repository.checks(syncTrip, "hash")
            if (status == null) {
                assertTrue(storedMarks.isEmpty())
            } else {
                assertEquals(status.raw, storedMarks.single().status)
            }
            val merged = PlanDecoder(dispatcher).detail(repository.detailValues(syncTrip).first())!!
            if (status == null) {
                assertTrue(merged.checkState.marks.isEmpty())
            } else {
                assertEquals(
                    status,
                    merged.checkState.marks
                        .single()
                        .status,
                )
            }
        } finally {
            withContext(NonCancellable) { store.close() }
        }
    }
}
