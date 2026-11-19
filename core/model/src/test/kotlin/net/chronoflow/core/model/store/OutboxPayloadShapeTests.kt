package net.chronoflow.core.model.store

import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.OutboxKind
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

class OutboxPayloadShapeTests {
    private fun row(change: OutboxChange) = OutboxPayload.record(change, "id", "now")

    private fun json(text: String) = ApiJson.response.parseToJsonElement(text) as JsonObject

    @Test fun nullCheckStatusIsOmitted() {
        val change = OutboxChange.CheckOff(CheckOffChange("test", "fake", BlockType.Sleep, "2026-11-10", 1, null))
        assertEquals(
            """{"local_date":"2026-11-10","ordinal":1,"plan_input_hash":"fake","tripID":"test","type":"sleep"}""",
            row(change).payload,
        )
        assertEquals(change, OutboxPayload.change(row(change)))
        assertEquals("trip:test", OutboxPayload.dependency(row(change)))
    }

    @Test fun doneCheckStatusIsStored() {
        val change =
            OutboxChange.CheckOff(
                CheckOffChange("test", "fake", BlockType.Unknown("future"), "2026-11-10", 1, CheckStatus.Done),
            )
        assertEquals(change, OutboxPayload.change(row(change)))
    }

    @Test fun settingsShape() {
        val change = OutboxChange.Settings(SettingsSection.Reminders, json("""{"light":true}"""))
        assertEquals("""{"patch":{"light":true},"section":"reminders"}""", row(change).payload)
        assertEquals(change, OutboxPayload.change(row(change)))
        assertEquals("settings:reminders", OutboxPayload.dependency(row(change)))
    }

    @Test fun policyShape() {
        val change = OutboxChange.Policies("1.0")
        assertEquals("""{"section":"policies","version":"1.0"}""", row(change).payload)
        assertEquals(change, OutboxPayload.change(row(change)))
    }

    @Test fun profileShape() {
        val change = OutboxChange.Profile(ProfileChange("Test", "test@example.com"))
        assertEquals("""{"email":"test@example.com","name":"Test"}""", row(change).payload)
        assertEquals(change, OutboxPayload.change(row(change)))
        assertEquals("profile", OutboxPayload.dependency(row(change)))
    }

    @Test fun walkthroughShape() {
        assertEquals("{}", row(OutboxChange.Walkthrough).payload)
        assertEquals(OutboxChange.Walkthrough, OutboxPayload.change(row(OutboxChange.Walkthrough)))
        assertEquals("walkthrough", OutboxPayload.dependency(row(OutboxChange.Walkthrough)))
    }

    @Test fun legacyDependenciesAndUnsupported() {
        val legacy = OutboxRecord("id", "check-off", """{"tripId":"x"}""", "now")
        assertEquals("trip:x", OutboxPayload.dependency(legacy))
        assertEquals("x", OutboxPayload.referencedTripId(legacy))
        assertEquals("settings:unknown", OutboxPayload.dependency(legacy.copy(kind = "settings")))
        val parked = legacy.copy(kind = "complete-trip", payload = """{"future":null}""")
        assertEquals(
            OutboxChange.Unsupported(OutboxKind.CompleteTrip, json(parked.payload)),
            OutboxPayload.change(parked),
        )
        assertThrows(LocalMutationFailure.Unsupported::class.java) { row(OutboxPayload.change(parked)) }
    }

    @Test fun malformedPayloadIsSanitized() {
        assertThrows(LocalMutationFailure.InvalidPayload::class.java) {
            OutboxPayload.change(OutboxRecord("id", "profile", "[]", "now"))
        }
        assertNull(LocalMutationFailure.InvalidPayload.message)
        assertThrows(LocalMutationFailure.InvalidPayload::class.java) {
            row(OutboxChange.Settings(SettingsSection.Preferences, json("{}")))
        }
    }
}
