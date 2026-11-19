package net.chronoflow.feature.auth.model.passkey

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.ApiPasskeyLoginInput
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.passkey.Base64URL
import net.chronoflow.core.model.passkey.PasskeyFailure

class PasskeySignIn(
    private val source: DataSource,
    private val provider: PasskeyAssertionProviding,
    private val identity: suspend () -> String,
    private val deviceName: suspend () -> String,
) {
    suspend fun signIn(): SessionResult {
        val deviceID = identity()
        currentCoroutineContext().ensureActive()
        val challenge = source.passkeyOptions()
        Base64URL.decode(challenge.options.challenge)
        if (challenge.options.rpId.isEmpty()) throw PasskeyFailure.Unavailable
        currentCoroutineContext().ensureActive()
        val assertion = provider.assertion(challenge)
        currentCoroutineContext().ensureActive()
        return source.passkeyLogin(
            ApiPasskeyLoginInput(
                challenge.challengeId,
                assertion,
                deviceID,
                deviceName(),
                draftToken = null,
                guestSecret = null,
            ),
        )
    }
}
