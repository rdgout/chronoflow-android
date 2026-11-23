package net.chronoflow.core.plan

import net.chronoflow.core.model.contracts.TripStatus

data class TripSection(
    val status: TripStatus,
    val trips: List<TripListValue>,
)

fun tripSections(trips: List<TripListValue>): List<TripSection> =
    listOf(TripStatus.Active, TripStatus.Upcoming, TripStatus.Completed).mapNotNull { status ->
        val values =
            trips.filter { it.status == status }.sortedWith(
                compareByDescending<TripListValue> { planDate(it.card.departureUtc) }.thenByDescending { it.card.id },
            )
        if (values.isEmpty()) null else TripSection(status, values)
    }
