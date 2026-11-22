package net.chronoflow.app.perf

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardState
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardDateTime
import net.chronoflow.core.wizard.model.WizardForm
import net.chronoflow.core.wizard.model.setRouteAirport
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

object PerfWizardJourney {
    fun apply(
        base: MemberWizardDraft,
        locale: Locale,
        now: Instant,
    ): MemberWizardDraft {
        val capture = returnDraft(locale)
        val airport =
            ApiAirportSuggestion(
                "HND",
                "Tokyo Haneda Airport",
                capture.state.airports
                    .getValue("NRT")
                    .city,
                "JP",
                "Asia/Tokyo",
            )
        val departure = now.plusSeconds(30 * 86_400L)
        val form =
            capture.form.setRouteAirport(airport, 1).updateLeg(0) {
                it.copy(
                    departLocal = WizardDateTime.string(departure.atZone(ZoneOffset.UTC).toLocalDateTime()),
                    arriveLocal =
                        WizardDateTime.string(
                            departure.plusSeconds(18 * 3_600L).atZone(ZoneOffset.UTC).toLocalDateTime(),
                        ),
                )
            }
        return base.copy(
            form = form,
            state =
                capture.state.copy(
                    instance = base.state.instance,
                    revision = base.state.revision + 1,
                    step = MemberWizardStep.returnChoice,
                    airports = capture.state.airports + (airport.iata to airport),
                    firstTrip = base.state.firstTrip,
                    profile = base.state.profile,
                    savedProfile = base.state.savedProfile,
                    sleepNeeded = base.state.sleepNeeded,
                ),
        )
    }

    private fun returnDraft(locale: Locale): MemberWizardDraft {
        val airports =
            listOf(
                ApiAirportSuggestion("AMS", "Amsterdam Airport Schiphol", "Amsterdam", "NL", "Europe/Amsterdam"),
                ApiAirportSuggestion(
                    "NRT",
                    "Narita International",
                    if (locale.language ==
                        "nl"
                    ) {
                        "Tokio"
                    } else {
                        "Tokyo"
                    },
                    "JP",
                    "Asia/Tokyo",
                ),
                ApiAirportSuggestion("DXB", "Dubai International", "Dubai", "AE", "Asia/Dubai"),
                ApiAirportSuggestion("BKK", "Suvarnabhumi", "Bangkok", "TH", "Asia/Bangkok"),
            )
        val form =
            WizardForm()
                .setRouteAirport(airports[0], 0)
                .setRouteAirport(airports[1], 1)
                .updateLeg(0) { it.copy(departLocal = "2026-10-12T14:40", arriveLocal = "2026-10-13T09:15") }
        val state =
            MemberWizardState(
                entered = true,
                fromLastTrip = false,
                sleepNeeded = false,
                step = MemberWizardStep.returnChoice,
                airports = airports.associateBy { it.iata },
                profile =
                    JsonObject(
                        mapOf(
                            "habitual_bed" to JsonPrimitive("23:30"),
                            "habitual_wake" to JsonPrimitive("07:15"),
                            "chronotype" to JsonPrimitive("intermediate"),
                        ),
                    ),
            )
        return MemberWizardDraft(form, state)
    }
}
