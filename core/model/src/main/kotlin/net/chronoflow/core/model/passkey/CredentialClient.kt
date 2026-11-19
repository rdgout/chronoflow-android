package net.chronoflow.core.model.passkey

fun interface CredentialClient {
    suspend fun getPasskey(requestJson: String): String
}
