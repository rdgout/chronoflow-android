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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class SecurityConflictTests(
    private val key: String,
    private val retainSetup: Boolean,
) {
    @Test fun conflictSentencesClearSetupAndCodesAndBecomeMessages() =
        runTest(timeout = 60.seconds) {
            var called = false
            val model =
                SettingsSecurityModel({
                    if (called) throw DataFailure.Validation(mapOf("form" to FieldMessage(key)), 409)
                    called = true
                    if (retainSetup) {
                        AccountSecurityResult.Setup(ApiTwoFactorSetup("secret", "otpauth://test"))
                    } else {
                        AccountSecurityResult.Codes(ApiRecoveryCodes(listOf("secret-code")))
                    }
                }, now = { Instant.ofEpochSecond(100) })
            model.showPrompt(SettingsSecurityPrompt.Enable)
            model.submitPrompt(FakeSecretField("pw"))
            assertTrue(
                model.state.value.setup != null ||
                    model.state.value.codes
                        .isNotEmpty(),
            )
            model.showPrompt(SettingsSecurityPrompt.Reveal)
            model.submitPrompt(FakeSecretField("pw"))
            assertEquals(key, model.state.value.message)
            assertNull(model.state.value.setup)
            assertTrue(
                model.state.value.codes
                    .isEmpty(),
            )
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-setup={1}")
        fun cases() =
            listOf(
                arrayOf<Any>("Two-factor authentication is already on.", true),
                arrayOf<Any>("Two-factor authentication is off.", false),
                arrayOf<Any>("Start two-factor setup again.", true),
            )
    }
}
