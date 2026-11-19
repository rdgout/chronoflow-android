package net.chronoflow.core.sync.refusal

import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.sync.SyncRefusal
import net.chronoflow.core.sync.SyncRefusalNotice
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RefusedChangeModelTests {
    private val model = RefusedChangeModel()
    private val first =
        SyncRefusal("a", "checkOff", 422, "now", "refused", SyncRefusalNotice.StepDone(BlockType.AvoidLight))

    private fun result(vararg refusals: SyncRefusal) = SyncResult(SyncState.Idle, 0, refusals = refusals.toList())

    @Test fun failedWithRetryShowsNothingThenIdleExposes() {
        model.receive(result(first).copy(state = SyncState.Failed, retryAfter = 1.0))
        assertNull(model.visible.value)
        model.receive(result())
        assertEquals(first, model.visible.value)
    }

    @Test fun lastNoticeWins() {
        val last = first.copy(id = "b")
        model.receive(result(first, last, first.copy(id = "c", notice = null)))
        assertEquals(last, model.visible.value)
    }

    @Test fun consumeHidesAndReplayDoesNotReshow() {
        model.receive(result(first))
        model.consume(first.id)
        assertNull(model.visible.value)
        model.receive(result(first))
        assertNull(model.visible.value)
    }

    @Test fun clearResets() {
        model.receive(result(first))
        model.consume(first.id)
        model.clear()
        model.receive(result(first))
        assertEquals(first, model.visible.value)
        model.clear()
        assertNull(model.visible.value)
    }

    @Test fun consumingOldIdDoesNotHideNewNotice() {
        model.receive(result(first.copy(id = "new")))
        model.consume(first.id)
        assertEquals("new", model.visible.value?.id)
    }
}
