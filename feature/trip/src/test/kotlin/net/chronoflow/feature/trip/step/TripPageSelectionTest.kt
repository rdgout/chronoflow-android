@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.trip.step

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.navigation.TripSheet
import net.chronoflow.core.plan.step.StepBadge
import net.chronoflow.core.plan.step.StepSheetState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripPageSelectionTest {
    @After fun resetDispatcher() {
        Dispatchers.resetMain()
    }

    @Test fun selectionKeepsTheTappedBlockAndLiveChecksThroughReplacementAndRemoval() =
        runTest(timeout = 60.seconds) {
            val rig = StepModelRig(this)
            try {
                runCurrent()
                val vm = rig.vm
                val id = rig.block.id
                vm.openStep(id)
                runCurrent()
                vm.select(
                    rig.detail.plan.days
                        .last()
                        .id,
                    0.0,
                )
                vm.toggleView()
                assertEquals(TripSheet.Step(id), vm.sheet.value)
                vm.setCheck(id, CheckStatus.Done)
                runCurrent()
                assertEquals(StepBadge.Done, (vm.stepSheet.value as StepSheetState.Step).badge)
                vm.setCheck(id, null)
                runCurrent()
                assertEquals(StepBadge.Now, (vm.stepSheet.value as StepSheetState.Step).badge)
                vm.setCheck(id, CheckStatus.Skipped)
                runCurrent()
                assertEquals(StepBadge.Skipped, (vm.stepSheet.value as StepSheetState.Step).badge)
                vm.openStep(id)
                assertEquals(TripSheet.Step(id), vm.sheet.value)
                rig.replace(
                    rig.detail.copy(
                        plan =
                            rig.detail.plan.copy(
                                blocks =
                                    rig.detail.plan.blocks.filterNot {
                                        it.id ==
                                            id
                                    },
                            ),
                    ),
                )
                runCurrent()
                assertEquals(StepSheetState.Gone, vm.stepSheet.value)
                val count = rig.writes.size
                vm.setCheck(id, CheckStatus.Done)
                runCurrent()
                assertEquals(count, rig.writes.size)
                rig.source.snapshot.value =
                    rig.source.snapshot.value
                        .copy(plan = null)
                runCurrent()
                assertEquals(StepSheetState.Gone, vm.stepSheet.value)
                assertEquals(id, vm.selectedStep.value?.blockId)
                assertTrue(rig.errors.isEmpty())
            } finally {
                rig.store.clear()
            }
        }
}
