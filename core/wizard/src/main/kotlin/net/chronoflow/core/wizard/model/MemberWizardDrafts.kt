package net.chronoflow.core.wizard.model

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.AirportRef
import net.chronoflow.core.model.contracts.ApiAirportSuggestion

object MemberWizardDrafts {
    fun memberInitial(
        hasTrip: Boolean,
        latestOrigin: AirportRef?,
        savedProfile: JsonObject?,
        sleepAnswered: Boolean?,
    ): MemberWizardDraft {
        var draft =
            MemberWizardDraft(
                state =
                    MemberWizardState(
                        firstTrip = !hasTrip,
                        savedProfile = savedProfile,
                        sleepNeeded =
                            sleepAnswered != true,
                    ),
            )
        if (latestOrigin != null) {
            val airport =
                ApiAirportSuggestion(
                    latestOrigin.iata,
                    latestOrigin.city ?: latestOrigin.iata,
                    latestOrigin.city,
                    "",
                    latestOrigin.zone,
                )
            draft =
                draft.copy(
                    form =
                        draft.form.updateLeg(
                            0,
                        ) { it.copy(fromIata = latestOrigin.iata, fromCity = latestOrigin.city) },
                    state =
                        draft.state.copy(
                            airports = mapOf(latestOrigin.iata to airport),
                            fromLastTrip = true,
                            firstTrip = false,
                        ),
                )
        }
        if (draft.state.sleepNeeded && savedProfile != null) {
            val profile =
                listOf(
                    "habitual_bed" to "habitualBed",
                    "habitual_wake" to "habitualWake",
                    "chronotype" to "chronotype",
                ).mapNotNull { (field, key) ->
                    savedProfile[key]?.takeIf { it != JsonNull }?.let {
                        field to
                            it
                    }
                }.toMap()
            draft = draft.copy(state = draft.state.copy(profile = JsonObject(profile)))
        }
        return draft
    }

    fun seeded(
        seed: MemberWizardDraft,
        initial: MemberWizardDraft,
    ): MemberWizardDraft =
        initial.copy(
            form = seed.form.copy(identity = null, override = WizardSleepOverride()),
            state =
                initial.state.copy(
                    airports = seed.airports,
                    entered = true,
                    fromLastTrip = false,
                    step = MemberWizardStep.checking,
                    returnToReady = true,
                    profile = JsonObject(emptyMap()),
                ),
        )

    fun guest(): MemberWizardDraft =
        MemberWizardDraft(
            state =
                MemberWizardState(
                    mode = WizardMode.guest,
                    lastInputStep = MemberWizardStep.sleep,
                    firstTrip = false,
                    profile =
                        JsonObject(
                            mapOf("habitual_bed" to JsonPrimitive("23:00"), "habitual_wake" to JsonPrimitive("07:00")),
                        ),
                ),
        )
}
