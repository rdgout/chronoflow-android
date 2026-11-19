package net.chronoflow.core.plan.step

import net.chronoflow.core.model.contracts.BlockParams
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.plan.CopyFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.at
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.plan.testCatalog
import net.chronoflow.core.plan.tripDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class StepSheetStateTest {
    private val detail = tripDetail()
    private val block =
        detail.plan.blocks
            .first {
                it.actionable && it.checkKey != null
            }.copy(startUtc = at(0.0), endUtc = at(180.0))
    private val sample =
        detail.copy(
            plan = detail.plan.copy(blocks = listOf(block)),
            checkState = detail.checkState.copy(marks = emptyList()),
        )

    private fun state(
        value: TripDetail? = sample,
        now: Double = -1.0,
        language: Language = Language.En,
        id: String = block.id,
    ) = stepSheetState(
        value,
        id,
        planDate(at(now))!!.toEpochMilli(),
        CopyLineFormatter(testCatalog),
        language,
        TimeFormat.Value24,
    )

    private fun step(
        value: TripDetail = sample,
        now: Double = -1.0,
        language: Language = Language.En,
    ) = state(value, now, language) as StepSheetState.Step

    private fun marked(status: CheckStatus) =
        sample.copy(checkState = sample.checkState.copy(marks = listOf(CheckMark(block.checkKey!!, status, at(0.0)))))

    @Test fun runningShowsNowAndRoundedUpDurationLeft() {
        val value = step(now = 0.5)
        assertEquals(StepBadge.Now, value.badge)
        assertEquals("3 hr", value.durationLeft)
    }

    @Test fun runningShowsDutchDuration() {
        assertEquals("3 uur", step(now = 0.5, language = Language.Nl).durationLeft)
    }

    @Test fun upcomingHasNoBadge() {
        assertNull(step().badge)
        assertNull(step().durationLeft)
    }

    @Test fun endingExactlyNowIsNotRunning() {
        assertNull(step(now = 180.0).badge)
        assertNull(step(now = 180.0).durationLeft)
    }

    @Test fun doneAndSkippedMarksPickBadgeAndUndo() {
        assertEquals(StepBadge.Done, step(marked(CheckStatus.Done)).badge)
        assertEquals(StepBadge.Skipped, step(marked(CheckStatus.Skipped)).badge)
        assertEquals(StepActions.Marked, step(marked(CheckStatus.Done)).actions)
        assertEquals(StepBadge.Skipped, step(marked(CheckStatus.Unknown("future"))).badge)
    }

    @Test fun informationStepHasNoActions() {
        val value =
            step(
                sample.copy(
                    plan = sample.plan.copy(blocks = listOf(block.copy(actionable = false, priority = Priority.Info))),
                ),
            )
        assertEquals(StepActions.None, value.actions)
        assertEquals("Information", value.priorityKey)
    }

    @Test fun nonActionableBlockIgnoresItsMark() {
        val marked = marked(CheckStatus.Done)
        val value = step(marked.copy(plan = marked.plan.copy(blocks = listOf(block.copy(actionable = false)))))
        assertNull(value.badge)
        assertEquals(StepActions.None, value.actions)
    }

    @Test fun missingBlockOrDetailIsGone() {
        assertEquals(StepSheetState.Gone, state(null))
        assertEquals(StepSheetState.Gone, state(id = "missing"))
    }

    @Test fun zoneChangeShowsStartOnly() {
        val value = step(sample.copy(plan = sample.plan.copy(blocks = listOf(block.copy(type = BlockType.ZoneChange)))))
        assertFalse(value.interval.contains("→"))
    }

    @Test fun reasonsAndInstructionsAreFormatted() {
        val value = step()
        val options =
            CopyFormat(Language.En, displayZone = block.displayTz, annotateInstantZone = true, trip = sample.trip)
        val expected =
            listOfNotNull(
                block.text,
                block.secondaryText,
            ).map { CopyLineFormatter(testCatalog).format(it, options) }
        assertEquals(expected, value.instructions)
        val note = detail.plan.blocks.firstOrNull { it.params is BlockParams.Note }
        if (note != null) {
            val result =
                stepSheetState(
                    detail,
                    note.id,
                    0,
                    CopyLineFormatter(testCatalog),
                    Language.En,
                    TimeFormat.Value24,
                ) as StepSheetState.Step
            assertEquals(
                (note.params as BlockParams.Note)
                    .value.note.lines.size,
                result.instructions.size,
            )
        }
    }
}
