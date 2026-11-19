package net.chronoflow.feature.auth.model.passkey

import net.chronoflow.core.model.api.ApiPasskeyAssertion
import net.chronoflow.core.model.api.ApiPasskeyChallenge
import net.chronoflow.core.model.passkey.PasskeyFailure

class DeterministicAssertionProvider(
    private val provide: suspend (ApiPasskeyChallenge) -> ApiPasskeyAssertion = { throw PasskeyFailure.Canceled },
) : PasskeyAssertionProviding {
    override suspend fun assertion(challenge: ApiPasskeyChallenge): ApiPasskeyAssertion = provide(challenge)
}
