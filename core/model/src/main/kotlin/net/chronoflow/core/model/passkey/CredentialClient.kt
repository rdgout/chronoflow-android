package net.chronoflow.core.model.passkey

fun interface CredentialClient {
    suspend fun getPasskey(requestJson: String): String

    suspend fun createPasskey(requestJson: String): String = throw PasskeyFailure.Unavailable
}
