package net.chronoflow.core.wizard

import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.testing.Fixtures
import java.time.Instant
import kotlin.math.roundToLong

fun wizardAirport(
    iata: String,
    city: String,
    zone: String,
): ApiAirportSuggestion = ApiAirportSuggestion(iata = iata, name = city, city = city, country = "", timezone = zone)

fun generationInput(): ApiTripInput = ApiJson.response.decodeFromString(Fixtures.text("wizard/input.json"))

fun generationReview(): ApiTripReview = ApiJson.response.decodeFromString(Fixtures.text("wizard/review.json"))

class WizardTestClock {
    var now: Instant = Instant.ofEpochSecond(1_800_000_000)

    fun advance(seconds: Double) {
        now = now.plusMillis((seconds * 1000).roundToLong())
    }
}
