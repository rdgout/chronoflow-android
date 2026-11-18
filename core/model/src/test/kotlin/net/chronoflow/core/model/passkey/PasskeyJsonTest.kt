package net.chronoflow.core.model.passkey

import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskeyChallenge
import net.chronoflow.core.model.api.ApiPasskeyAssertion
import net.chronoflow.core.model.api.ApiPasskeyAssertionResponse
import net.chronoflow.core.model.api.CredentialType
import net.chronoflow.core.testing.Fixtures
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PasskeyJsonTest {
    private val response = """{"id":"AQ","rawId":"AQ","type":"public-key","authenticatorAttachment":"platform","clientExtensionResults":{},"response":{"clientDataJSON":"eyJ0eXBlIjoid2ViYXV0aG4uZ2V0In0","authenticatorData":"Ag==","signature":"Aw","userHandle":""}}"""

    @Test fun requestJsonHasExactlyTheFiveOptionKeys() {
        val fixture = ApiJson.response.decodeFromString(ApiPasskeyChallenge.serializer(), Fixtures.text("auth/passkey-options.json"))
        assertEquals("""{"challenge":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA","rpId":"app.chronoflow.net","timeout":60000,"userVerification":"required","allowCredentials":[]}""", PasskeyJson.requestJson(fixture.options))
    }
    @Test fun credentialManagerResponseBecomesTheLoginAssertion() {
        val value = PasskeyJson.assertion(response)
        assertEquals(ApiPasskeyAssertion("AQ", "AQ", CredentialType.PublicKey,
            ApiPasskeyAssertionResponse("eyJ0eXBlIjoid2ViYXV0aG4uZ2V0In0", "Ag", "Aw", null)), value)
        val encoded = ApiJson.request.encodeToJsonElement(ApiPasskeyAssertion.serializer(), value).jsonObject
        assertEquals(setOf("id", "rawId", "type", "response"), encoded.keys)
        assertEquals(setOf("clientDataJSON", "authenticatorData", "signature"), encoded.getValue("response").jsonObject.keys)
    }
    @Test fun standardAlphabetIsNormalized() {
        assertEquals("-_8", PasskeyJson.assertion(response.replace("\"Aw\"", "\"+/8\"")).response.signature)
    }
    @Test fun mismatchedIdAndRawIdIsInvalidEncoding() {
        assertThrows(PasskeyFailure.InvalidEncoding::class.java) { PasskeyJson.assertion(response.replace("\"id\":\"AQ\"", "\"id\":\"Ag\"")) }
    }
    @Test fun wrongTypeMissingFieldsOrMalformedJsonAreUnavailableWrongType() {
        assertThrows(PasskeyFailure.Unavailable::class.java) { PasskeyJson.assertion(response.replace("public-key", "password")) }
    }
    @Test fun wrongTypeMissingFieldsOrMalformedJsonAreUnavailableMissingField() {
        assertThrows(PasskeyFailure.Unavailable::class.java) { PasskeyJson.assertion(response.replace("\"signature\":\"Aw\",", "")) }
    }
    @Test fun wrongTypeMissingFieldsOrMalformedJsonAreUnavailableMalformedJson() {
        assertThrows(PasskeyFailure.Unavailable::class.java) { PasskeyJson.assertion("{") }
    }
    @Test fun missingRawIdFallsBackToId() { assertEquals("AQ", PasskeyJson.assertion(response.replace("\"rawId\":\"AQ\",", "")).rawId) }
    @Test fun assertionDescriptionsRedactEveryField() {
        val assertion = PasskeyJson.assertion(response)
        assertEquals("ApiPasskeyAssertion(***)", assertion.toString())
        assertEquals("ApiPasskeyAssertionResponse(***)", assertion.response.toString())
    }
}
