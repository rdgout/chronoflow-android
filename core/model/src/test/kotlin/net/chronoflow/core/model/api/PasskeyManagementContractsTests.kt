package net.chronoflow.core.model.api

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.int
import org.junit.Test

class PasskeyManagementContractsTests {
    @Test
    fun registrationChallengeDecodesTheDocumentedOptions() {
        val json =
            """
            {"challengeId":"c1","options":{"challenge":"Y2g","timeout":60000,
            "rp":{"id":"app.chronoflow.net","name":"app.chronoflow.net"},
            "user":{"id":"dXNlcg","name":"demo@example.com","displayName":"Demo"},
            "pubKeyCredParams":[{"type":"public-key","alg":-7},{"type":"public-key","alg":-257}],
            "authenticatorSelection":{"requireResidentKey":true,"userVerification":"required","residentKey":"required"},
            "attestation":"none",
            "excludeCredentials":[{"type":"public-key","id":"AQID"}]}}
            """.trimIndent()
        val challenge = ApiJson.response.decodeFromString(ApiPasskeyRegistrationChallenge.serializer(), json)
        assertThat(challenge.challengeId).isEqualTo("c1")
        assertThat(challenge.options.timeout).isEqualTo(60_000)
        assertThat(challenge.options.rp).isEqualTo(ApiPasskeyRelyingParty("app.chronoflow.net", "app.chronoflow.net"))
        assertThat(challenge.options.user).isEqualTo(ApiPasskeyUserEntity("dXNlcg", "demo@example.com", "Demo"))
        assertThat(challenge.options.pubKeyCredParams.map { it.alg }).containsExactly(-7, -257)
        assertThat(challenge.options.authenticatorSelection)
            .isEqualTo(ApiPasskeyAuthenticatorSelection(true, "required", "required"))
        assertThat(challenge.options.attestation).isEqualTo("none")
        assertThat(challenge.options.excludeCredentials)
            .containsExactly(ApiPasskeyCredentialDescriptor(CredentialType.PublicKey, "AQID"))
        val missing = """{"challengeId":"c2","options":{"challenge":"Y2g","rp":{"id":"a","name":"a"}}}"""
        assertFailure { ApiJson.response.decodeFromString(ApiPasskeyRegistrationChallenge.serializer(), missing) }
            .isInstanceOf<SerializationException>()
    }

    @Test
    fun registrationInputEncodesTheDocumentedBodyWithoutAPassword() {
        val input =
            ApiPasskeyRegistrationInput(
                "c1",
                "Pixel",
                ApiPasskeyAttestation(
                    "AQID",
                    "AQID",
                    CredentialType.PublicKey,
                    ApiPasskeyAttestationResponse("Y2Q", "YXR0", listOf("internal", "hybrid")),
                ),
            )
        val text = ApiJson.request.encodeToString(ApiPasskeyRegistrationInput.serializer(), input)
        val obj = ApiJson.response.parseToJsonElement(text) as JsonObject
        assertThat(obj.keys).isEqualTo(setOf("challengeId", "name", "credential"))
        val credential = obj["credential"] as JsonObject
        assertThat(credential.keys).isEqualTo(setOf("id", "rawId", "type", "response"))
        assertThat(credential["type"]).isEqualTo(JsonPrimitive("public-key"))
        val response = credential["response"] as JsonObject
        assertThat(response["clientDataJSON"]).isEqualTo(JsonPrimitive("Y2Q"))
        assertThat(response["attestationObject"]).isEqualTo(JsonPrimitive("YXR0"))
        val bare = ApiPasskeyAttestationResponse("a", "b")
        val encoded = ApiJson.request.encodeToString(ApiPasskeyAttestationResponse.serializer(), bare)
        assertThat(
            (ApiJson.response.parseToJsonElement(encoded) as JsonObject).containsKey("transports"),
        ).isEqualTo(false)
    }

    @Test
    fun passkeyListItemDecodesTheDocumentedFields() {
        val text =
            """
            {"passkeys":[{"id":12,"name":"Pixel","authenticator":"Google Password Manager",
            "createdAt":"2026-11-12T09:30:00.000000Z","lastUsedAt":"2026-11-12T10:02:41.000000Z"},
            {"id":7,"name":"MacBook","authenticator":null,"createdAt":"2026-10-30T18:11:05.000000Z","lastUsedAt":null}]}
            """.trimIndent()
        val list = ApiJson.response.decodeFromString(ApiPasskeyList.serializer(), text)
        assertThat(list.passkeys.map { it.id }).containsExactly(12L, 7L)
        assertThat(list.passkeys[0].authenticator).isEqualTo("Google Password Manager")
        assertThat(list.passkeys[1].authenticator).isNull()
        assertThat(list.passkeys[1].lastUsedAt).isNull()
        assertThat(
            ApiJson.response.decodeFromString(ApiPasskeyList.serializer(), """{"passkeys":[]}""").passkeys,
        ).isEmpty()
    }

    @Test
    fun securitySummaryCountIsOptionalAndWholeNumbersOnly() {
        val summary =
            ApiJson.response.parseToJsonElement(
                """{"passkeyCount":2,"zero":0,"twoFactorEnabled":false}""",
            ) as JsonObject
        assertThat(summary["passkeyCount"].int).isEqualTo(2)
        assertThat(summary["zero"].int).isEqualTo(0)
        assertThat(summary["missing"].int).isNull()
        assertThat(JsonPrimitive(1.5).int).isNull()
        assertThat(JsonPrimitive(-1).int).isNull()
        assertThat(JsonPrimitive("2").int).isNull()
    }
}
