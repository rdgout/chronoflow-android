package net.chronoflow.feature.settings.passkeys

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.passkey.DeterministicRegistrationProvider
import net.chronoflow.core.model.passkey.PasskeyFailure
import net.chronoflow.core.model.passkey.PasskeyRegistrationProviding
import net.chronoflow.feature.settings.security.SettingsSecurityModel
import net.chronoflow.feature.settings.security.SettingsSecurityPrompt
import net.chronoflow.feature.settings.testing.FakeSecretField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class SettingsPasskeysRegistrationFailureTests(
    private val fault: Int,
) {
    @Test fun registrationFailuresShowTheRegistrationMessageAndSendNothing() =
        runTest(timeout = 60.seconds) {
            val server = PasskeyServer()
            var calls = 0
            var provider: PasskeyRegistrationProviding? =
                DeterministicRegistrationProvider {
                    calls++
                    throw PasskeyFailure.Unavailable
                }
            if (fault == 1) server.optionsChallenge = "not base64url!"
            if (fault == 2) provider = null
            val model = SettingsSecurityModel(server::perform, { Instant.ofEpochSecond(1000) }, provider)
            model.showPrompt(SettingsSecurityPrompt.AddPasskey)
            model.submitPrompt(FakeSecretField("pw"))
            assertEquals(SettingsSecurityModel.REGISTRATION_FAILURE, model.state.value.message)
            assertNull(model.state.value.prompt)
            assertNull(server.added)
            assertFalse(model.state.value.busy)
            assertTrue(
                model.state.value.errors
                    .isEmpty(),
            )
            assertEquals(if (fault == 0) 1 else 0, calls)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "fault={0}")
        fun cases() = listOf(arrayOf(0), arrayOf(1), arrayOf(2))
    }
}
