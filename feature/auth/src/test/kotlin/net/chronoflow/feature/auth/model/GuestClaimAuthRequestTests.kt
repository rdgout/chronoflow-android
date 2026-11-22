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

@RunWith(Parameterized::class)
class GuestClaimAuthRequestTests(
    private val method: String,
    private val live: Boolean,
) {
    @Test fun requestUsesOnlyTheClaimFieldsForItsMethod() =
        runTest(timeout = 60.seconds) {
            val source = ClaimAuthSource(StandardTestDispatcher(testScheduler), live)
            val session = FakeSessionController()
            val accepted = mutableListOf<Pair<SessionResult, AuthenticationMethod>>()
            val provider: suspend () -> GuestClaim? = { if (live) GuestClaim("guest-token", 4, 7, "secret") else null }
            val passkey =
                PasskeySignIn(
                    source,
                    DeterministicAssertionProvider { testAssertion() },
                    { "device" },
                    { "phone" },
                    provider,
                )
            val model =
                SignInModel(
                    source,
                    session,
                    { "device" },
                    { "phone" },
                    PasskeyCapability(true),
                    passkey::signIn,
                    accepted = { result, method -> accepted.add(result to method) },
                    onTwoFactor = {},
                    claim = provider,
                )
            val password = FakeSecretField("Password123!")
            val confirmation = FakeSecretField("Password123!")
            val expectedMethod =
                when (method) {
                    "register" -> {
                        val registration = model.makeSignUpModel {}
                        assertEquals(
                            SignUpModel.Outcome.Accepted,
                            registration.submit(password, confirmation, AuthLanguage.En) {},
                        )
                        assertTrue(confirmation.cleared)
                        AuthenticationMethod.SignUp
                    }

                    "password" -> {
                        model.submitPassword(password) {}
                        AuthenticationMethod.Password
                    }

                    else -> {
                        model.submitPasskey(password)
                        AuthenticationMethod.Passkey
                    }
                }
            val body = checkNotNull(source.body)
            assertEquals(if (live) JsonPrimitive("guest-token") else null, body["draft_token"])
            assertEquals(if (live) JsonPrimitive("secret") else null, body["guest_secret"])
            assertEquals(if (live && method == "register") JsonPrimitive(4) else null, body["input_revision"])
            assertEquals(if (live && method == "register") JsonPrimitive(7) else null, body["review_revision"])
            assertEquals(JsonPrimitive("device"), body["device_id"])
            assertEquals(JsonPrimitive("phone"), body["device_name"])
            assertEquals(expectedMethod, accepted.single().second)
            assertEquals(if (live) "guest-token" else null, accepted.single().first.tripId)
            assertEquals(1, session.accepted.size)
            assertTrue(password.cleared)
            assertEquals(if (method == "passkey") 2 else 1, source.calls)
            assertFalse(PasskeyCapability.Shipped.enabled)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-live={1}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("register", true),
                arrayOf("register", false),
                arrayOf("password", true),
                arrayOf("password", false),
                arrayOf("passkey", true),
                arrayOf("passkey", false),
            )
    }
}
