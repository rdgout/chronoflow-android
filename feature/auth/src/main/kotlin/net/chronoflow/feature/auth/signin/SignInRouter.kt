package net.chronoflow.feature.auth.signin

import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.auth.AuthenticationMethod

interface SignInRouter {
    fun back()

    fun forgot()

    fun signUp()

    fun guest()

    fun backToSignIn()

    fun twoFactor()

    suspend fun acceptedSession(
        result: SessionResult,
        method: AuthenticationMethod,
    )
}
