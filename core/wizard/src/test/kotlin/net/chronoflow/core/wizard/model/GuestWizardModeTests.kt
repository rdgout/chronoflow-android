package net.chronoflow.core.wizard.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.api.ApiJson
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed::class)
class GuestWizardModeTests {
    class Codec {
        @Test fun legacyStateDecodesAsMemberAndModeRoundTrips() {
            val state = MemberWizardState()
            val encoded = ApiJson.response.encodeToJsonElement(MemberWizardState.serializer(), state) as JsonObject
            assertEquals(
                WizardMode.member,
                ApiJson.response
                    .decodeFromString<MemberWizardState>(
                        JsonObject(encoded - "mode").toString(),
                    ).mode,
            )
            val guest = state.copy(mode = WizardMode.guest)
            assertEquals(
                guest,
                ApiJson.response.decodeFromString<MemberWizardState>(ApiJson.response.encodeToString(guest)),
            )
        }
    }
}
