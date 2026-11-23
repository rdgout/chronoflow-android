package net.chronoflow.feature.trips

import androidx.compose.runtime.Immutable
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.ui.trip.TripCardUi

@Immutable
sealed interface TripsUiState {
    @Immutable data object Loading : TripsUiState

    @Immutable data object Empty : TripsUiState

    @Immutable data class Content(
        val sections: List<TripSectionUi>,
    ) : TripsUiState
}

@Immutable
data class TripSectionUi(
    val status: TripStatus,
    val trips: List<TripCardUi>,
)
