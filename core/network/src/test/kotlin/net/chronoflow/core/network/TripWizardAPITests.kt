@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import mockwebserver3.MockResponse
import mockwebserver3.SocketEffect
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.model.api.ApiTripSaveInput
import net.chronoflow.core.network.support.MockApi
import net.chronoflow.core.network.support.RecordingDeadline
import net.chronoflow.core.network.support.failure
import net.chronoflow.core.network.support.json
import net.chronoflow.core.network.support.requestJson
import net.chronoflow.core.network.support.response
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TripWizardAPITests {
    @get:Rule val api = MockApi()

    @Test fun wireContractsRequestsAndResponses() =
        runTest(timeout = 60.seconds) {
            val source = ApiDataSource(api.client())
            api.reply("POST", "/v1/trips/review", 200, json("wizard/review.json"))
            api.reply("POST", "/v1/trips", 201, json("wizard/save.json"))
            api.reply("GET", "/v1/trips/trip/input", 200, json("wizard/snapshot.json"))
            api.reply("POST", "/v1/trips/trip/review", 200, json("wizard/review.json"))
            api.reply("PUT", "/v1/trips/trip", 200, json("wizard/save.json"))
            api.reply("GET", "/v1/airports", 200, json("wizard/airports.json"))
            val expected = fixtureObject("wizard/review-request.json")
            val review =
                source.reviewTrip(
                    ApiTripReviewInput(wizardInput(), profile = expected.getValue("profile").jsonObject),
                )
            assertThat(review.canSave).isEqualTo(true)
            assertThat(source.saveTrip(ApiTripSaveInput.from(review.identity)).tripId).isEqualTo(review.identity.token)
            val edit = source.tripInput("trip")
            assertThat(edit.input).isEqualTo(wizardInput())
            assertThat(
                edit.input.outbound.legs
                    .first()
                    .stay.bedAvailableMode,
            ).isEqualTo(null)
            source.reviewTripEdit(ApiTripReviewInput(edit.input), "trip", edit.sourceRevision)
            source.updateTrip("trip", saveInput(), "a".repeat(64))
            assertThat(
                source.searchAirports("São Paulo & city+airport", 7).first().timezone,
            ).isEqualTo("Europe/Amsterdam")
            assertThat(requestJson(api.requests[0])).isEqualTo(expected)
            assertThat(requestJson(api.requests[1])).isEqualTo(fixtureObject("wizard/save-request.json"))
            assertThat(requestJson(api.requests[3])).isEqualTo(
                JsonObject(
                    expected.filterKeys { it != "profile" } + ("source_revision" to JsonPrimitive(edit.sourceRevision)),
                ),
            )
            assertThat(requestJson(api.requests[4])).isEqualTo(
                JsonObject(
                    fixtureObject("wizard/save-request.json") + ("source_revision" to JsonPrimitive("a".repeat(64))),
                ),
            )
            assertThat(
                api.requests[5].url.encodedQuery,
            ).isEqualTo("limit=7&query=S%C3%A3o%20Paulo%20%26%20city%2Bairport")
            assertThat(api.requests[5].headers["Authorization"]).isEqualTo(null)
            assertThat(api.server.requestCount).isEqualTo(6)
        }

    @Test fun every429RetainsRetryDeadlineReview() = throttle("review")

    @Test fun every429RetainsRetryDeadlineSave() = throttle("save")

    @Test fun every429RetainsRetryDeadlineUpdate() = throttle("update")

    private fun throttle(operation: String) =
        runTest(timeout = 60.seconds) {
            val method = if (operation == "update") "PUT" else "POST"
            val path =
                when (operation) {
                    "review" -> "/v1/trips/review"
                    "save" -> "/v1/trips"
                    else -> "/v1/trips/trip"
                }
            api.route(
                method,
                path,
            ) {
                MockResponse
                    .Builder()
                    .code(429)
                    .body("not json")
                    .addHeader("Retry-After", "17")
                    .build()
            }
            val source = ApiDataSource(api.client())
            val failure =
                failure {
                    when (operation) {
                        "review" -> source.reviewTrip(ApiTripReviewInput(wizardInput()))
                        "save" -> source.saveTrip(saveInput())
                        else -> source.updateTrip("trip", saveInput(), "source")
                    }
                }
            assertThat(failure).isEqualTo(DataFailure.Throttled(17.0, 429))
            assertThat(api.server.requestCount).isEqualTo(1)
        }

    @Test fun bodyRetryHintSurvivesMissingEnvelopeCode() =
        runTest(timeout = 60.seconds) {
            api.reply("POST", "/v1/trips/review", 429, """{"retryAfterSeconds":23}""")
            assertThat(failure { ApiDataSource(api.client()).reviewTrip(ApiTripReviewInput(wizardInput())) })
                .isEqualTo(DataFailure.Throttled(23.0, 429))
        }

    @Test fun saveBodyThrottleAndEmailVerification() =
        runTest(timeout = 60.seconds) {
            api.route("POST", "/v1/trips") {
                MockResponse
                    .Builder()
                    .code(429)
                    .body(json("wizard/throttled.json"))
                    .addHeader("Retry-After", "99")
                    .build()
            }
            val source = ApiDataSource(api.client())
            assertThat(failure { source.saveTrip(saveInput()) }).isEqualTo(DataFailure.Throttled(42.0, 429))
            api.reply("POST", "/v1/trips/review", 403, json("errors/email_unverified.json"))
            assertThat(
                failure { source.reviewTrip(ApiTripReviewInput(wizardInput())) },
            ).isEqualTo(DataFailure.EmailUnverified(403))
        }

    @Test fun saveMapsDomainValidationCapacity() = domain("capacity", DataFailure.SaveCapacity)

    @Test fun saveMapsDomainValidationExpired() = domain("expired", DataFailure.ReviewExpired)

    @Test fun saveMapsDomainValidationCredits() = domain("credits", DataFailure.NotEnoughCredits)

    @Test fun saveMapsDomainValidationProfile() =
        runTest(timeout = 60.seconds) {
            api.reply("POST", "/v1/trips", 422, json("wizard/profile.json"))
            val failure = failure { ApiDataSource(api.client()).saveTrip(saveInput()) } as DataFailure.ProfileInvalid
            assertThat(failure.fields.containsKey("profile.habitual_bed")).isEqualTo(true)
            assertThat(api.server.requestCount).isEqualTo(1)
        }

    private fun domain(
        name: String,
        expected: DataFailure,
    ) = runTest(timeout = 60.seconds) {
        api.reply("POST", "/v1/trips", 422, json("wizard/$name.json"))
        assertThat(failure { ApiDataSource(api.client()).saveTrip(saveInput()) }).isEqualTo(expected)
        assertThat(api.server.requestCount).isEqualTo(1)
    }

    @Test fun transportAndDeadlineAreOfflineWithoutRetry() =
        runTest(timeout = 60.seconds) {
            api.route(
                "POST",
                "/v1/trips/review",
            ) { MockResponse.Builder().onResponseStart(SocketEffect.CloseSocket()).build() }
            assertThat(
                failure {
                    ApiDataSource(api.client()).reviewTrip(ApiTripReviewInput(wizardInput()))
                },
            ).isEqualTo(DataFailure.Offline)
            assertThat(api.server.requestCount).isEqualTo(1)
            val started = CompletableDeferred<Unit>()
            api.route("POST", "/v1/trips/review") {
                started.complete(Unit)
                MockResponse.Builder().onResponseStart(SocketEffect.Stall).build()
            }
            val client =
                api.client(
                    deadline =
                        ApiDeadline {
                            assertThat(it).isEqualTo(90.seconds)
                            started.await()
                        },
                )
            assertThat(
                failure {
                    ApiDataSource(client).reviewTrip(ApiTripReviewInput(wizardInput()))
                },
            ).isEqualTo(DataFailure.Offline)
            assertThat(api.server.requestCount).isEqualTo(2)
        }

    @Test fun savesRejectAnEmptyTripId() =
        runTest(timeout = 60.seconds) {
            val reply = JsonObject(fixtureObject("wizard/save.json") + ("tripId" to JsonPrimitive(""))).toString()
            api.reply("POST", "/v1/trips", 201, reply)
            api.reply("PUT", "/v1/trips/trip", 200, reply)
            val source = ApiDataSource(api.client())
            assertThat(failure { source.saveTrip(saveInput()) }).isEqualTo(DataFailure.Server(201))
            assertThat(failure { source.updateTrip("trip", saveInput(), "source") }).isEqualTo(DataFailure.Server(200))
        }
}
