package net.chronoflow.feature.auth.model.passkey

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import net.chronoflow.core.model.api.ApiPasskeyAssertion
import net.chronoflow.core.model.api.ApiPasskeyChallenge
import net.chronoflow.core.model.passkey.Base64URL
import net.chronoflow.core.model.passkey.CredentialClient
import net.chronoflow.core.model.passkey.PasskeyJson

class SystemPasskeyAssertionProvider(
    private val client: CredentialClient,
) : PasskeyAssertionProviding {
    override suspend fun assertion(challenge: ApiPasskeyChallenge): ApiPasskeyAssertion {
        currentCoroutineContext().ensureActive()
        Base64URL.decode(challenge.options.challenge)
        val json = client.getPasskey(PasskeyJson.requestJson(challenge.options))
        return PasskeyJson.assertion(json)
    }
}
