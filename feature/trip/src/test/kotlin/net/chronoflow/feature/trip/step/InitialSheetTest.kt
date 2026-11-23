@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.trip.step

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.navigation.TripSheet
import net.chronoflow.core.testing.LargeTimelineFixtures
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class InitialSheetTest {
    @After fun resetDispatcher() {
        Dispatchers.resetMain()
    }

    private val detail = LargeTimelineFixtures.fixture("S1").detail
    private val block = detail.plan.blocks.first { it.actionable && it.checkKey != null }

    @Test fun existingStepOpensOnFirstReady() =
        runTest(timeout = 60.seconds) {
            val rig = StepModelRig(this, initial = TripSheet.Step(block.id))
            try {
                assertNull(rig.vm.sheet.value)
                runCurrent()
                assertEquals(TripSheet.Step(block.id), rig.vm.sheet.value)
            } finally {
                rig.store.clear()
            }
        }

    @Test fun missingStepIsCleared() =
        runTest(timeout = 60.seconds) {
            val rig = StepModelRig(this, initial = TripSheet.Step("missing"))
            try {
                runCurrent()
                assertNull(rig.vm.sheet.value)
            } finally {
                rig.store.clear()
            }
        }

    @Test fun notificationHashMatchOpensOnlyOnce() =
        runTest(timeout = 60.seconds) {
            val rig = StepModelRig(this, hash = detail.plan.meta.inputHash, blockId = block.id)
            try {
                runCurrent()
                assertEquals(TripSheet.Step(block.id), rig.vm.sheet.value)
                rig.vm.dismissSheet()
                rig.replace(detail.copy(detailVersion = "next"))
                runCurrent()
                assertNull(rig.vm.sheet.value)
            } finally {
                rig.store.clear()
            }
        }

    @Test fun notificationHashMismatchDoesNotOpen() =
        runTest(timeout = 60.seconds) {
            val rig = StepModelRig(this, hash = "mismatch", blockId = block.id)
            try {
                runCurrent()
                assertNull(rig.vm.sheet.value)
            } finally {
                rig.store.clear()
            }
        }

    @Test fun tripActionsWithoutCardIsCleared() =
        runTest(timeout = 60.seconds) {
            val rig = StepModelRig(this, initial = TripSheet.TripActions)
            try {
                runCurrent()
                assertNull(rig.vm.sheet.value)
                rig.vm.openTripActions()
                assertNull(rig.vm.sheet.value)
            } finally {
                rig.store.clear()
            }
        }
}
