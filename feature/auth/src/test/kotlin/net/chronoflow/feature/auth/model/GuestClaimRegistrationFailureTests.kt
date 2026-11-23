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
class GuestClaimRegistrationFailureTests(
    private val name: String,
    private val failure: DataFailure,
    private val expired: Boolean,
) {
    @Test fun registrationFailureClearsBothPasswordsAndReturnsExpiry() =
        runTest(timeout = 60.seconds) {
            val source = ClaimAuthSource(StandardTestDispatcher(testScheduler), true).apply { signUpFailure = failure }
            var expiries = 0
            val session = FakeSessionController()
            val signIn =
                SignInModel(
                    source,
                    session,
                    { "device" },
                    { "phone" },
                    accepted = { _, _ ->
                        error("Failed registration accepted")
                    },
                    onTwoFactor = {},
                    claim = { GuestClaim("guest-token", 4, 7, "secret") },
                    guestReviewExpired = { expiries++ },
                )
            val model = signIn.makeSignUpModel {}
            model.editName("Traveller")
            model.editEmail("new@example.com")
            val password = FakeSecretField("Password123!")
            val confirmation = FakeSecretField("Password123!")
            val outcome = model.submit(password, confirmation, AuthLanguage.En) {}
            assertEquals(if (expired) SignUpModel.Outcome.GuestReviewExpired else null, outcome)
            assertEquals(if (expired) 1 else 0, expiries)
            assertTrue(password.cleared && confirmation.cleared)
            assertFalse(model.form.busy)
            assertEquals("Traveller", model.name.value)
            assertEquals("new@example.com", model.email.value)
            if (expired) assertNull(model.form.state.value.banner) else assertNotNull(model.form.state.value.banner)
            assertTrue(session.accepted.isEmpty())
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("expired", DataFailure.ReviewExpired, true),
                arrayOf(
                    "validation-expired",
                    DataFailure.Validation(
                        mapOf(
                            "review_expired" to
                                FieldMessage("This review has expired. Update your plan before saving."),
                        ),
                        422,
                    ),
                    true,
                ),
                arrayOf("offline", DataFailure.Offline, false),
                arrayOf("server", DataFailure.Server(503), false),
            )
    }
}
