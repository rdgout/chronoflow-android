package net.chronoflow.feature.auth.model.passkey

import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskeyAssertion
import net.chronoflow.core.model.api.ApiPasskeyAssertionResponse
import net.chronoflow.core.model.api.CredentialType
import net.chronoflow.core.model.passkey.Base64URL
import net.chronoflow.core.model.passkey.PasskeyFailure
import net.chronoflow.feature.auth.model.AuthBannerKind
import net.chronoflow.feature.auth.model.AuthFailureMapping
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PasskeyEncodingTests {
    @Test fun binaryBytesRoundTripWithoutUTF8OrPaddingAndAllDTOFieldsUseBase64URL() {
        val bytes = ByteArray(256) { it.toByte() }
        val encoded = Base64URL.encode(bytes)
        assertFalse(encoded.any { it == '=' || it == '+' || it == '/' })
        assertArrayEquals(bytes, Base64URL.decode(encoded))
        val assertion = Base64URL.assertion(bytes, bytes, bytes, bytes, bytes)
        assertEquals(
            ApiPasskeyAssertion(
                encoded,
                encoded,
                CredentialType.PublicKey,
                ApiPasskeyAssertionResponse(encoded, encoded, encoded, encoded),
            ),
            assertion,
        )
        assertNull(Base64URL.assertion(bytes, bytes, bytes, bytes, null).response.userHandle)
        assertEquals(
            assertion,
            ApiJson.response.decodeFromString(
                ApiPasskeyAssertion.serializer(),
                ApiJson.request.encodeToString(ApiPasskeyAssertion.serializer(), assertion),
            ),
        )
    }

    private fun invalid(value: String) {
        assertThrows(PasskeyFailure.InvalidEncoding::class.java) { Base64URL.decode(value) }
    }

    @Test fun invalidEncodingEmpty() = invalid("")

    @Test fun invalidEncodingLength() = invalid("a")

    @Test fun invalidEncodingPadding() = invalid("AQ==")

    @Test fun invalidEncodingPlus() = invalid("AQ+")

    @Test fun invalidEncodingSlash() = invalid("AQ/")

    @Test fun invalidEncodingNonCanonical() = invalid("AR")

    @Test fun invalidEncodingUnicode() = invalid("é")

    @Test fun invalidEncodingWhitespace() = invalid("a b")

    private fun fieldFailure(
        field: String,
        key: String,
    ) {
        val mapping =
            AuthFailureMapping(
                DataFailure.Validation(mapOf(field to FieldMessage(key)), 422),
                setOf("email", "password"),
            )
        assertEquals(AuthBannerKind.Server, mapping.banner)
        assertTrue(mapping.fields.isEmpty())
    }

    @Test fun passkeyFieldFailuresUseOnlyDesignedBannerAndNeverLeakServerCopyCredential() =
        fieldFailure("credential", "We couldn't sign you in with that passkey. Please try again.")

    @Test fun passkeyFieldFailuresUseOnlyDesignedBannerAndNeverLeakServerCopyChallenge() =
        fieldFailure("challengeId", "The passkey request expired. Please try again.")
}
