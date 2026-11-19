package net.chronoflow.core.datamock

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiTripReviewInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class NoHealthDataTests {
    @Test fun readinessOnlyAgeAndShiftWorkBlockAndPreferencesAdjust() {
        val body = mapOf("ageBand" to JsonPrimitive("18_64"), "shiftWork" to JsonPrimitive(false))
        assertEquals(JsonPrimitive("ready"), MockSettings.readiness(JsonObject(body))["variant"])
        val noCaffeine = MockSettings.readiness(JsonObject(body + ("excludeCaffeine" to JsonPrimitive(true))))
        assertEquals(JsonPrimitive("adjusted"), noCaffeine["variant"])
        assertEquals(JsonArray(emptyList()), noCaffeine["missing"])
        assertNull(noCaffeine["lightDevicesAllowed"])
        val movement = MockSettings.readiness(JsonObject(body + ("flightMovementBreaks" to JsonPrimitive(true))))
        assertEquals(JsonPrimitive("adjusted"), movement["variant"])
        assertEquals(JsonArray(emptyList()), movement["missing"])
        assertNull(movement["lightDevicesAllowed"])
        assertEquals(
            JsonPrimitive("adjusted"),
            MockSettings.readiness(
                JsonObject(body + ("ageBand" to JsonPrimitive("65_plus"))),
            )["variant"],
        )
        assertEquals(
            JsonPrimitive("excluded"),
            MockSettings.readiness(
                JsonObject(body + ("ageBand" to JsonPrimitive("under_18"))),
            )["variant"],
        )
        assertEquals(
            JsonPrimitive("excluded"),
            MockSettings.readiness(
                JsonObject(body + ("shiftWork" to JsonPrimitive(true))),
            )["variant"],
        )
    }

    private val retired =
        listOf("bipolar", "eye_disease", "pregnancy", "vte", "sleep_phase_disorder", "oral_contraception")

    @Test fun mockRejectsEveryRetiredReviewProfileKeyWith422() =
        runTest(timeout = 60.seconds) {
            val source = wizardMockSource()
            login(source)

            suspend fun reject(key: String) {
                val error =
                    failure {
                        source.reviewTrip(
                            ApiTripReviewInput(
                                wizardInput(),
                                profile =
                                    JsonObject(
                                        mapOf(key to JsonPrimitive(false)),
                                    ),
                            ),
                        )
                    }
                assertEquals(422, error.status)
                assertEquals(
                    DataFailure.ProfileInvalid(
                        mapOf("profile.form" to FieldMessage("This setting cannot be changed here.")),
                    ),
                    error,
                )
            }
            reject(retired[0])
            reject(retired[1])
            reject(retired[2])
            reject(retired[3])
            reject(retired[4])
            reject(retired[5])
            reject("caffeine_ruled_out")
        }
}
