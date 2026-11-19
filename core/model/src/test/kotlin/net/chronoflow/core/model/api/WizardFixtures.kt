package net.chronoflow.core.model.api

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.model.contracts.fixtureObject
import net.chronoflow.core.testing.Fixtures

internal fun wizardInput(): ApiTripInput =
    ApiJson.response.decodeFromString(ApiTripInput.serializer(), Fixtures.text("wizard/input.json"))

internal fun guestReviewInput(): ApiGuestTripReviewInput {
    val body = fixtureObject("wizard/guest-review-request.json")
    return ApiGuestTripReviewInput(
        input = wizardInput(),
        profile = body["profile"] as JsonObject,
        deviceID = (body["device_id"] as JsonPrimitive).content,
    )
}
