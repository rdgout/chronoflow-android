package net.chronoflow.feature.trip.actions

import androidx.compose.runtime.Immutable
import net.chronoflow.core.plan.actions.TripActionsAvailability
import net.chronoflow.core.plan.actions.TripActionsHeader

@Immutable
data class TripActionsUi(
    val tripId: String,
    val header: TripActionsHeader,
    val availability: TripActionsAvailability,
)
