@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import mockwebserver3.MockResponse
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.network.support.MockApi
import net.chronoflow.core.network.support.RecordingDeadline
import net.chronoflow.core.network.support.failure
import net.chronoflow.core.network.support.json
import net.chronoflow.core.network.support.requestJson
import net.chronoflow.core.network.support.response
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class GuestTripReviewAPITests {
    @get:Rule val api = MockApi()

    @Test fun publicRouteDecodesMintedOrNullSecretMinted() = review("a".repeat(64))

    @Test fun publicRouteDecodesMintedOrNullSecretNull() = review(null)

    private fun review(secret: String?) =
        runTest(timeout = 60.seconds) {
            val raw = fixtureObject("wizard/review.json")
            val identity = raw.getValue("identity").jsonObject
            val reply =
                JsonObject(
                    raw +
                        (
                            "identity" to
                                JsonObject(
                                    identity + ("guestSecret" to (secret?.let { JsonPrimitive(it) } ?: JsonNull)),
                                )
                        ),
                )
            api.reply("POST", "/v1/guest/trips/review", 200, reply.toString())
            val deadline = RecordingDeadline()
            val result = ApiDataSource(api.client(deadline = deadline)).reviewGuestTrip(guestReviewInput())
            assertThat(result.identity.guestSecret).isEqualTo(secret)
            assertThat(api.requests.single().headers["Authorization"]).isEqualTo(null)
            assertThat(requestJson(api.requests.single())).isEqualTo(fixtureObject("wizard/guest-review-request.json"))
            assertThat(deadline.durations.single()).isEqualTo(90.seconds)
            assertThat(api.server.requestCount).isEqualTo(1)
        }

    @Test fun mapsGuestFailuresExpired() =
        fails(422, """{"code":"review_expired","message":"Review expired."}""", DataFailure.ReviewExpired)

    @Test fun mapsGuestFailuresValidationExpired() = fails(422, json("wizard/expired.json"), DataFailure.ReviewExpired)

    @Test fun mapsGuestFailuresThrottled() = fails(429, json("wizard/throttled.json"), DataFailure.Throttled(42.0, 429))

    @Test fun mapsGuestFailuresBare429() = fails(429, "", DataFailure.Throttled(17.0, 429))

    @Test fun mapsGuestFailuresProfile() =
        fails(
            422,
            """{"code":"validation_failed","message":"Invalid.","errors":{"profile.habitual_bed":"Check your bedtime and wake-up time."}}""",
            DataFailure.ProfileInvalid(
                mapOf("profile.habitualBed" to FieldMessage("Check your bedtime and wake-up time.")),
            ),
        )

    private fun fails(
        status: Int,
        body: String,
        expected: DataFailure,
    ) = runTest(timeout = 60.seconds) {
        api.route("POST", "/v1/guest/trips/review") {
            MockResponse
                .Builder()
                .code(status)
                .body(body)
                .addHeader("Retry-After", "17")
                .build()
        }
        assertThat(failure { ApiDataSource(api.client()).reviewGuestTrip(guestReviewInput()) }).isEqualTo(expected)
        assertThat(api.server.requestCount).isEqualTo(1)
    }
}
