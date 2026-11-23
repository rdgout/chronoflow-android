package net.chronoflow.core.datamock

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class MockPasswordConfirmationTests(
    private val value: String,
    private val passwordError: FieldMessage?,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun rows(): List<Array<Any?>> =
            listOf(
                arrayOf(newPassword, null),
                arrayOf("short", FieldMessage("Use at least :min characters.", mapOf("min" to JsonPrimitive(12)))),
                arrayOf("alllowercase12!", FieldMessage("Use both uppercase and lowercase letters.")),
                arrayOf("ALLUPPERCASE12!", FieldMessage("Use both uppercase and lowercase letters.")),
                arrayOf("NoNumbersHere!", FieldMessage("Use at least one number.")),
                arrayOf("NoSymbolsHere12", FieldMessage("Use at least one symbol.")),
            )
    }

    @Test fun confirmationKeepsIndependentPasswordErrors() =
        runTest(timeout = 60.seconds) {
            val source = mockSource()
            val fields =
                mapOf("password_confirmation" to FieldMessage("The passwords don't match yet.")) +
                    if (passwordError == null) emptyMap() else mapOf("password" to passwordError)
            val expected = DataFailure.Validation(fields, 422)
            assertEquals(
                expected,
                failure {
                    source.signUp(registration().copy(password = value, passwordConfirmation = "mismatch"))
                },
            )
            assertTrue(source.inbox().isEmpty())
            source.requestPasswordReset("demo@example.com")
            val reset = (source.inbox().last() as MockMail.PasswordReset).input
            assertEquals(
                expected,
                failure {
                    source.resetPassword(reset.copy(password = value, passwordConfirmation = "mismatch"))
                },
            )
            source.resetPassword(reset.copy(password = newPassword, passwordConfirmation = newPassword))
        }
}
