package net.chronoflow.feature.auth.testing

import kotlinx.coroutines.CoroutineDispatcher
import net.chronoflow.core.datamock.MemoryMockStateStore
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.datamock.MockServerAccount
import net.chronoflow.core.datamock.MockServerState
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.AccountSettings
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.auth.AuthenticationMethod
import net.chronoflow.core.model.passkey.PasskeyCapability
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.testing.Fixtures
import net.chronoflow.feature.auth.model.SecretField
import net.chronoflow.feature.auth.model.SessionAcceptor
import net.chronoflow.feature.auth.model.SignInModel
import net.chronoflow.feature.auth.signin.SignInRouter
import java.time.Instant

const val AUTH_DEVICE_ID = "0199a3c0-0000-7000-8000-000000000001"

fun authTestSource(
    io: CoroutineDispatcher,
    verified: Boolean = true,
    twoFactor: Boolean = false,
    password: String = "Password123!",
    assertionProvider: net.chronoflow.core.datamock.AssertionProvider = { _, _ -> null },
): MockDataSource {
    val original =
        ApiJson.response.decodeFromString(
            AccountSnapshot.serializer(),
            Fixtures.text("account/snapshot.json"),
        )
    val settings = original.settings
    val snapshot =
        AccountSnapshot(
            original.id,
            original.name,
            original.email,
            verified,
            AccountSettings(
                settings.bodyClock,
                settings.sleepAnswered,
                settings.preferences,
                settings.reminders,
                settings.planCredits,
                AccountPolicies("1.0", "1.0", "1.0"),
            ),
            original.readiness,
            original.billing,
            original.security,
        )
    val account =
        MockServerAccount(
            snapshot,
            password,
            twoFactorCode = if (twoFactor) "123456" else null,
            walkthroughSeen = true,
        )
    return MockDataSource(store = MemoryMockStateStore(), seed = MockServerState(accounts = listOf(account)), now = {
        Instant.parse("2026-11-18T00:00:00Z")
    }, delay = { 0.0 }, sleep = {}, debugAppSeed = false, io = io, assertionProvider = assertionProvider)
}

class FakeSessionController : SessionAcceptor {
    val accepted = mutableListOf<SessionResult>()
    var failure: SessionFailure? = null
    var onAccept: () -> Unit = {}

    override suspend fun accept(result: SessionResult) {
        failure?.let { throw it }
        accepted += result
        onAccept()
    }
}

class FakeSecretField(
    var value: String = "Password123!",
    private val onClear: () -> Unit = {},
) : SecretField {
    var cleared: Boolean = false
        private set

    override fun read(): String = value

    override fun clear() {
        value = ""
        cleared = true
        onClear()
    }

    override fun toString(): String = "FakeSecretField(<redacted>)"
}

class RecordingRoutes : SignInRouter {
    val accepted = mutableListOf<Pair<SessionResult, AuthenticationMethod>>()
    val actions = mutableListOf<String>()

    override fun back() {
        actions += "back"
    }

    override fun forgot() {
        actions += "forgot"
    }

    override fun signUp() {
        actions += "signUp"
    }

    override fun guest() {
        actions += "guest"
    }

    var backToSignIn = 0
        private set

    override fun backToSignIn() {
        backToSignIn++
    }

    override fun twoFactor() {
        actions += "twoFactor"
    }

    override suspend fun acceptedSession(
        result: SessionResult,
        method: AuthenticationMethod,
    ) {
        accepted +=
            result to method
    }
}

class AuthTestFixture(
    io: CoroutineDispatcher,
    verified: Boolean = true,
    twoFactor: Boolean = false,
    capability: Boolean = false,
    password: String = "Password123!",
    identity: suspend () -> String = {
        AUTH_DEVICE_ID
    },
    passkey: (suspend () -> SessionResult)? = null,
) {
    val source = authTestSource(io, verified, twoFactor, password)
    val session = FakeSessionController()
    val routes = RecordingRoutes()
    val model =
        SignInModel(
            source,
            session,
            identity,
            { "test" },
            PasskeyCapability(capability),
            passkey =
                passkey ?: { throw net.chronoflow.core.model.passkey.PasskeyFailure.Unavailable },
            accepted = routes::acceptedSession,
            onTwoFactor = routes::twoFactor,
        )
}
