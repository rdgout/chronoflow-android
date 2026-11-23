package net.chronoflow.core.ui.trip

import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.plan.CopyFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.TripListValue
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.plan.todayView
import net.chronoflow.core.plan.trippage.PlanPreferences
import java.time.Instant

fun tripCardUi(
    value: TripListValue,
    detail: TripDetail?,
    now: Instant,
    preferences: PlanPreferences,
    language: Language,
    formatter: CopyLineFormatter,
): TripCardUi {
    val card = value.card
    val options = CopyFormat(language, preferences.timeFormat)
    val next =
        if (value.status == TripStatus.Active && detail != null) {
            todayView(detail, detail.checkState.marks, now, preferences.hiddenCategories).nextStep?.let { block ->
                val start = planDate(block.startUtc)
                NextStepUi(
                    block.type,
                    start ?: Instant.EPOCH,
                    formatter.format(block.text, options.copy(displayZone = block.displayTz)),
                )
            }
        } else {
            null
        }
    return TripCardUi(
        card.id,
        value.status,
        card.flightNumber,
        formatter.formatOffset(card.offsetDifferenceMinutes.toDouble(), language),
        AirportUi(card.from.iata, card.from.city),
        AirportUi(card.to.iata, card.to.city),
        formatter.formatDayMonth(card.departureUtc, language, card.from.zone) + " " +
            formatter.formatInstant(card.departureUtc, options, card.from.zone) + " → " +
            formatter.formatDayMonth(card.arrivalUtc, language, card.to.zone) + " " +
            formatter.formatInstant(card.arrivalUtc, options, card.to.zone),
        next,
    )
}
