package net.chronoflow.feature.trips

import androidx.compose.runtime.Immutable
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.TripStatus

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

@Immutable
data class TripCardUi(
    val id: String,
    val status: TripStatus,
    val flightNumber: String?,
    val offset: String,
    val from: AirportUi,
    val to: AirportUi,
    val times: String,
    val next: NextStepUi?,
)

@Immutable
data class AirportUi(
    val iata: String,
    val city: String?,
)

@Immutable
data class NextStepUi(
    val type: BlockType,
    val label: NextLabel,
    val text: String,
)

@Immutable
sealed interface NextLabel {
    @Immutable data object Now : NextLabel

    @Immutable data class In(
        val duration: String,
    ) : NextLabel
}
