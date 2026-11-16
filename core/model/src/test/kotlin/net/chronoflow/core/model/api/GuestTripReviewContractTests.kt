package net.chronoflow.core.model.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.contracts.ApiTripInputOverride
import net.chronoflow.core.model.contracts.fixtureObject
import org.junit.Test

class GuestTripReviewContractTests {
    private fun encode(value: ApiGuestTripReviewInput): JsonObject =
        ApiJson.request.parseToJsonElement(
            ApiJson.request.encodeToString(ApiGuestTripReviewInput.serializer(), value),
        ) as JsonObject

    @Test
    fun firstCallMatchesGuestRequestRules() {
        val base = guestReviewInput()
        val input =
            base.input.copy(
                `return` = base.input.outbound,
                tripEndKind = "return_legs",
                tripEndNights = 7,
                override = ApiTripInputOverride(true, "22:00", "06:00", null),
            )
        val retired =
            listOf(
                "bipolar",
                "eye_disease",
                "eyeDisease",
                "pregnancy",
                "vte",
                "sleep_phase_disorder",
                "oral_contraception",
                "caffeine_ruled_out",
                "unrecognised",
                "invalidDrafts",
            )
        val profile = JsonObject(base.profile + retired.associateWith { JsonPrimitive(true) })
        val body = encode(base.copy(input = input, profile = profile))
        assertThat(body).isEqualTo(fixtureObject("wizard/guest-review-request.json"))
        assertThat(body.containsKey("return")).isEqualTo(false)
        assertThat(body.containsKey("override")).isEqualTo(false)
        assertThat((body["profile"] as JsonObject)["habitual_bed"]).isEqualTo(JsonPrimitive("23:00"))
        assertThat((body["profile"] as JsonObject)["habitual_wake"]).isEqualTo(JsonPrimitive("07:00"))
        assertThat(body["draft_token"]).isEqualTo(JsonNull)
        assertThat(body["input_revision"]).isEqualTo(JsonPrimitive(0))
        assertThat(body["review_revision"]).isEqualTo(JsonNull)
        assertThat(body.containsKey("guest_secret")).isEqualTo(false)
    }

    @Test
    fun subsequentIdentityAndSecretAreFlattened() {
        val secret = "a".repeat(64)
        val request =
            guestReviewInput().copy(
                identity = ApiReviewIdentity("draft", 2, 3, null, null),
                guestSecret = secret,
            )
        val body = encode(request)
        assertThat(body["draft_token"]).isEqualTo(JsonPrimitive("draft"))
        assertThat(body["input_revision"]).isEqualTo(JsonPrimitive(2))
        assertThat(body["review_revision"]).isEqualTo(JsonPrimitive(3))
        assertThat(body["guest_secret"]).isEqualTo(JsonPrimitive(secret))
        assertThat(body.containsKey("identity")).isEqualTo(false)
        assertThat(body.containsKey("source_revision")).isEqualTo(false)
    }
}
