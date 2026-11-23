package net.chronoflow.core.model.passkey

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.api.ApiPasskeyCreationOptions
import net.chronoflow.core.model.api.ApiPasskeyCredentialDescriptor
import net.chronoflow.core.model.api.CredentialType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SystemPasskeyRegistrationProviderTest {
    private var calls = 0
    private var request: String? = null
    private val client =
        object : CredentialClient {
            override suspend fun getPasskey(requestJson: String): String = error("unused")

            override suspend fun createPasskey(requestJson: String): String {
                calls++
                request = requestJson
                return RegistrationFixture.RESPONSE
            }
        }

    @Test fun passesTheCreationJsonAndConvertsTheResponse() =
        runTest(timeout = 60.seconds) {
            val result = SystemPasskeyRegistrationProvider(client).registration(RegistrationFixture.challenge)
            assertEquals(PasskeyJson.creationJson(RegistrationFixture.challenge.options), request)
            assertEquals(PasskeyJson.attestation(RegistrationFixture.RESPONSE), result)
            assertEquals(1, calls)
        }

    @Test fun invalidChallengeUserIdOrExcludedIdFailsBeforeTheCeremonyChallenge() =
        runTest(timeout = 60.seconds) {
            invalid(RegistrationFixture.challenge.options.copy(challenge = "not base64url!"))
        }

    @Test fun invalidChallengeUserIdOrExcludedIdFailsBeforeTheCeremonyUserId() =
        runTest(timeout = 60.seconds) {
            val options = RegistrationFixture.challenge.options
            invalid(options.copy(user = options.user.copy(id = "!")))
        }

    @Test fun invalidChallengeUserIdOrExcludedIdFailsBeforeTheCeremonyExcludedId() =
        runTest(timeout = 60.seconds) {
            invalid(
                RegistrationFixture.challenge.options.copy(
                    excludeCredentials = listOf(ApiPasskeyCredentialDescriptor(CredentialType.PublicKey, "!")),
                ),
            )
        }

    private suspend fun invalid(options: ApiPasskeyCreationOptions) {
        try {
            SystemPasskeyRegistrationProvider(
                client,
            ).registration(RegistrationFixture.challenge.copy(options = options))
            fail("Expected invalid encoding")
        } catch (failure: PasskeyFailure) {
            assertSame(PasskeyFailure.InvalidEncoding, failure)
        }
        assertEquals(0, calls)
    }

    @Test fun emptyRelyingPartyIsUnavailable() =
        runTest(timeout = 60.seconds) {
            val options = RegistrationFixture.challenge.options
            try {
                SystemPasskeyRegistrationProvider(client).registration(
                    RegistrationFixture.challenge.copy(options = options.copy(rp = options.rp.copy(id = ""))),
                )
                fail("Expected unavailable")
            } catch (failure: PasskeyFailure) {
                assertSame(PasskeyFailure.Unavailable, failure)
            }
            assertEquals(0, calls)
        }

    @Test fun cancellationPropagates() =
        runTest(timeout = 60.seconds) {
            val canceled =
                object : CredentialClient {
                    override suspend fun getPasskey(requestJson: String): String = error("unused")

                    override suspend fun createPasskey(requestJson: String): String = throw PasskeyFailure.Canceled
                }
            try {
                SystemPasskeyRegistrationProvider(canceled).registration(RegistrationFixture.challenge)
                fail("Expected cancellation")
            } catch (failure: PasskeyFailure) {
                assertSame(PasskeyFailure.Canceled, failure)
            }
        }
}
