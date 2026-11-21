package net.chronoflow.core.model.passkey

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import net.chronoflow.core.model.api.ApiPasskeyAttestation
import net.chronoflow.core.model.api.ApiPasskeyRegistrationChallenge

fun interface PasskeyRegistrationProviding {
    suspend fun registration(challenge: ApiPasskeyRegistrationChallenge): ApiPasskeyAttestation
}

class DeterministicRegistrationProvider(
    private val provide: suspend (ApiPasskeyRegistrationChallenge) -> ApiPasskeyAttestation = { challenge ->
        Base64URL.attestation(
            ("credential:" + challenge.options.challenge).encodeToByteArray(),
            ("client-data:" + challenge.options.challenge).encodeToByteArray(),
            ("attestation:" + challenge.options.challenge).encodeToByteArray(),
            listOf("internal"),
        )
    },
) : PasskeyRegistrationProviding {
    override suspend fun registration(challenge: ApiPasskeyRegistrationChallenge): ApiPasskeyAttestation =
        provide(challenge)
}

class SystemPasskeyRegistrationProvider(
    private val client: CredentialClient,
) : PasskeyRegistrationProviding {
    override suspend fun registration(challenge: ApiPasskeyRegistrationChallenge): ApiPasskeyAttestation {
        currentCoroutineContext().ensureActive()
        val options = challenge.options
        Base64URL.decode(options.challenge)
        Base64URL.decode(options.user.id)
        Base64URL.excludedCredentialIDs(options)
        if (options.rp.id.isEmpty()) throw PasskeyFailure.Unavailable
        return PasskeyJson.attestation(client.createPasskey(PasskeyJson.creationJson(options)))
    }
}
