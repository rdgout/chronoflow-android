package net.chronoflow.feature.auth.model.passkey

import net.chronoflow.core.model.api.ApiPasskeyAssertion
import net.chronoflow.core.model.api.ApiPasskeyChallenge

fun interface PasskeyAssertionProviding {
    suspend fun assertion(challenge: ApiPasskeyChallenge): ApiPasskeyAssertion
}
