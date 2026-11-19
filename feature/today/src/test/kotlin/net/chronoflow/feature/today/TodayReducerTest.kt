package net.chronoflow.feature.today

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.CopyArg
import net.chronoflow.core.model.contracts.CopyArgKind
import net.chronoflow.core.model.contracts.CopyArgValue
import net.chronoflow.core.model.contracts.CopyArgument
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.plan.trippage.PlanPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TodayReducerTest {
    private fun check(block: (TodayInput) -> Unit) =
        runTest(timeout = 60.seconds) {
            block(todayInput(todayFormatter(StandardTestDispatcher(testScheduler))))
        }

    @Test fun notLoadedIsLoading() =
        check {
            assertEquals(TodayUiState.Loading, TodayReducer.reduce(it.copy(trips = null)))
        }

    @Test fun unsyncedIsLoading() =
        check {
            assertEquals(TodayUiState.Loading, TodayReducer.reduce(it.copy(synced = false)))
        }

    @Test fun missingFormatterIsLoading() =
        check {
            assertEquals(TodayUiState.Loading, TodayReducer.reduce(it.copy(formatter = null)))
        }

    @Test fun noTripsIsEmpty() =
        check {
            assertEquals(TodayUiState.Empty, TodayReducer.reduce(it.copy(trips = emptyList())))
        }

    @Test fun completedUsesCitiesAndPlanEndDate() =
        check {
            val trip = todayTrip(status = TripStatus.Completed)
            assertEquals(
                TodayUiState.Completed("Amsterdam → New York", "2026-11-09"),
                TodayReducer.reduce(it.copy(trips = listOf(trip))),
            )
        }

    @Test fun completedFallsBackToAirportCodesAndArrivalDate() =
        check {
            val trip = todayTrip(status = TripStatus.Completed)
            val card = trip.card.copy(from = trip.card.from.copy(city = null), to = trip.card.to.copy(city = null))
            assertEquals(
                TodayUiState.Completed("AMS → JFK", card.arriveLocal.take(10)),
                TodayReducer.reduce(it.copy(trips = listOf(trip.copy(card = card, planEndDate = null)))),
            )
        }

    @Test fun upcomingKeepsPreparationDays() =
        check {
            val trip = todayTrip(status = TripStatus.Upcoming, start = todayNow.plusSeconds(86400))
            val state = TodayReducer.reduce(it.copy(trips = listOf(trip))) as TodayUiState.Future
            assertEquals(3, state.preparationDays)
            assertEquals(trip.planStartUtc, state.planStartUtc)
        }

    @Test fun futureInstantInAnotherZoneUsesNoAirportCityContext() =
        check { input ->
            val formatter = input.formatter!!
            val detail = input.heroDetail!!
            val arg = CopyArg("time", CopyArgKind.Instant, CopyArgValue.Text(todayNow.toString()), "America/New_York")
            val block =
                detail.plan.blocks.first().copy(
                    displayTz = "Europe/Amsterdam",
                    text = CopyLine(":time", listOf(CopyArgument.Standard(arg))),
                )
            val trip = detail.trip.copy(to = detail.trip.to.copy(city = "Queens", zone = "America/New_York"))
            val futureDetail = detail.copy(trip = trip, plan = detail.plan.copy(blocks = listOf(block)))
            val upcoming = todayTrip(status = TripStatus.Upcoming, start = todayNow.plusSeconds(86400))
            val futureInput = input.copy(trips = listOf(upcoming), heroDetail = futureDetail)
            val options = TodayReducer.futureOptions(futureInput, block)
            assertNull(options.trip)
            assertEquals("05:00 (New York)", formatter.formatArgument(arg, options.copy(annotateInstantZone = true)))
            val state = TodayReducer.reduce(futureInput) as TodayUiState.Future
            assertEquals("05:00", state.next!!.title)
        }

    @Test fun futurePlanStartFallsBackToDepartureZone() =
        check {
            val trip =
                todayTrip(
                    status = TripStatus.Upcoming,
                    start = todayNow.plusSeconds(86400),
                ).copy(planStartUtc = null, planStartZone = null)
            val state = TodayReducer.reduce(it.copy(trips = listOf(trip))) as TodayUiState.Future
            assertEquals(trip.card.departureUtc, state.planStartUtc)
            assertEquals(trip.card.from.zone, state.planStartZone)
        }

    @Test fun activeWithoutDetailKeepsHeroAndHasNoPanel() =
        check {
            assertNull((TodayReducer.reduce(it.copy(heroDetail = null)) as TodayUiState.Active).panel)
        }

    @Test fun allHiddenBeatsSomeHidden() =
        check {
            val state =
                TodayReducer.reduce(
                    it.copy(prefs = PlanPreferences(hiddenCategories = BlockType.known)),
                ) as TodayUiState.Active
            assertEquals(HiddenNotice.All, state.panel!!.hidden)
            assertTrue(state.panel.current.isEmpty())
        }

    @Test fun markedRowsKeepTheirSectionAndPosition() =
        check { input ->
            val before = (TodayReducer.reduce(input) as TodayUiState.Active).panel!!
            val detail = input.heroDetail!!
            val block = detail.plan.blocks[2]
            val changed =
                detail.copy(
                    checkState =
                        detail.checkState.copy(
                            marks = listOf(CheckMark(block.checkKey!!, CheckStatus.Done, block.startUtc)),
                        ),
                )
            val after = (TodayReducer.reduce(input.copy(heroDetail = changed)) as TodayUiState.Active).panel!!
            assertEquals(before.upcoming.map { it.blockId }, after.upcoming.map { it.blockId })
            assertEquals(before.current.map { it.blockId }, after.current.map { it.blockId })
            assertEquals(CheckStatus.Done, after.upcoming[1].mark)
        }

    @Test fun nonActionableBlockHasNoMarkOrActionableTagModel() =
        check { input ->
            val detail = input.heroDetail!!
            val block =
                detail.plan.blocks
                    .first()
                    .copy(actionable = false)
            val changed =
                detail.copy(
                    plan = detail.plan.copy(blocks = listOf(block)),
                    checkState =
                        detail.checkState.copy(
                            marks = listOf(CheckMark(block.checkKey!!, CheckStatus.Done, block.startUtc)),
                        ),
                )
            val row =
                (
                    TodayReducer.reduce(
                        input.copy(heroDetail = changed),
                    ) as TodayUiState.Active
                ).panel!!.current.single()
            assertFalse(row.actionable)
            assertNull(row.mark)
        }

    @Test fun staleDetailIsIgnored() =
        check {
            assertNull((TodayReducer.reduce(it.copy(heroDetail = todayDetail("other"))) as TodayUiState.Active).panel)
        }
}
