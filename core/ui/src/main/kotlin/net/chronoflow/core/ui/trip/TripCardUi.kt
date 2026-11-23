package net.chronoflow.core.ui.trip

import androidx.compose.runtime.Immutable
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.TripStatus
import java.time.Instant

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
    val startUtc: Instant,
    val text: String,
)
