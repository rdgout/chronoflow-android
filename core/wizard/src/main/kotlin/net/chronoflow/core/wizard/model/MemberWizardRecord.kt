package net.chronoflow.core.wizard.model

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiJson

object MemberWizardRecord {
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
        val form = draft.form.copy(identity = draft.form.identity?.copy(guestSecret = null))
        val cleaned = draft.state.discardRetiredValues()
        val state =
            cleaned.copy(
                review = cleaned.review?.let { it.copy(identity = it.identity.copy(guestSecret = null)) },
            )
        return Payload(
            ApiJson.response.encodeToString(form),
            ApiJson.response.encodeToString(state),
            ApiJson.response.encodeToString(draft.fieldErrors),
        )
    }

    fun decode(payload: Payload): MemberWizardDraft? =
        try {
            val form = ApiJson.response.decodeFromString<WizardForm>(payload.form)
            var state = ApiJson.response.decodeFromString<MemberWizardState>(payload.state).discardRetiredValues()
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
                    ApiJson.response.decodeFromString<Map<String, FieldMessage>>(payload.fieldErrors),
                )
            }
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
}
