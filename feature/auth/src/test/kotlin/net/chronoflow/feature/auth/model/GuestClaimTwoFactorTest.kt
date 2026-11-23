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

class GuestClaimTwoFactorTest {
    @Test fun twoFactorInputHasNoClaimFields() =
        runTest(timeout = 60.seconds) {
            val source = ClaimAuthSource(StandardTestDispatcher(testScheduler), true).apply { twoFactor = true }
            val session = FakeSessionController()
            val model =
                SignInModel(source, session, { "device" }, { "phone" }, accepted = { _, method ->
                    assertEquals(AuthenticationMethod.TwoFactor, method)
                }, onTwoFactor = {}, claim = { GuestClaim("guest-token", 4, 7, "secret") })
            model.submitPassword(FakeSecretField()) {}
            val second = checkNotNull(model.twoFactor.value)
            second.editCode("123456")
            second.submit()
            assertNotNull(source.twoFactorInput)
            val properties = TwoFactorInput::class.java.declaredFields.map { it.name }
            assertFalse("draftToken" in properties || "guestSecret" in properties)
            assertEquals(1, session.accepted.size)
        }
}
