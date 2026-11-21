package net.chronoflow.feature.settings.security

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.ApiRecoveryCodes
import net.chronoflow.core.model.api.ApiTwoFactorSetup
import net.chronoflow.feature.settings.testing.FakeSecretField
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class SecurityValidationTests(
    private val key: String,
    private val expected: String,
    private val field: String,
) {
    @Test fun policyKeysMapToTheIosCopy() =
        runTest(timeout = 60.seconds) {
            val model =
                SettingsSecurityModel({
                    throw DataFailure.Validation(
                        mapOf(
                            field to
                                FieldMessage(
                                    key,
                                    mapOf("min" to JsonPrimitive(12)),
                                ),
                        ),
                        422,
                    )
                }) { Instant.ofEpochSecond(100) }
            model.showPrompt(SettingsSecurityPrompt.Reveal)
            model.submitPrompt(FakeSecretField("pw"))
            assertEquals(expected, model.state.value.errors[field] ?: model.state.value.message)
            if (key == "Use at least :min characters.") {
                assertEquals(
                    mapOf("min" to "12"),
                    model.state.value.errorArguments[field],
                )
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}-{2}")
        fun cases(): List<Array<String>> =
            listOf(
                arrayOf(
                    "The provided password was incorrect.",
                    "The provided password was incorrect.",
                    "current_password",
                ),
                arrayOf("Use at least :min characters.", "Use at least {{min}} characters.", "password"),
                arrayOf(
                    "Use both uppercase and lowercase letters.",
                    "Use both uppercase and lowercase letters.",
                    "password",
                ),
                arrayOf("Use at least one number.", "Use at least one number.", "password"),
                arrayOf("Use at least one symbol.", "Use at least one symbol.", "password"),
                arrayOf("The passwords don't match yet.", "The passwords don't match.", "password_confirmation"),
                arrayOf(
                    "This password has appeared in a data leak. Choose a different password.",
                    "Check the highlighted field.",
                    "password",
                ),
                arrayOf(
                    "The authentication code was not accepted. Try again.",
                    "That code didn't work. Codes change every 30 seconds; try the newest one.",
                    "code",
                ),
                arrayOf("bad", "We couldn't register that passkey. Please try again.", "credential"),
                arrayOf("bad", "We couldn't register that passkey. Please try again.", "name"),
                arrayOf("bad", "The passkey registration request expired. Please try again.", "challengeId"),
            )
    }
}
