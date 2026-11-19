package net.chronoflow.feature.trips

import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.plan.CopyFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.TripListValue
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.plan.todayView
import java.time.Duration
import java.time.Instant
import kotlin.math.ceil

internal fun tripCardUi(
    value: TripListValue,
    detail: TripDetail?,
    now: Instant,
    preferences: TripsPreferences,
    language: Language,
    formatter: CopyLineFormatter,
): TripCardUi {
    val card = value.card
    val options = CopyFormat(language, preferences.timeFormat)
    val next =
        if (value.status == TripStatus.Active && detail != null) {
            todayView(detail, detail.checkState.marks, now, preferences.hidden).nextStep?.let { block ->
                val start = planDate(block.startUtc)
                val minutes =
                    start?.let {
                        ceil(Duration.between(now, it).toMillis() / 60_000.0).coerceAtLeast(0.0)
                    } ?: 0.0
                val label =
                    if (start != null && start <= now) {
                        NextLabel.Now
                    } else {
                        NextLabel.In(formatter.formatMinutes(minutes, language, duration = true))
                    }
                NextStepUi(block.type, label, formatter.format(block.text, options.copy(displayZone = block.displayTz)))
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
