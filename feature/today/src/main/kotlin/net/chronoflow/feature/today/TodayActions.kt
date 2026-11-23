package net.chronoflow.feature.today

data class TodayActions(
    val openTrip: (String) -> Unit,
    val settings: () -> Unit,
    val pastTrips: () -> Unit,
    val create: () -> Unit,
    val refresh: suspend () -> Unit,
)
