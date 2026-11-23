package net.chronoflow.feature.wizard.debug

import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardDateTime
import net.chronoflow.core.wizard.model.setRouteAirport
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

object WizardSaveJourney {
    fun apply(
        base: MemberWizardDraft,
        locale: Locale,
        now: Instant,
    ): MemberWizardDraft {
        val capture = WizardBoardCaptureSeed.draft("WizardReturn", locale)
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
}
