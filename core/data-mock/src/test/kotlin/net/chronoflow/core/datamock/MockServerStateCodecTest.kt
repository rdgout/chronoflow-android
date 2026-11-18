package net.chronoflow.core.datamock

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.core.model.contracts.ApiTripInput
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.testing.Fixtures
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
    @Test fun draftDecodeNormalizesProfile() {
        val raw = legacyDraftProfile()
        val draft = MockJson.decodeFromJsonElement(MockWizardDraft.serializer(), draftJson(raw))
        assertEquals(ProfilePreferences.bodyClock(raw), draft.profile)
    }

    @Test fun guestDraftDecodeNormalizesNestedProfile() {
        val raw = legacyDraftProfile()
        val obj =
            JsonObject(
                mapOf(
                    "deviceID" to JsonPrimitive(mockDeviceID),
                    "secretHash" to JsonPrimitive("artificial-secret-hash"),
                    "draft" to draftJson(raw),
                ),
            )
        val guest = MockJson.decodeFromJsonElement(MockGuestDraft.serializer(), obj)
        assertEquals(ProfilePreferences.bodyClock(raw), guest.draft.profile)
    }

    private fun legacyDraftProfile(): JsonObject =
        JsonObject(
            mapOf(
                "ageBand" to JsonPrimitive("18_64"),
                "pregnancy" to JsonPrimitive(true),
                "caffeineRuledOut" to JsonPrimitive(true),
            ),
        )

    private fun draftJson(profile: JsonObject): JsonObject =
        JsonObject(
            mapOf(
                "owner" to JsonPrimitive("owner"),
                "input" to ApiJson.response.encodeToJsonElement(ApiTripInput.serializer(), wizardInput()),
                "review" to ApiJson.response.parseToJsonElement(Fixtures.text("wizard/review.json")),
                "profile" to profile,
                "expiresAt" to JsonPrimitive(100.0),
            ),
        )

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
