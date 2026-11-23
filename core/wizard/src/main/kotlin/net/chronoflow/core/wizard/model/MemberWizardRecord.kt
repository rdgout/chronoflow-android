package net.chronoflow.core.wizard.model

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiJson

object MemberWizardRecord {
    private val recoveryJson = Json(ApiJson.response) { encodeDefaults = true }

    const val ID = "member-wizard"
    const val EXPIRY_MILLIS = 1_800_000L

    data class Payload(
        val form: String,
        val state: String,
        val fieldErrors: String,
    )

    fun isExpired(
        updatedAtMillis: Long,
        nowMillis: Long,
    ): Boolean = nowMillis - updatedAtMillis >= EXPIRY_MILLIS

    fun encode(draft: MemberWizardDraft): Payload {
        val form = withoutGuestSecret(draft.form)
        val cleaned = draft.state.discardRetiredValues()
        val state =
            cleaned.copy(
                review = cleaned.review?.let { it.copy(identity = it.identity.copy(guestSecret = null)) },
                originalForm = cleaned.originalForm?.let(::withoutGuestSecret),
                sourceChange = cleaned.sourceChange?.let { it.copy(form = withoutGuestSecret(it.form)) },
            )
        return Payload(
            recoveryJson.encodeToString(form),
            recoveryJson.encodeToString(state),
            recoveryJson.encodeToString(draft.fieldErrors),
        )
    }

    fun decode(payload: Payload): MemberWizardDraft? =
        try {
            val form = recoveryJson.decodeFromString<WizardForm>(payload.form)
            var state = recoveryJson.decodeFromString<MemberWizardState>(payload.state).discardRetiredValues()
            if (form.legs.size !in 1..WizardForm.maxFlights || form.stays.size != form.legs.size ||
                form.returnBoundary !in 1..form.legs.size
            ) {
                null
            } else {
                if (state.step ==
                    MemberWizardStep.continuation
                ) {
                    state =
                        state.copy(
                            step =
                                if (state.review?.prompts?.any { !it.blocking } ==
                                    true
                                ) {
                                    MemberWizardStep.fineTune
                                } else {
                                    MemberWizardStep.ready
                                },
                        )
                }
                if (state.step == MemberWizardStep.returnFlight &&
                    state.returnRouteConfirmed != true
                ) {
                    state = state.copy(step = MemberWizardStep.returnRoute)
                }
                MemberWizardDraft(
                    form,
                    state,
                    recoveryJson.decodeFromString<Map<String, FieldMessage>>(payload.fieldErrors),
                )
            }
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun withoutGuestSecret(form: WizardForm): WizardForm =
        form.copy(identity = form.identity?.copy(guestSecret = null))
}
