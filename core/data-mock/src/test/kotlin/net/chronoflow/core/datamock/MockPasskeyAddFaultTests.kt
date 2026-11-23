package net.chronoflow.core.datamock

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.ApiPasskeyRegistrationInput
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class MockPasskeyAddFaultTests(
    private val fault: String,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun rows(): List<Array<String>> =
            listOf(
                arrayOf("challenge"),
                arrayOf("expired"),
                arrayOf("attestation"),
                arrayOf("padded"),
                arrayOf("duplicate"),
                arrayOf("name"),
                arrayOf("long"),
            )
    }

    @Test fun addRejectsEachBadInputWithoutRegistering() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val source = mockSource(clock, seed = passkeyState())
            login(source)
            if (fault ==
                "duplicate"
            ) {
                val first = options(source)
                source.accountSecurity(
                    AccountSecurityChange.AddPasskey(
                        ApiPasskeyRegistrationInput(first.challengeId, "One", attestation(first)),
                    ),
                )
            }
            val challenge = options(source)
            var input = ApiPasskeyRegistrationInput(challenge.challengeId, "Phone", attestation(challenge))
            input =
                when (fault) {
                    "challenge" -> {
                        input.copy(challengeId = "unknown")
                    }

                    "expired" -> {
                        clock.advance(121.0)
                        input
                    }

                    "attestation" -> {
                        input.copy(
                            credential =
                                input.credential.copy(
                                    response = input.credential.response.copy(attestationObject = "not base64url!"),
                                ),
                        )
                    }

                    "padded" -> {
                        input.copy(credential = input.credential.copy(id = "AQID="))
                    }

                    "duplicate" -> {
                        input
                    }

                    "long" -> {
                        input.copy(name = "a".repeat(256))
                    }

                    else -> {
                        input.copy(name = "  ")
                    }
                }
            val before = list(source).size
            val failure =
                failure {
                    source.accountSecurity(
                        AccountSecurityChange.AddPasskey(input),
                    )
                } as DataFailure.Validation
            assertEquals(422, failure.status)
            when (fault) {
                "name" -> {
                    assertEquals("Please fill this in.", failure.fields["name"]?.key)
                }

                "long" -> {
                    assertEquals(
                        FieldMessage("Use no more than :max characters.", mapOf("max" to JsonPrimitive(255))),
                        failure.fields["name"],
                    )
                }

                "challenge", "expired" -> {
                    assertEquals(
                        "The passkey registration request expired. Please try again.",
                        failure.fields["challengeId"]?.key,
                    )
                }

                else -> {
                    assertEquals(
                        "We couldn't register that passkey. Please try again.",
                        failure.fields["credential"]?.key,
                    )
                }
            }
            assertEquals(before, list(source).size)
        }
}
