package net.chronoflow.core.plan

import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.TripProgress
import net.chronoflow.core.model.contracts.TripStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

internal fun todayRecoveryCard(detail: TripDetail): TripCardView =
    TripCardView(
        detail.trip.id,
        detail.trip.from,
        detail.trip.to,
        detail.trip.departLocal,
        "",
        detail.trip.departureUtc,
        detail.trip.endUtc,
        null,
        emptyList(),
        0,
        TripProgress(TripStatus.Upcoming, null),
        false,
        false,
        false,
        false,
        false,
    )

class TodayEdgeTests {
    private val original = tripDetail()
    private val now =
        planDate(
            original.plan.days
                .first()
                .startUtc,
        )!!
    private val block =
        original.plan.blocks.first { it.actionable && it.checkKey != null }.copy(
            type = BlockType.Sleep,
            priority = Priority.Core,
            startUtc = now.minusSeconds(60).toString(),
            endUtc = now.plusSeconds(600).toString(),
        )

    private fun detail(vararg blocks: net.chronoflow.core.model.contracts.BlockView) =
        original.copy(plan = original.plan.copy(blocks = blocks.toList()))

    @Test fun skippedMarkMovesNextButKeepsCurrentRow() {
        val view =
            todayView(detail(block), listOf(CheckMark(block.checkKey!!, CheckStatus.Skipped, block.startUtc)), now)
        assertNull(view.nextStep)
        assertEquals(listOf(block), view.currentSteps)
    }

    @Test fun markedUpcomingRowStaysInPlace() {
        val future = block.copy(startUtc = now.plusSeconds(60).toString())
        val view = todayView(detail(future), listOf(CheckMark(block.checkKey!!, CheckStatus.Done, block.startUtc)), now)
        assertEquals(listOf(future), view.upcomingSteps)
        assertNull(view.nextStep)
    }

    @Test fun optionalSeekLightIsAlwaysStripped() {
        val optional = block.copy(type = BlockType.SeekLight, priority = Priority.Optional)
        val view = todayView(detail(optional), emptyList(), now)
        assertTrue(view.currentSteps.isEmpty())
        assertNull(view.nextStep)
    }

    @Test fun oneMillisecondRoundsUp() = minutes(1, 1)

    @Test fun oneMinuteRemainsOne() = minutes(60_000, 1)

    @Test fun oneMinuteAndOneMillisecondRoundsUp() = minutes(60_001, 2)

    @Test fun startedStepHasZeroMinutes() = minutes(-1, 0)

    private fun minutes(
        delta: Long,
        expected: Int,
    ) {
        val future = block.copy(startUtc = now.plusMillis(delta).toString())
        assertEquals(expected, todayView(detail(future), emptyList(), now).minutesUntilNext)
    }

    @Test fun coreHiddenNoticeReadsTheUnstrippedPlan() {
        val stripped = block.copy(type = BlockType.CaffeineOK)
        val view = todayView(detail(stripped), emptyList(), now, listOf(BlockType.CaffeineOK))
        assertTrue(view.importantHidden)
        assertTrue(view.currentSteps.isEmpty())
    }

    @Test fun sixteenOfSeventeenCategoriesIsNotAll() {
        assertEquals(17, BlockType.known.size)
        assertFalse(todayView(detail(block), emptyList(), now, BlockType.known.dropLast(1)).allCategoriesHidden)
        assertTrue(todayView(detail(block), emptyList(), now, BlockType.known).allCategoriesHidden)
    }

    @Test fun invalidZoneFallsBackToUtc() {
        assertEquals(2, calendarDaysUntil("2026-11-10T00:00:00Z", Instant.parse("2026-11-08T23:30:00Z"), "Mars/Base"))
    }

    @Test fun pastDateGivesZero() {
        assertEquals(0, calendarDaysUntil(now.minusSeconds(86400).toString(), now, "UTC"))
    }

    @Test fun startIsCurrentAndEndIsNotCurrent() {
        assertEquals(listOf(block), todayView(detail(block), emptyList(), planDate(block.startUtc)!!).currentSteps)
        assertTrue(todayView(detail(block), emptyList(), planDate(block.endUtc)!!).currentSteps.isEmpty())
    }

    @Test fun zeroDurationIsOnlyUpcomingBeforeItsStart() {
        val point = block.copy(startUtc = now.toString(), endUtc = now.toString())
        assertEquals(listOf(point), todayView(detail(point), emptyList(), now.minusMillis(1)).upcomingSteps)
        assertTrue(todayView(detail(point), emptyList(), now).currentSteps.isEmpty())
        assertTrue(todayView(detail(point), emptyList(), now).upcomingSteps.isEmpty())
    }

    @Test fun noDaysUsesTripBounds() {
        val empty = original.copy(plan = original.plan.copy(days = emptyList(), blocks = emptyList()))
        assertTrue(todayView(empty, emptyList(), planDate(empty.trip.departureUtc)!!.minusMillis(1)).beforePlan)
        assertTrue(todayView(empty, emptyList(), planDate(empty.trip.endUtc)!!).afterPlan)
    }

    @Test fun unknownTypesNeverRender() {
        val unknown = block.copy(type = BlockType.Unknown("new"))
        val view = todayView(detail(unknown), emptyList(), now)
        assertTrue(view.currentSteps.isEmpty())
        assertTrue(view.upcomingSteps.isEmpty())
    }

    @Test fun upcomingKeepsPlanOrderAndCapsAtFour() {
        val blocks = (1..6).map { block.copy(id = "b$it", startUtc = now.plusSeconds(600L - it).toString()) }
        assertEquals(blocks.take(4), todayView(detail(*blocks.toTypedArray()), emptyList(), now).upcomingSteps)
    }

    @Test fun missingPlanStartUsesServerStatusAndDeparture() {
        val card = todayRecoveryCard(original)
        val early = TripListValue(card.copy(id = "early", departureUtc = now.toString()), null, null, TripStatus.Active)
        val late =
            TripListValue(
                card.copy(id = "late", departureUtc = now.plusSeconds(1).toString()),
                null,
                null,
                TripStatus.Active,
            )
        val selected = selectTodayTrip(listOf(late, early), now)!!
        assertEquals("early", selected.card.id)
        assertEquals(TripStatus.Upcoming, tripDisplayStatus(selected.card, null, null, now))
    }

    @Test fun equalActiveEndsTieBreakById() {
        val card = todayRecoveryCard(original)
        val a =
            TripListValue(
                card.copy(id = "a"),
                now.minusSeconds(1).toString(),
                now.plusSeconds(1).toString(),
                TripStatus.Upcoming,
            )
        val b = a.copy(card = card.copy(id = "b"))
        assertEquals("a", selectTodayTrip(listOf(b, a), now)?.card?.id)
        assertEquals("a", lastEndedTrip(listOf(b, a), now.plusSeconds(2))?.card?.id)
    }

    @Test fun completedEndIsClampedToNowBeforeIdentityTieBreak() {
        val card = todayRecoveryCard(original).copy(progress = TripProgress(TripStatus.Completed, null))
        val a = TripListValue(card.copy(id = "a"), null, now.plusSeconds(10).toString(), TripStatus.Completed)
        val b = a.copy(card = card.copy(id = "b"), planEndUtc = now.plusSeconds(20).toString())
        assertEquals("a", lastEndedTrip(listOf(b, a), now)?.card?.id)
        assertNull(selectTodayTrip(listOf(b, a), now))
    }
}
