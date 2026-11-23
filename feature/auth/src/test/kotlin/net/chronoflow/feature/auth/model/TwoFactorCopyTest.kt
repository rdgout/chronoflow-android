package net.chronoflow.feature.auth.model

import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.auth.shell.resolve
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Logic
@Config(sdk = [36])
@RunWith(ParameterizedRobolectricTestRunner::class)
class TwoFactorCopyTest(
    private val name: String,
    private val language: String,
    private val key: String,
    private val expected: String,
) {
    @Test fun fieldAndThrottleCopyResolve() {
        RuntimeEnvironment.setQualifiers(language)
        val resources = RuntimeEnvironment.getApplication().resources
        val actual =
            if (key == "throttle") {
                val form = AuthFormModel(setOf("code"), AuthFormContext.TwoFactor)
                form.receive(DataFailure.Throttled(40.0, 429))
                form.takeAnnouncement { it.resolve(resources) }
            } else {
                checkNotNull(AuthFieldCopy.field(FieldMessage(key))).resolve(resources)
            }
        assertEquals(expected, actual)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases() =
            listOf(
                arrayOf(
                    "CodeMissing",
                    "en",
                    "Enter the 6-digit code from your authenticator app.",
                    "Enter the 6-digit code from your authenticator app.",
                ),
                arrayOf(
                    "CodeMissing",
                    "nl",
                    "Enter the 6-digit code from your authenticator app.",
                    "Vul de 6-cijferige code uit je authenticator-app in.",
                ),
                arrayOf(
                    "CodeRejected",
                    "en",
                    "That code didn't work. Codes change every 30 seconds, so try the newest one.",
                    "That code didn't work. Codes change every 30 seconds, so try the newest one.",
                ),
                arrayOf(
                    "CodeRejected",
                    "nl",
                    "That code didn't work. Codes change every 30 seconds, so try the newest one.",
                    "Die code werkte niet. Codes veranderen elke 30 seconden, dus probeer de nieuwste.",
                ),
                arrayOf(
                    "RecoveryMissing",
                    "en",
                    "Enter one of the recovery codes you saved when you set up two-factor authentication.",
                    "Enter one of the recovery codes you saved when you set up two-factor authentication.",
                ),
                arrayOf(
                    "RecoveryMissing",
                    "nl",
                    "Enter one of the recovery codes you saved when you set up two-factor authentication.",
                    "Vul een van de herstelcodes in die je hebt bewaard bij het instellen van tweefactorauthenticatie.",
                ),
                arrayOf(
                    "RecoveryRejected",
                    "en",
                    "This recovery code was not accepted. Try another saved code.",
                    "This recovery code was not accepted. Try another saved code.",
                ),
                arrayOf(
                    "RecoveryRejected",
                    "nl",
                    "This recovery code was not accepted. Try another saved code.",
                    "Deze herstelcode is niet geaccepteerd. Probeer een andere bewaarde code.",
                ),
                arrayOf("Throttle", "en", "throttle", "Too many attempts. Wait a few minutes before you try again."),
                arrayOf(
                    "Throttle",
                    "nl",
                    "throttle",
                    "Te veel pogingen. Wacht een paar minuten voordat je het opnieuw probeert.",
                ),
            )
    }
}
