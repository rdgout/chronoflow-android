package net.chronoflow.core.model.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.model.contracts.ApiTripInputBusy
import net.chronoflow.core.model.contracts.ApiTripInputJourney
import net.chronoflow.core.model.contracts.ObjectSerializer
import net.chronoflow.core.model.contracts.ProfilePreferences

@Serializable(with = ApiGuestTripReviewInput.Serializer::class)
data class ApiGuestTripReviewInput(
    val input: ApiTripInput,
    val identity: ApiReviewIdentity? = null,
    val profile: JsonObject,
    val deviceID: String,
    val guestSecret: String? = null,
) {
    override fun toString(): String =
        "ApiGuestTripReviewInput(input=$input, identity=$identity, profile=$profile, deviceID=$deviceID, " +
            "guestSecret=${redacted(guestSecret)})"

    object Serializer : ObjectSerializer<ApiGuestTripReviewInput>(
        "net.chronoflow.core.model.api.ApiGuestTripReviewInput",
    ) {
        override fun decode(
            json: Json,
            obj: JsonObject,
        ): ApiGuestTripReviewInput = throw SerializationException("ApiGuestTripReviewInput is encode-only")

        override fun encode(
            json: Json,
            value: ApiGuestTripReviewInput,
        ): JsonObject {
            val input = value.input
            val identity = value.identity
            val body = mutableMapOf<String, JsonElement>()
            body["outbound"] = json.encodeToJsonElement(ApiTripInputJourney.serializer(), input.outbound)
            body["trip_end_kind"] = JsonPrimitive("one_way")
            body["preparation_days"] = JsonPrimitive(input.preparationDays)
            body["fast_delay_opt_in"] = JsonPrimitive(input.fastDelayOptIn)
            body["latest_wake"] = input.latestWake?.let { JsonPrimitive(it) } ?: JsonNull
            body["feasible_night_bed"] = input.feasibleNightBed?.let { JsonPrimitive(it) } ?: JsonNull
            body["feasible_night_wake"] = input.feasibleNightWake?.let { JsonPrimitive(it) } ?: JsonNull
            body["pre_departure_busy_answered"] = JsonPrimitive(input.preDepartureBusyAnswered)
            body["pre_departure_busy_intervals"] =
                json.encodeToJsonElement(ListSerializer(ApiTripInputBusy.serializer()), input.preDepartureBusyIntervals)
            body["draft_token"] = identity?.token?.let { JsonPrimitive(it) } ?: JsonNull
            body["input_revision"] = JsonPrimitive(identity?.inputRevision ?: 0)
            body["review_revision"] = identity?.reviewRevision?.let { JsonPrimitive(it) } ?: JsonNull
            body["profile"] = ProfilePreferences.request(value.profile)
            body["device_id"] = JsonPrimitive(value.deviceID)
            value.guestSecret?.let { body["guest_secret"] = JsonPrimitive(it) }
            return JsonObject(body)
        }
    }
}
