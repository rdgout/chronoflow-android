package net.chronoflow.feature.today

import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.BlockView
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.plan.CopyFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.TripListValue
import net.chronoflow.core.plan.blockInterval
import net.chronoflow.core.plan.checkIdentity
import net.chronoflow.core.plan.dayLabelKey
import net.chronoflow.core.plan.dayStatusLabelKey
import net.chronoflow.core.plan.lastEndedTrip
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.plan.selectTodayTrip
import net.chronoflow.core.plan.todayView
import net.chronoflow.core.plan.trippage.PlanPreferences
import net.chronoflow.core.plan.zoneLabel
import net.chronoflow.core.ui.trip.tripCardUi
import java.time.Instant

data class TodayInput(
    val trips: List<TripListValue>?,
    val synced: Boolean,
    val prefs: PlanPreferences,
    val heroDetail: TripDetail?,
    val now: Instant,
    val language: Language,
    val formatter: CopyLineFormatter?,
)

object TodayReducer {
    fun reduce(input: TodayInput): TodayUiState {
        val trips = input.trips ?: return TodayUiState.Loading
        val formatter = input.formatter ?: return TodayUiState.Loading
        if (!input.synced) return TodayUiState.Loading
        if (trips.isEmpty()) return TodayUiState.Empty
        val hero = selectTodayTrip(trips, input.now)
        if (hero == null) {
            val ended = lastEndedTrip(trips, input.now) ?: return TodayUiState.Empty
            val card = ended.card
            return TodayUiState.Completed(
                "${card.from.city ?: card.from.iata} → ${card.to.city ?: card.to.iata}",
                ended.planEndDate ?: card.arriveLocal.take(10),
            )
        }
        val detail = input.heroDetail?.takeIf { it.trip.id == hero.card.id }
        val view = detail?.let { todayView(it, it.checkState.marks, input.now, input.prefs.hiddenCategories) }
        if (hero.status == TripStatus.Upcoming) {
            val next =
                view?.nextStep?.let { block ->
                    val options =
                        CopyFormat(input.language, input.prefs.timeFormat, block.displayTz, trip = detail.trip)
                    NextStepUi(
                        block.id,
                        block.type,
                        formatter.format(block.text, options),
                        planDate(block.startUtc) ?: Instant.EPOCH,
                        formatter.formatInstant(block.startUtc, options),
                    )
                }
            return TodayUiState.Future(
                hero,
                next,
                hero.preparationDays,
                hero.planStartUtc ?: hero.card.departureUtc,
                hero.planStartZone ?: hero.card.from.zone,
            )
        }
        val panel =
            if (view == null) {
                null
            } else {
                val day = view.currentDay
                val dayLabel =
                    day?.let {
                        formatter.catalog.text(
                            dayLabelKey(it.labelPhase) ?: "",
                            input.language,
                            arguments = mapOf("count" to (it.postArrivalIndex ?: 0).toString()),
                            count = (it.postArrivalIndex ?: 0).toDouble(),
                        )
                    }

                fun row(
                    block: BlockView,
                    running: Boolean,
                ): TodayRowUi {
                    val options =
                        CopyFormat(input.language, input.prefs.timeFormat, block.displayTz, trip = detail.trip)
                    val actionable = block.actionable && block.checkKey != null
                    val identity = block.checkKey?.let(::checkIdentity)
                    val mark =
                        if (!actionable) {
                            null
                        } else {
                            view.marks
                                .firstOrNull {
                                    checkIdentity(it.key) == identity
                                }?.status
                        }
                    val target = if (running) block.endUtc else block.startUtc
                    val instantOnly =
                        block.type == BlockType.CaffeineServing ||
                            block.type == BlockType.ZoneChange || block.startUtc == block.endUtc
                    return TodayRowUi(
                        block.id,
                        block.type,
                        actionable,
                        formatter.format(block.text, options),
                        zoneLabel(block.displayTz, detail.trip),
                        running,
                        planDate(target) ?: Instant.EPOCH,
                        formatter.formatInstant(target, options),
                        mark,
                        if (instantOnly) {
                            formatter.formatInstant(block.startUtc, options)
                        } else {
                            formatter.blockInterval(block, options, separator = " → ")
                        },
                    )
                }
                TodayPanelUi(
                    dayLabel,
                    day?.status?.takeIf { dayStatusLabelKey(it) != null },
                    view.dayProgress?.takeIf { it.total > 0 }?.let { ProgressUi(it.done, it.total, it.skipped) },
                    view.currentSteps.map { row(it, true) },
                    view.upcomingSteps.map { row(it, false) },
                    when {
                        view.allCategoriesHidden -> HiddenNotice.All
                        view.importantHidden -> HiddenNotice.Some
                        else -> HiddenNotice.None
                    },
                )
            }
        return TodayUiState.Active(tripCardUi(hero, detail, input.now, input.prefs, input.language, formatter), panel)
    }
}
