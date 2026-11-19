package net.chronoflow.core.plan

import net.chronoflow.core.model.contracts.TripStatus
import java.time.Instant

fun selectTodayTrip(
    trips: List<TripListValue>,
    now: Instant,
): TripListValue? =
    trips
        .filter { status(it, now) != TripStatus.Completed }
        .sortedWith { a, b ->
            val activeA = status(a, now) == TripStatus.Active
            val activeB = status(b, now) == TripStatus.Active
            if (activeA != activeB) return@sortedWith if (activeA) -1 else 1
            val startA =
                planDate(
                    if (activeA) a.planEndUtc ?: a.card.arrivalUtc else a.planStartUtc ?: a.card.departureUtc,
                )
            val startB =
                planDate(
                    if (activeB) b.planEndUtc ?: b.card.arrivalUtc else b.planStartUtc ?: b.card.departureUtc,
                )
            if (startA == startB) a.card.id.compareTo(b.card.id) else compareValues(startA, startB)
        }.firstOrNull()

fun lastEndedTrip(
    trips: List<TripListValue>,
    now: Instant,
): TripListValue? =
    trips
        .filter { status(it, now) == TripStatus.Completed }
        .sortedWith { a, b ->
            val endA = planDate(a.planEndUtc ?: a.card.arrivalUtc)?.coerceAtMost(now)
            val endB = planDate(b.planEndUtc ?: b.card.arrivalUtc)?.coerceAtMost(now)
            if (endA == endB) a.card.id.compareTo(b.card.id) else compareValues(endB, endA)
        }.firstOrNull()

private fun status(
    trip: TripListValue,
    now: Instant,
): TripStatus = tripDisplayStatus(trip.card, trip.planStartUtc, trip.planEndUtc, now)
