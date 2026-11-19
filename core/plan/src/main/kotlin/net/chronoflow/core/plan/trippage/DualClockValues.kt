package net.chronoflow.core.plan.trippage

import net.chronoflow.core.model.contracts.AirportRef
import net.chronoflow.core.model.contracts.DayView
import net.chronoflow.core.plan.CopyFormat
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.planDate
import java.time.Instant

enum class ClockLabel { Home, There, YoureHere, HomeTime, DestinationTime }

data class DualClockValue(
    val label: ClockLabel,
    val place: String,
    val time: String,
    val active: Boolean,
)

fun activeZone(
    days: List<DayView>,
    now: Instant,
): String? {
    for (day in days) {
        for (zone in day.zones) {
            val start = planDate(zone.startUtc) ?: continue
            val end = planDate(zone.endUtc) ?: continue
            if (start <= now && now < end) return zone.tz
        }
    }
    return null
}

fun dualClockValues(
    from: AirportRef,
    to: AirportRef,
    now: Instant,
    activeZone: String?,
    remote: Boolean,
    formatter: CopyLineFormatter,
    options: CopyFormat,
): List<DualClockValue> =
    listOf(from, to).mapIndexed { index, place ->
        val active = place.zone == activeZone && (index == 0 || from.zone != to.zone)
        val label =
            when {
                remote -> if (index == 0) ClockLabel.Home else ClockLabel.There
                active -> ClockLabel.YoureHere
                index == 0 -> ClockLabel.HomeTime
                else -> ClockLabel.DestinationTime
            }
        DualClockValue(
            label,
            place.city ?: place.iata,
            formatter.formatInstant(now.toString(), options, place.zone),
            active,
        )
    }
