package net.chronoflow.core.datamock

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.model.api.ApiGuestTripReviewInput
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.testing.Fixtures

internal fun wizardInput(): ApiTripInput =
    ApiJson.response.decodeFromString(ApiTripInput.serializer(), Fixtures.text("wizard/input.json"))

internal fun guestReviewInput(): ApiGuestTripReviewInput {
    val request = ApiJson.response.parseToJsonElement(Fixtures.text("wizard/guest-review-request.json")).jsonObject
    return ApiGuestTripReviewInput(
        wizardInput(),
        profile = request.getValue("profile").jsonObject,
        deviceID = request.getValue("device_id").string!!,
    )
}

internal fun wizardProfile(): JsonObject =
    JsonObject(
        mapOf(
            "age_band" to JsonPrimitive("18_64"),
            "shift_work" to JsonPrimitive(false),
            "exclude_caffeine" to JsonPrimitive(false),
            "flight_movement_breaks" to JsonPrimitive(false),
        ),
    )

internal fun wizardMockSource(store: MockStateStore = MemoryMockStateStore()): MockDataSource =
    mockSource(
        store = store,
        seed =
            scenarioState().updateAccount(demoID) {
                it.copy(
                    snapshot =
                        it.snapshot.with(
                            settings =
                                it.snapshot.settings.with(
                                    planCredits =
                                        JsonObject(
                                            mapOf("credits" to JsonPrimitive(3), "unlimited" to JsonPrimitive(false)),
                                        ),
                                ),
                        ),
                )
            },
    )
