package net.chronoflow.core.plan.step

import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.plan.prepare
import net.chronoflow.core.plan.tripDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckActionsTest {
    private val day =
        prepare(tripDetail()).days.first { d ->
            d.blocks.any {
                it.block.actionable &&
                    it.block.checkKey != null
            }
        }
    private val block = day.blocks.first { it.block.actionable && it.block.checkKey != null }

    @Test fun actionableUnmarkedIsOpen() {
        assertEquals(
            StepActions.Open,
            checkActions(day.copy(blocks = listOf(block.copy(status = null)))).byBlock[block.id],
        )
    }

    @Test fun doneIsMarked() {
        assertEquals(
            StepActions.Marked,
            checkActions(day.copy(blocks = listOf(block.copy(status = CheckStatus.Done)))).byBlock[block.id],
        )
    }

    @Test fun skippedIsMarked() {
        assertEquals(
            StepActions.Marked,
            checkActions(day.copy(blocks = listOf(block.copy(status = CheckStatus.Skipped)))).byBlock[block.id],
        )
    }

    @Test fun noKeyIsAbsent() {
        assertTrue(
            checkActions(
                day.copy(blocks = listOf(block.copy(block = block.block.copy(checkKey = null)))),
            ).byBlock.isEmpty(),
        )
    }

    @Test fun unchangedDayKeepsActions() {
        assertSame(day.checkActions, checkActions(day))
        assertEquals(checkActions(day), checkActions(day))
    }
}
