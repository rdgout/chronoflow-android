package net.chronoflow.core.wizard.model

import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import java.time.Instant

sealed interface AirportSearchState {
    data object Hint : AirportSearchState

    data class Searching(
        val query: String,
        val stale: List<ApiAirportSuggestion>,
    ) : AirportSearchState

    data class Results(
        val query: String,
        val airports: List<ApiAirportSuggestion>,
    ) : AirportSearchState

    data class Empty(
        val query: String,
    ) : AirportSearchState

    data class Failed(
        val query: String,
    ) : AirportSearchState

    data class Throttled(
        val query: String,
        val retryAt: Instant,
    ) : AirportSearchState
}
