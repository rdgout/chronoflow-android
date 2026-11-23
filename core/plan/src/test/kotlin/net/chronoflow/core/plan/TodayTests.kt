package net.chronoflow.core.plan

import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayTests {
    @Test fun todaySeparatesCurrentAndFourUpcomingAndDoesNotHideTheNextImportantStep() {
        val original = tripDetail()
        val detail = original.copy(plan = original.plan.copy(blocks = original.plan.blocks.sortedBy { it.startUtc }))
        val now = planDate(detail.plan.days[0].startUtc)!!
        val view = todayView(detail, emptyList(), now)
        assertTrue(view.upcomingSteps.size <= 4)
        assertTrue(view.currentSteps.all { planDate(it.startUtc)!! <= now && now < planDate(it.endUtc)!! })
        assertTrue(view.currentSteps.all { it.type !in listOf(BlockType.LightOK, BlockType.LightNeutral) })
        val next = view.nextStep
        assertNotNull(next)
        next!!
        val hidden = todayView(detail, emptyList(), now, listOf(next.type))
        assertEquals(next.id, hidden.nextStep?.id)
        assertFalse(hidden.currentSteps.any { it.type == next.type })
        val marked = todayView(detail, listOf(CheckMark(next.checkKey!!, CheckStatus.Done, next.startUtc)), now)
        assertNotEquals(next.id, marked.nextStep?.id)
    }

    @Test fun calendarDaysUseThePlanZoneAcrossDaylightSaving() {
        assertEquals(
            1,
            calendarDaysUntil("2026-03-29T22:30:00Z", planDate("2026-03-28T23:30:00Z")!!, "Europe/Amsterdam"),
        )
    }

    @Test fun nextStepIgnoresHiddenCategories() {
        val detail = tripDetail()
        val now =
            planDate(
                detail.plan.days
                    .first()
                    .startUtc,
            )!!
        val visible = todayView(detail, emptyList(), now)
        val hidden = todayView(detail, emptyList(), now, BlockType.known)
        assertNotNull(visible.nextStep)
        assertEquals(visible.nextStep, hidden.nextStep)
        assertTrue(hidden.allCategoriesHidden)
        assertTrue(hidden.currentSteps.isEmpty())
        assertTrue(hidden.upcomingSteps.isEmpty())
    }
}
