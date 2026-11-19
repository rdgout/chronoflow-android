@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.trip.step

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.contracts.CheckStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class StepCheckTest {
    @After fun resetDispatcher() {
        Dispatchers.resetMain()
    }

    @Test fun localCheckUndoAndReplanAreObservedThroughTheOutbox() =
        runTest(timeout = 60.seconds) {
            val rig = StepModelRig(this)
            try {
                runCurrent()
                val vm = rig.vm
                val key = checkNotNull(rig.block.checkKey)
                vm.setCheck(rig.block.id, CheckStatus.Done)
                runCurrent()
                assertEquals(
                    CheckOffChange(
                        "fixture-S1",
                        rig.detail.plan.meta.inputHash,
                        key.type,
                        key.localDate,
                        key.ordinal,
                        CheckStatus.Done,
                    ),
                    rig.writes.single(),
                )
                assertEquals(
                    CheckStatus.Done,
                    vm.ui.value.plan!!
                        .detail.checkState.marks
                        .first { it.key == key }
                        .status,
                )
                assertEquals(
                    CheckStatus.Done,
                    vm.ui.value.plan!!
                        .prepared.days
                        .flatMap { it.blocks }
                        .first { it.id == rig.block.id }
                        .status,
                )
                val replacement =
                    rig.detail.plan.copy(
                        meta =
                            rig.detail.plan.meta
                                .copy(inputHash = "replacement"),
                    )
                rig.replace(rig.detail.copy(plan = replacement))
                runCurrent()
                assertEquals(
                    CheckStatus.Done,
                    vm.ui.value.plan!!
                        .detail.checkState.marks
                        .first { it.key == key }
                        .status,
                )
                vm.setCheck(rig.block.id, null)
                runCurrent()
                assertEquals("replacement", rig.writes.last().planInputHash)
                assertEquals(null, rig.writes.last().status)
                assertTrue(
                    vm.ui.value.plan!!
                        .detail.checkState.marks
                        .none { it.key == key },
                )
            } finally {
                rig.store.clear()
            }
        }

    @Test fun invalidatedPermitDoesNotWriteOrShowAnErrorState() =
        runTest(timeout = 60.seconds) {
            val rig = StepModelRig(this)
            try {
                runCurrent()
                rig.permit.invalidate()
                rig.vm.setCheck(rig.block.id, CheckStatus.Done)
                runCurrent()
                assertTrue(rig.writes.isEmpty())
                assertFalse(rig.vm.checkBusy.value)
            } finally {
                rig.store.clear()
            }
        }

    @Test fun busyBlocksSecondWrite() =
        runTest(timeout = 60.seconds) {
            val gate = CompletableDeferred<Unit>()
            val rig = StepModelRig(this, beforeWrite = { gate.await() })
            try {
                runCurrent()
                rig.vm.setCheck(
                    rig.block.id,
                    CheckStatus.Done,
                )
                rig.vm.setCheck(
                    rig.block.id,
                    CheckStatus.Skipped,
                )
                runCurrent()
                assertTrue(rig.vm.checkBusy.value)
                gate.complete(Unit)
                runCurrent()
                assertEquals(1, rig.writes.size)
                assertFalse(rig.vm.checkBusy.value)
            } finally {
                rig.store.clear()
            }
        }

    @Test fun missingNonActionableAndNoKeyAreNoOps() =
        runTest(timeout = 60.seconds) {
            val rig = StepModelRig(this)
            try {
                runCurrent()
                rig.vm.setCheck("missing", CheckStatus.Done)
                val info = rig.detail.plan.copy(blocks = listOf(rig.block.copy(actionable = false)))
                rig.replace(rig.detail.copy(plan = info))
                runCurrent()
                rig.vm.setCheck(rig.block.id, CheckStatus.Done)
                val noKey = rig.detail.plan.copy(blocks = listOf(rig.block.copy(checkKey = null)))
                rig.replace(rig.detail.copy(plan = noKey))
                runCurrent()
                rig.vm.setCheck(
                    rig.block.id,
                    CheckStatus.Done,
                )
                runCurrent()
                assertTrue(rig.writes.isEmpty())
            } finally {
                rig.store.clear()
            }
        }

    @Test fun endedSessionIsNoOp() =
        runTest(timeout = 60.seconds) {
            val rig = StepModelRig(this)
            try {
                runCurrent()
                rig.current = false
                rig.vm.setCheck(
                    rig.block.id,
                    CheckStatus.Done,
                )
                runCurrent()
                assertTrue(rig.writes.isEmpty())
            } finally {
                rig.store.clear()
            }
        }
}
