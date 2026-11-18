package net.chronoflow.core.model.passkey

import net.chronoflow.core.model.api.ApiPasskeyAssertion
import net.chronoflow.core.model.api.ApiPasskeyAssertionResponse
import net.chronoflow.core.model.api.CredentialType
import java.util.Base64

object Base64URL {
    fun encode(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    fun decode(value: String): ByteArray {
        if (value.isEmpty() || value.length % 4 == 1 ||
            value.any {
                it !in 'A'..'Z' && it !in 'a'..'z' && it !in '0'..'9' && it != '-' && it != '_'
            }
        ) {
            throw PasskeyFailure.InvalidEncoding
        }
        val bytes =
            try {
                Base64.getUrlDecoder().decode(value)
            } catch (_: IllegalArgumentException) {
                throw PasskeyFailure.InvalidEncoding
            }
        if (encode(bytes) != value) throw PasskeyFailure.InvalidEncoding
        return bytes
    }

    fun decodeLenient(value: String): ByteArray = decode(value.replace('+', '-').replace('/', '_').trimEnd('='))

    fun assertion(
        credentialID: ByteArray,
        clientDataJSON: ByteArray,
        authenticatorData: ByteArray,
        signature: ByteArray,
        userHandle: ByteArray?,
    ): ApiPasskeyAssertion {
        val id = encode(credentialID)
        return ApiPasskeyAssertion(
            id,
            id,
            CredentialType.PublicKey,
            ApiPasskeyAssertionResponse(
                encode(clientDataJSON),
                encode(authenticatorData),
                encode(signature),
                userHandle?.let(::encode),
            ),
        )
    }
}
