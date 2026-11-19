@file:Suppress("ktlint:standard:max-line-length")

package net.chronoflow.core.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiTripReviewInput
import net.chronoflow.core.network.support.MockApi
import net.chronoflow.core.network.support.RecordingDeadline
import net.chronoflow.core.network.support.failure
import net.chronoflow.core.network.support.json
import net.chronoflow.core.network.support.requestJson
import net.chronoflow.core.network.support.response
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class TripWizardTypedErrorTests(
    private val name: String,
) {
    @get:Rule val api = MockApi()

    @Test fun typedErrors() =
        runTest(timeout = 60.seconds) {
            api.reply("POST", "/v1/trips/review", if (name == "throttled") 429 else 422, json("wizard/$name.json"))
            val result = failure { ApiDataSource(api.client()).reviewTrip(ApiTripReviewInput(wizardInput())) }
            when (name) {
                "unknown-field" -> {
                    assertThat(result).isEqualTo(
                        DataFailure.Validation(
                            mapOf("legs.0.surprise" to FieldMessage("This setting cannot be changed here.")),
                            422,
                        ),
                    )
                }

                "journey" -> {
                    assertThat(
                        (result as DataFailure.Validation).fields.keys,
                    ).isEqualTo(setOf("stays.0.bedAvailableFromLocal", "legs.1.arriveLocal", "end.nights"))
                }

                "capacity" -> {
                    assertThat(result).isEqualTo(DataFailure.SaveCapacity)
                }

                "profile" -> {
                    val fields = (result as DataFailure.ProfileInvalid).fields
                    assertThat(fields.containsKey("profile")).isEqualTo(true)
                    assertThat(fields.containsKey("profile.habitualBed")).isEqualTo(true)
                }

                "expired" -> {
                    assertThat(result).isEqualTo(DataFailure.ReviewExpired)
                }

                "credits" -> {
                    assertThat(result).isEqualTo(DataFailure.NotEnoughCredits)
                }

                "throttled" -> {
                    assertThat(result).isEqualTo(DataFailure.Throttled(42.0, 429))
                }
            }
            assertThat(api.server.requestCount).isEqualTo(1)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<String>> =
            listOf(
                arrayOf(
                    "unknown-field",
                ),
                arrayOf("journey"),
                arrayOf("capacity"),
                arrayOf("profile"),
                arrayOf("expired"),
                arrayOf("credits"),
                arrayOf("throttled"),
            )
    }
}
