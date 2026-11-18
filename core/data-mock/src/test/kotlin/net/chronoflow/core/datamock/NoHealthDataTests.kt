package net.chronoflow.core.datamock

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

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
}
