package net.chronoflow.core.datamock

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.model.api.AuthLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

internal fun codecState(): MockServerState =
    MockServerState(
        listOf(
            MockServerAccount(
                mockRegistration(
                    MockScenarioValues.demoID,
                    "Demo Traveller",
                    MockScenarioValues.demoEmail,
                    AuthLanguage.En,
                    Instant.EPOCH,
                ),
                "artificial-password",
            ),
        ),
        "artificial-signing-key",
    )

/** Android state codec coverage (new). */
class MockServerStateCodecTest {
    @Test fun stateRoundTrips() {
        val state = codecState()
        assertEquals(
            state,
            MockJson.decodeFromString(
                MockServerState.serializer(),
                MockJson.encodeToString(MockServerState.serializer(), state),
            ),
        )
    }

    @Test fun schemaIsRequired() {
        val obj = MockJson.encodeToJsonElement(MockServerState.serializer(), codecState()).jsonObject
        assertThrows(SerializationException::class.java) {
            MockJson.decodeFromJsonElement(
                MockServerState.serializer(),
                JsonObject(obj - "schemaVersion"),
            )
        }
    }

    @Test fun emptyObjectIsRejected() {
        assertThrows(
            SerializationException::class.java,
        ) { MockJson.decodeFromString(MockServerState.serializer(), "{}") }
    }

    @Test fun unknownKeysAreIgnored() {
        val state = codecState()
        val obj = MockJson.encodeToJsonElement(MockServerState.serializer(), state).jsonObject
        assertEquals(
            state,
            MockJson.decodeFromJsonElement(
                MockServerState.serializer(),
                JsonObject(obj + ("future" to JsonPrimitive(true))),
            ),
        )
    }

    @Test fun credentialDescriptionsAreRedacted() {
        val state = codecState()
        assertFalse(state.toString().contains(state.signingKey))
        assertFalse(
            state.accounts.values
                .single()
                .toString()
                .contains("artificial-password"),
        )
        assertFalse(MockReset("artificial-token", 1.0).toString().contains("artificial-token"))
    }
}
