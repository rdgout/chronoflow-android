package net.chronoflow.feature.auth.model.passkey

import kotlinx.coroutines.CoroutineDispatcher
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.ApiPasskeyAssertion
import net.chronoflow.core.model.api.ApiPasskeyChallenge
import net.chronoflow.core.model.api.ApiPasskeyLoginInput
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.passkey.Base64URL
import net.chronoflow.core.model.passkey.PasskeyCapability
import net.chronoflow.core.testing.Fixtures
import net.chronoflow.feature.auth.model.SignInModel
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import net.chronoflow.feature.auth.testing.FakeSessionController
import net.chronoflow.feature.auth.testing.RecordingRoutes
import net.chronoflow.feature.auth.testing.authTestSource

fun testAssertion(): ApiPasskeyAssertion =
    Base64URL.assertion(
        byteArrayOf(1),
        """{"type":"webauthn.get"}""".toByteArray(),
        byteArrayOf(2),
        byteArrayOf(3),
        null,
    )

fun passkeySource(
    io: CoroutineDispatcher,
    verified: Boolean = true,
    twoFactor: Boolean = false,
): MockDataSource {
    val snapshot =
        ApiJson.response.decodeFromString(
            AccountSnapshot.serializer(),
            Fixtures.text("account/snapshot.json"),
        )
    return authTestSource(io, verified, twoFactor, assertionProvider = { credential, _ ->
        if (credential.id == "AQ") snapshot.id else null
    })
}

class PasskeyTestFixture(
    io: CoroutineDispatcher,
    provider: PasskeyAssertionProviding,
    verified: Boolean = true,
    twoFactor: Boolean = false,
) {
    val source = passkeySource(io, verified, twoFactor)
    val session = FakeSessionController()
    val routes = RecordingRoutes()
    var loginCalls = 0
        private set
    private val recording =
        object : DataSource by source {
            override suspend fun passkeyLogin(input: ApiPasskeyLoginInput): SessionResult {
                loginCalls++
                return source.passkeyLogin(input)
            }
        }
    val model =
        SignInModel(
            source,
            session,
            { AUTH_DEVICE_ID },
            { "test" },
            PasskeyCapability(true),
            PasskeySignIn(recording, provider, { AUTH_DEVICE_ID }, { "test" })::signIn,
            accepted = routes::acceptedSession,
            onTwoFactor = routes::twoFactor,
        )
}

fun fixtureChallenge(): ApiPasskeyChallenge =
    ApiJson.response.decodeFromString(ApiPasskeyChallenge.serializer(), Fixtures.text("auth/passkey-options.json"))
