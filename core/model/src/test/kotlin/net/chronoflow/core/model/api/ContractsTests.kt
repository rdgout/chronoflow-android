package net.chronoflow.core.model.api

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import org.junit.Test

class ContractsTests {
    @Test
    fun unknownJSONAndFailureInterpolation() {
        val text = """{"future":{"null":null,"list":[true,3.25,"x"]}}"""
        val element = ApiJson.response.parseToJsonElement(text)
        assertThat(element is JsonObject).isEqualTo(true)
        assertThat(ApiJson.response.encodeToString(JsonObject.serializer(), element as JsonObject)).isEqualTo(text)
        val max = FieldMessage("Use no more than :max characters.", mapOf("max" to JsonPrimitive(255)))
        assertThat(max.interpolationKey).isEqualTo("Use no more than %lld characters.")
        val min = FieldMessage("At least {{min}}.", mapOf("min" to JsonPrimitive(12)))
        assertThat(min.interpolationKey).isEqualTo("At least %lld.")
    }

    @Test
    fun failureInterpolationOrdersArgumentsByName() {
        val message =
            FieldMessage(
                "From :min to {{max}} for :attribute.",
                mapOf("min" to JsonPrimitive(1), "max" to JsonPrimitive(12), "attribute" to JsonPrimitive("name")),
            )
        assertThat(message.interpolationKey).isEqualTo("From %3\$lld to %2\$lld for %1\$@.")
        assertThat(message.catalogArguments).containsExactly(JsonPrimitive("name"), JsonPrimitive(12), JsonPrimitive(1))
    }

    @Test
    fun signInVariantsAndMalformed() {
        val twoFactor =
            ApiJson.response.decodeFromString(
                SignInResult.serializer(),
                """{"kind":"two-factor","challengeId":"fake"}""",
            )
        assertThat(twoFactor).isEqualTo(SignInResult.TwoFactor("fake"))
        listOf(
            """{"kind":"unknown"}""",
            """{"kind":"two-factor"}""",
            """{"kind":"session","token":"fake"}""",
            """{"kind":"two-factor","challengeId":""}""",
        ).forEach { json ->
            assertFailure {
                ApiJson.response.decodeFromString(SignInResult.serializer(), json)
            }.isInstanceOf<SerializationException>()
        }
    }

    @Test
    fun proofIsExclusive() {
        val input = TwoFactorInput("fake", TwoFactorInput.Proof.RecoveryCode("fake-recovery"))
        val text = ApiJson.request.encodeToString(TwoFactorInput.serializer(), input)
        assertThat(ApiJson.response.decodeFromString(TwoFactorInput.serializer(), text)).isEqualTo(input)
        assertFailure {
            ApiJson.response.decodeFromString(
                TwoFactorInput.serializer(),
                """{"challengeId":"fake","code":"1","recovery_code":"2"}""",
            )
        }.isInstanceOf<SerializationException>()
        assertFailure {
            ApiJson.response.decodeFromString(TwoFactorInput.serializer(), """{"challengeId":"fake"}""")
        }.isInstanceOf<SerializationException>()
    }

    // The outbox half of the Swift test lands with the outbox in 4d.
    @Test
    fun inputWireKeysAndOutboxRoundTrips() {
        val input =
            RegisterInput(
                name = "Test",
                email = "test@example.com",
                password = "artificial",
                passwordConfirmation = "artificial",
                deviceID = "test-device",
                deviceName = "Pixel",
                language = AuthLanguage.Nl,
                draftToken = "artificial-draft",
                inputRevision = 1,
                reviewRevision = 1,
                guestSecret = "artificial-secret",
            )
        val text = ApiJson.request.encodeToString(RegisterInput.serializer(), input)
        val obj = ApiJson.response.parseToJsonElement(text) as JsonObject
        assertThat(obj["device_id"]).isEqualTo(JsonPrimitive("test-device"))
        assertThat(obj["password_confirmation"]).isEqualTo(JsonPrimitive("artificial"))
        assertThat(obj["draft_token"]).isEqualTo(JsonPrimitive("artificial-draft"))
        assertThat(obj["language"]).isEqualTo(JsonPrimitive("nl"))
        assertThat(obj.containsKey("deviceID")).isEqualTo(false)
        assertThat(ApiJson.response.decodeFromString(RegisterInput.serializer(), text)).isEqualTo(input)
    }

    private val assertionJson =
        """{"id":"AA","rawId":"AA==","type":"public-key",""" +
            """"response":{"clientDataJSON":"AA","authenticatorData":"AA==","signature":"AA","userHandle":null}}"""

    @Test
    fun passkeyNullAndEncoding() {
        val assertion = ApiJson.response.decodeFromString(ApiPasskeyAssertion.serializer(), assertionJson)
        assertThat(assertion.response.userHandle).isNull()
        val input = ApiPasskeyLoginInput("fake", assertion, "fake-device", "Pixel")
        val text = ApiJson.request.encodeToString(ApiPasskeyLoginInput.serializer(), input)
        assertThat(ApiJson.response.decodeFromString(ApiPasskeyLoginInput.serializer(), text)).isEqualTo(input)
        listOf(
            assertionJson.replace("public-key", "password"),
            assertionJson.replace(""""signature":"AA",""", ""),
        ).forEach { bad ->
            assertFailure { ApiJson.response.decodeFromString(ApiPasskeyAssertion.serializer(), bad) }
                .isInstanceOf<SerializationException>()
        }
    }

    @Test
    fun nullablePasskeyUserHandleAndDiscriminators() {
        listOf("", ""","userHandle":null""", ""","userHandle":""""").forEach { member ->
            val text = """{"clientDataJSON":"AA","authenticatorData":"AA==","signature":"AA"$member}"""
            val response = ApiJson.response.decodeFromString(ApiPasskeyAssertionResponse.serializer(), text)
            assertThat(response.userHandle == null || response.userHandle == "").isEqualTo(true)
        }
        val options =
            """{"challenge":"AA","rpId":"app.chronoflow.net","timeout":60000,""" +
                """"userVerification":"required","allowCredentials":[]}"""
        listOf(options.replace("required", "preferred"), options.replace("[]", "[{}]")).forEach { invalid ->
            assertFailure { ApiJson.response.decodeFromString(ApiPasskeyRequestOptions.serializer(), invalid) }
                .isInstanceOf<SerializationException>()
        }
    }
}
