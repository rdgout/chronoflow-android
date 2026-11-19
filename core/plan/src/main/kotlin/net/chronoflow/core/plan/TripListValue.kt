package net.chronoflow.core.plan

import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripStatus

data class TripListValue(
    val card: TripCardView,
    val planStartUtc: String?,
    val planEndUtc: String?,
    val status: TripStatus,
    val planStartZone: String? = null,
    val planEndDate: String? = null,
    val preparationDays: Int? = null,
)

data class TripsDecodeResult(
    val values: List<TripListValue>,
    val error: Throwable?,
)
