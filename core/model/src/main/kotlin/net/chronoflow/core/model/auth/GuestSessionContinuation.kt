package net.chronoflow.core.model.auth

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import net.chronoflow.core.model.api.ApiJson

sealed interface GuestSessionContinuation {
    data class Trip(
        val tripId: String,
    ) : GuestSessionContinuation

    data object MemberWizard : GuestSessionContinuation

    data object Trips : GuestSessionContinuation

    fun encode(): String =
        JsonObject(
            when (this) {
                is Trip -> mapOf("kind" to JsonPrimitive("trip"), "tripId" to JsonPrimitive(tripId))
                MemberWizard -> mapOf("kind" to JsonPrimitive("memberWizard"))
                Trips -> mapOf("kind" to JsonPrimitive("trips"))
            },
        ).toString()

    companion object {
        fun decode(json: String): GuestSessionContinuation? {
            val value =
                try {
                    ApiJson.response.parseToJsonElement(json) as? JsonObject
                } catch (_: SerializationException) {
                    null
                } ?: return null
            return when ((value["kind"] as? JsonPrimitive)?.contentOrNull) {
                "trip" -> {
                    (value["tripId"] as? JsonPrimitive)
                        ?.takeIf { it.isString }
                        ?.contentOrNull
                        ?.takeIf { it.isNotBlank() }
                        ?.let(::Trip)
                }

                "memberWizard" -> {
                    MemberWizard
                }

                "trips" -> {
                    Trips
                }

                else -> {
                    null
                }
            }
        }
    }
}
