@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.feature.wizard.debug

import net.chronoflow.core.model.contracts.ApiAirportSuggestion

class FixedAirportSearch(
    board: String = "WizardStopSearch",
) {
    init {
        require(WizardBoardCaptureSeed.knows(board)) { "Unknown wizard board" }
    }

    fun results(query: String): List<ApiAirportSuggestion> =
        when {
            query.trim().startsWith("Am", ignoreCase = true) -> {
                listOf(
                    ApiAirportSuggestion("AMS", "Amsterdam Airport Schiphol", "Amsterdam", "NL", "Europe/Amsterdam"),
                    ApiAirportSuggestion("AMM", "Queen Alia International", "Amman", "JO", "Asia/Amman"),
                    ApiAirportSuggestion(
                        "AMA",
                        "Rick Husband Amarillo International",
                        "Amarillo",
                        "US",
                        "America/Chicago",
                    ),
                    ApiAirportSuggestion(
                        "AMD",
                        "Sardar Vallabhbhai Patel International",
                        "Ahmedabad",
                        "IN",
                        "Asia/Kolkata",
                    ),
                )
            }

            query.trim().startsWith("Du", ignoreCase = true) -> {
                listOf(
                    ApiAirportSuggestion("DXB", "Dubai International", "Dubai", "AE", "Asia/Dubai"),
                    ApiAirportSuggestion("DWC", "Al Maktoum International", "Dubai", "AE", "Asia/Dubai"),
                    ApiAirportSuggestion("DUS", "Düsseldorf Airport", "Düsseldorf", "DE", "Europe/Berlin"),
                    ApiAirportSuggestion("DUB", "Dublin Airport", "Dublin", "IE", "Europe/Dublin"),
                )
            }

            query.trim().startsWith("Ban", ignoreCase = true) -> {
                listOf(
                    ApiAirportSuggestion("BKK", "Suvarnabhumi Airport", "Bangkok", "TH", "Asia/Bangkok"),
                    ApiAirportSuggestion("DMK", "Don Mueang International", "Bangkok", "TH", "Asia/Bangkok"),
                )
            }

            else -> {
                emptyList()
            }
        }
}
