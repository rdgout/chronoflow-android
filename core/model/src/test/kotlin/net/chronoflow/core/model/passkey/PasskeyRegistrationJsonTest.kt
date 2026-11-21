package net.chronoflow.core.model.passkey

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskeyAttestation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PasskeyRegistrationJsonTest {
    @Test fun creationJsonKeepsTheServerOptionsExactly() {
        val expected = ApiJson.response.parseToJsonElement(RegistrationFixture.OPTIONS).jsonObject
        val actual =
            ApiJson.response
                .parseToJsonElement(
                    PasskeyJson.creationJson(RegistrationFixture.challenge.options),
                ).jsonObject
        assertEquals(
            setOf(
                "challenge",
                "timeout",
                "rp",
                "user",
                "pubKeyCredParams",
                "authenticatorSelection",
                "attestation",
                "excludeCredentials",
            ),
            actual.keys,
        )
        assertEquals(expected, actual)
    }

    @Test fun attestationIsRebuiltFromParsedFields() {
        val result = PasskeyJson.attestation(RegistrationFixture.RESPONSE)
        assertEquals(
            Base64URL.attestation(byteArrayOf(1), byteArrayOf(2), byteArrayOf(-5, -1), listOf("internal", "hybrid")),
            result,
        )
        val json = ApiJson.request.encodeToJsonElement(ApiPasskeyAttestation.serializer(), result).jsonObject
        assertEquals(setOf("id", "rawId", "type", "response"), json.keys)
        assertEquals(
            setOf("clientDataJSON", "attestationObject", "transports"),
            json.getValue("response").jsonObject.keys,
        )
    }

    @Test fun transportsAreFilteredAndOmittedWhenEmpty() {
        val transports =
            buildJsonArray {
                add(JsonPrimitive("x".repeat(33)))
                add(JsonPrimitive(12))
                add(JsonPrimitive(""))
                add(JsonPrimitive("internal"))
                add(JsonPrimitive("hybrid"))
                add(JsonPrimitive("usb"))
                add(JsonPrimitive("nfc"))
                add(JsonPrimitive("ble"))
                add(JsonPrimitive("one"))
                add(JsonPrimitive("two"))
                add(JsonPrimitive("three"))
                add(JsonPrimitive("four"))
            }
        val response = RegistrationFixture.RESPONSE.replace("[\"internal\",\"hybrid\"]", transports.toString())
        assertEquals(
            listOf("internal", "hybrid", "usb", "nfc", "ble", "one", "two", "three"),
            PasskeyJson.attestation(response).response.transports,
        )
        val empty = PasskeyJson.attestation(RegistrationFixture.RESPONSE.replace("[\"internal\",\"hybrid\"]", "[]"))
        assertNull(empty.response.transports)
        assertEquals(
            setOf("clientDataJSON", "attestationObject"),
            ApiJson.request
                .encodeToJsonElement(
                    ApiPasskeyAttestation.serializer(),
                    empty,
                ).jsonObject
                .getValue("response")
                .jsonObject.keys,
        )
    }

    @Test fun mismatchedIdAndRawIdIsInvalidEncoding() {
        assertThrows(PasskeyFailure.InvalidEncoding::class.java) {
            PasskeyJson.attestation(RegistrationFixture.RESPONSE.replace("\"id\":\"AQ\"", "\"id\":\"Ag\""))
        }
    }

    @Test fun wrongTypeMissingFieldsOrMalformedJsonAreUnavailableWrongType() {
        assertThrows(PasskeyFailure.Unavailable::class.java) {
            PasskeyJson.attestation(RegistrationFixture.RESPONSE.replace("public-key", "password"))
        }
    }

    @Test fun wrongTypeMissingFieldsOrMalformedJsonAreUnavailableMissingField() {
        assertThrows(PasskeyFailure.Unavailable::class.java) {
            PasskeyJson.attestation(RegistrationFixture.RESPONSE.replace("\"attestationObject\":\"+/8=\",", ""))
        }
    }

    @Test fun wrongTypeMissingFieldsOrMalformedJsonAreUnavailableMalformedJson() {
        assertThrows(PasskeyFailure.Unavailable::class.java) { PasskeyJson.attestation("{") }
    }

    @Test fun registrationDescriptionsRedactChallengeAndCredentialData() {
        val challenge = RegistrationFixture.challenge
        val credential = PasskeyJson.attestation(RegistrationFixture.RESPONSE)
        assertEquals("ApiPasskeyRegistrationChallenge(***)", challenge.toString())
        assertEquals("ApiPasskeyCreationOptions(***)", challenge.options.toString())
        assertEquals("ApiPasskeyUserEntity(***)", challenge.options.user.toString())
        assertEquals(
            "ApiPasskeyCredentialDescriptor(***)",
            challenge.options.excludeCredentials
                .single()
                .toString(),
        )
        assertEquals("ApiPasskeyAttestation(***)", credential.toString())
        assertEquals("ApiPasskeyAttestationResponse(***)", credential.response.toString())
        assertEquals(
            "ApiPasskeyRegistrationInput(***)",
            net.chronoflow.core.model.api
                .ApiPasskeyRegistrationInput("secret", "name", credential)
                .toString(),
        )
    }
}
