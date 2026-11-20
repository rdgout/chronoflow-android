package net.chronoflow.core.wizard.model

import kotlinx.serialization.Serializable
import net.chronoflow.core.model.contracts.ApiAirportSuggestion

@Serializable
data class WizardRouteAirport(
    val iata: String,
    val city: String?,
)

fun WizardForm.route(home: Boolean = false): List<WizardRouteAirport> {
    val flights = if (home) legs.drop(returnBoundary) else legs.take(returnBoundary)
    val first = flights.firstOrNull() ?: return emptyList()
    return listOf(WizardRouteAirport(first.fromIata, first.fromCity)) +
        flights.map { WizardRouteAirport(it.toIata, it.toCity) }
}

fun WizardForm.routeComplete(home: Boolean = false): Boolean {
    val airports = route(home)
    return airports.size > 1 && airports.none { it.iata.isEmpty() } && airports.first().iata != airports.last().iata &&
        airports.zipWithNext().all { (a, b) -> a.iata != b.iata }
}

fun WizardForm.setRouteAirport(
    airport: ApiAirportSuggestion,
    at: Int,
    home: Boolean = false,
): WizardForm {
    val airports = route(home)
    if (at !in airports.indices || (home && at <= 0)) return this
    val start = if (home) returnBoundary else 0
    var result = this
    if (at > 0) result = result.updateLeg(start + at - 1) { it.copy(toIata = airport.iata, toCity = airport.city) }
    if (at <
        airports.size - 1
    ) {
        result =
            result.updateLeg(start + at) { it.copy(fromIata = airport.iata, fromCity = airport.city) }
    }
    return result
}

fun WizardForm.makeDirect(home: Boolean = false): WizardForm {
    val start = if (home) returnBoundary else 0
    var result = this
    while ((if (home) result.legs.size - result.returnBoundary else result.returnBoundary) > 1) {
        result = result.removeStop(start + 1)
    }
    return result
}
