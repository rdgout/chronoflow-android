package net.chronoflow.feature.trip.page

data class TripPageArgs(
    val tripId: String,
    val dayId: String? = null,
    val blockId: String? = null,
    val startAtSummary: Boolean = false,
    val awaitingSync: Boolean = false,
)

data class ScrollCommand(
    val dayId: String,
    val offset: Double,
    val animated: Boolean,
)
