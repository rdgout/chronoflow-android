package net.chronoflow.core.model.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.contracts.ApiReviewItinerary
import net.chronoflow.core.model.contracts.ApiReviewPrice
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.model.contracts.ObjectSerializer
import net.chronoflow.core.model.contracts.ProfilePreferences

typealias ApiTripPrice = ApiReviewPrice
typealias ApiTripItinerary = ApiReviewItinerary

@Serializable(with = ApiTripReviewInput.Serializer::class)
data class ApiTripReviewInput(
    val input: ApiTripInput,
    val identity: ApiReviewIdentity? = null,
    val profile: JsonObject? = null,
) {
    object Serializer : ObjectSerializer<ApiTripReviewInput>("net.chronoflow.core.model.api.ApiTripReviewInput") {
        override fun decode(
            json: Json,
            obj: JsonObject,
        ): ApiTripReviewInput = throw SerializationException("ApiTripReviewInput is encode-only")

        override fun encode(
            json: Json,
            value: ApiTripReviewInput,
        ): JsonObject {
            val identity = value.identity
            val body = json.encodeToJsonElement(ApiTripInput.serializer(), value.input) as JsonObject
            val extra = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
            extra["draft_token"] = identity?.token?.let { JsonPrimitive(it) } ?: JsonNull
            extra["input_revision"] = JsonPrimitive(identity?.inputRevision ?: 0)
            extra["review_revision"] = identity?.reviewRevision?.let { JsonPrimitive(it) } ?: JsonNull
            identity?.sourceRevision?.let { extra["source_revision"] = JsonPrimitive(it) }
            value.profile?.let { extra["profile"] = ProfilePreferences.request(it) }
            return JsonObject(body + extra)
        }
    }
}

@Serializable
data class ApiTripSaveInput(
    @SerialName("draft_token") val draftToken: String,
    @SerialName("input_revision") val inputRevision: Int,
    @SerialName("review_revision") val reviewRevision: Int,
) {
    companion object {
        fun from(identity: ApiReviewIdentity): ApiTripSaveInput {
            val revision = identity.reviewRevision
            if (identity.token.isEmpty() || identity.inputRevision <= 0 || revision == null || revision <= 0) {
                throw DataFailure.ReviewExpired
            }
            return ApiTripSaveInput(identity.token, identity.inputRevision, revision)
        }
    }
}
