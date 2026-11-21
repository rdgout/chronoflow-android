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
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class SecurityCodeTests(
    private val value: String,
) {
    @Test fun codeMustBeSixAsciiDigits() =
        runTest(timeout = 60.seconds) {
            val moment = Instant.ofEpochSecond(100)
            val model = SettingsSecurityModel({ error("Invalid code reached the server") }, now = { moment })
            val code = FakeSecretField(value)
            model.confirm(code)
            assertEquals("Enter the 6-digit code from your authenticator app.", model.state.value.errors["code"])
            assertFalse(code.cleared)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() = listOf("12345", "12345a", "١٢٣٤٥٦")
    }
}
