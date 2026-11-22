package net.chronoflow.feature.auth.model

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskeyChallenge
import net.chronoflow.core.model.api.ApiPasskeyLoginInput
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.RegisterInput
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.api.TwoFactorInput
import net.chronoflow.core.model.auth.AuthenticationMethod
import net.chronoflow.core.model.guest.GuestClaim
import net.chronoflow.core.model.passkey.PasskeyCapability
import net.chronoflow.core.testing.Fixtures
import net.chronoflow.feature.auth.model.passkey.DeterministicAssertionProvider
import net.chronoflow.feature.auth.model.passkey.PasskeySignIn
import net.chronoflow.feature.auth.model.passkey.testAssertion
import net.chronoflow.feature.auth.testing.FakeSecretField
import net.chronoflow.feature.auth.testing.FakeSessionController
import net.chronoflow.feature.auth.testing.authTestSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

internal class ClaimAuthSource(
    io: CoroutineDispatcher,
    private val live: Boolean,
    private val base: DataSource = authTestSource(io),
) : DataSource by base {
    var body: JsonObject? = null
    var calls = 0
    var signUpFailure: DataFailure? = null
    var twoFactor = false
    var twoFactorInput: TwoFactorInput? = null
    private val result =
        ApiJson.response
            .decodeFromString(SessionResult.serializer(), Fixtures.text("auth/session.json"))
            .copy(tripId = if (live) "guest-token" else null)

    override suspend fun signUp(input: RegisterInput): SessionResult {
        calls++
        body = ApiJson.response.encodeToJsonElement(RegisterInput.serializer(), input).jsonObject
        signUpFailure?.let { throw it }
        return result
    }

    override suspend fun signIn(input: PasswordSignInInput): SignInResult {
        calls++
        body = ApiJson.response.encodeToJsonElement(PasswordSignInInput.serializer(), input).jsonObject
        return if (twoFactor) SignInResult.TwoFactor("challenge") else SignInResult.Session(result)
    }

    override suspend fun passkeyOptions(): ApiPasskeyChallenge {
        calls++
        return ApiJson.response.decodeFromString(
            ApiPasskeyChallenge.serializer(),
            Fixtures.text("auth/passkey-options.json"),
        )
    }

    override suspend fun passkeyLogin(input: ApiPasskeyLoginInput): SessionResult {
        calls++
        body = ApiJson.response.encodeToJsonElement(ApiPasskeyLoginInput.serializer(), input).jsonObject
        return result
    }

    override suspend fun completeTwoFactor(input: TwoFactorInput): SessionResult {
        twoFactorInput = input
        return result.copy(tripId = null)
    }
}
