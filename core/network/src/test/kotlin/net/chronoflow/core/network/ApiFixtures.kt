@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.chronoflow.core.model.api.ApiGuestTripReviewInput
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskeyAssertion
import net.chronoflow.core.model.api.ApiPasskeyAssertionResponse
import net.chronoflow.core.model.api.ApiPasskeyLoginInput
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.api.ApiTripSaveInput
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.core.model.api.CredentialType
import net.chronoflow.core.model.api.RegisterInput
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.model.contracts.ApiTripReview
import net.chronoflow.core.model.contracts.ApiTripSaveResult
import net.chronoflow.core.network.support.json

internal fun fixtureObject(name: String): JsonObject = ApiJson.response.parseToJsonElement(json(name)) as JsonObject

internal fun wizardInput(): ApiTripInput =
    ApiJson.response.decodeFromString(ApiTripInput.serializer(), json("wizard/input.json"))

internal fun guestReviewInput(): ApiGuestTripReviewInput {
    val body = fixtureObject("wizard/guest-review-request.json")
    return ApiGuestTripReviewInput(
        wizardInput(),
        profile = body.getValue("profile") as JsonObject,
        deviceID = body.getValue("device_id").jsonPrimitive.content,
    )
}

internal fun assertion() =
    ApiPasskeyAssertion("AA", "AA==", CredentialType.PublicKey, ApiPasskeyAssertionResponse("AA", "AA", "AA"))

internal fun passkeyInput() = ApiPasskeyLoginInput("fake", assertion(), "test-device", "Phone")

internal fun registration() =
    RegisterInput("Test", "test@example.com", "fake", "fake", "test-device", "Phone", AuthLanguage.Nl)

internal fun saveInput(): ApiTripSaveInput =
    ApiTripSaveInput.from(
        ApiJson.response.decodeFromString(ApiTripReview.serializer(), json("wizard/review.json")).identity,
    )

internal fun testSource(client: ApiClient): ApiDataSource =
    object : ApiDataSource(client) {
        override suspend fun searchAirports(
            query: String,
            limit: Int?,
        ): List<ApiAirportSuggestion> = error("unused")

        override suspend fun reviewTrip(input: ApiTripReviewInput): ApiTripReview = error("unused")

        override suspend fun reviewTripEdit(
            input: ApiTripReviewInput,
            id: String,
            sourceRevision: String,
        ): ApiTripReview = error("unused")

        override suspend fun saveTrip(identity: ApiTripSaveInput): ApiTripSaveResult = error("unused")

        override suspend fun updateTrip(
            id: String,
            identity: ApiTripSaveInput,
            sourceRevision: String,
        ): ApiTripSaveResult = error("unused")

        override suspend fun tripInput(id: String): ApiTripInputSnapshot = error("unused")
    }
