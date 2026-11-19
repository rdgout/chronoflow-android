package net.chronoflow.feature.auth.model.passkey

import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.datamock.MockMethod
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiPasskeyChallenge
import net.chronoflow.core.model.api.ApiPasskeyLoginInput
import net.chronoflow.core.model.api.UserVerification
import net.chronoflow.core.model.passkey.Base64URL
import net.chronoflow.core.model.passkey.PasskeyFailure
import net.chronoflow.core.navigation.Route
import net.chronoflow.feature.auth.model.AuthBannerKind
import net.chronoflow.feature.auth.model.AuthNavigation
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import net.chronoflow.feature.auth.testing.FakeSecretField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class PasskeySignInTests {
    private suspend fun accepts(
        verified: Boolean,
        twoFactor: Boolean,
        io: kotlinx.coroutines.CoroutineDispatcher,
    ) {
        val fixture =
            PasskeyTestFixture(
                io,
                DeterministicAssertionProvider { challenge ->
                    assertEquals("app.chronoflow.net", challenge.options.rpId)
                    assertEquals(UserVerification.Required, challenge.options.userVerification)
                    assertEquals(32, Base64URL.decode(challenge.options.challenge).size)
                    testAssertion()
                },
                verified,
                twoFactor,
            )
        fixture.model.submitPasskey(FakeSecretField())
        assertEquals(1, fixture.session.accepted.size)
        assertEquals(1, fixture.routes.accepted.size)
        assertEquals(
            if (verified) Route.Today else Route.Verify,
            AuthNavigation.route(
                fixture.routes.accepted
                    .single()
                    .first.account,
            ),
        )
        assertEquals(0, fixture.routes.actions.count { it == "twoFactor" })
        assertNull(fixture.model.navigation.challengeID)
    }

    @Test fun mockPasskeyAcceptsVerifiedUnverifiedAndTwoFactorAccountsDirectlyVerified() =
        runTest(timeout = 60.seconds) {
            accepts(true, false, StandardTestDispatcher(testScheduler))
        }

    @Test fun mockPasskeyAcceptsVerifiedUnverifiedAndTwoFactorAccountsDirectlyUnverified() =
        runTest(timeout = 60.seconds) {
            accepts(false, false, StandardTestDispatcher(testScheduler))
        }

    @Test fun mockPasskeyAcceptsVerifiedUnverifiedAndTwoFactorAccountsDirectlyTwoFactor() =
        runTest(timeout = 60.seconds) {
            accepts(true, true, StandardTestDispatcher(testScheduler))
        }

    @Test fun canceledPromptIsSilentAndRetryAlwaysGetsFreshOptions() =
        runTest(timeout = 60.seconds) {
            val challenges = mutableListOf<String>()
            val f =
                PasskeyTestFixture(
                    StandardTestDispatcher(testScheduler),
                    DeterministicAssertionProvider {
                        challenges += it.challengeId
                        if (challenges.size == 1) throw PasskeyFailure.Canceled
                        testAssertion()
                    },
                )
            f.model.submitPasskey(FakeSecretField())
            assertFalse(f.model.form.busy)
            assertNull(f.model.form.state.value.banner)
            assertNull(f.model.form.state.value.failure)
            assertTrue(f.session.accepted.isEmpty())
            f.model.submitPasskey(FakeSecretField())
            assertEquals(2, challenges.size)
            assertNotEquals(challenges[0], challenges[1])
            assertEquals(1, f.session.accepted.size)
        }

    @Test fun consumedExpiredCredentialThrottleServerAndOfflineFailuresRetryWithoutSession() =
        runTest(timeout = 60.seconds) {
            val challenges = mutableSetOf<String>()
            val f =
                PasskeyTestFixture(
                    StandardTestDispatcher(testScheduler),
                    DeterministicAssertionProvider {
                        assertTrue(challenges.add(it.challengeId))
                        testAssertion()
                    },
                )

            suspend fun attempt(failure: DataFailure) {
                f.source.failNext(MockMethod.PasskeyLogin, failure)
                f.model.submitPasskey(FakeSecretField())
                assertEquals(failure, f.model.form.state.value.failure)
                assertFalse(f.model.form.busy)
                assertTrue(f.session.accepted.isEmpty())
            }
            attempt(
                DataFailure.Validation(
                    mapOf("challengeId" to FieldMessage("The passkey request expired. Please try again.")),
                    422,
                ),
            )
            attempt(
                DataFailure.Validation(
                    mapOf("credential" to FieldMessage("We couldn't sign you in with that passkey. Please try again.")),
                    422,
                ),
            )
            attempt(DataFailure.Throttled(87.0, 429))
            attempt(DataFailure.Server(500))
            attempt(DataFailure.Offline)
            f.model.submitPasskey(FakeSecretField())
            assertEquals(6, challenges.size)
            assertEquals(1, f.session.accepted.size)
        }

    @Test fun failedCredentialConsumesChallengeAndRetryFetchesAnother() =
        runTest(timeout = 60.seconds) {
            val challenges = mutableListOf<ApiPasskeyChallenge>()
            val f =
                PasskeyTestFixture(
                    StandardTestDispatcher(testScheduler),
                    DeterministicAssertionProvider {
                        challenges += it
                        if (challenges.size == 1) testAssertion().copy(id = "Ag", rawId = "Ag") else testAssertion()
                    },
                )
            f.model.submitPasskey(FakeSecretField())
            assertEquals(AuthBannerKind.Server, f.model.form.state.value.banner)
            try {
                f.source.passkeyLogin(
                    ApiPasskeyLoginInput(challenges.first().challengeId, testAssertion(), AUTH_DEVICE_ID, "test"),
                )
                fail("Used challenge must be rejected")
            } catch (failure: DataFailure.Validation) {
                assertEquals(
                    mapOf("challengeId" to FieldMessage("The passkey request expired. Please try again.")),
                    failure.fields,
                )
            }
            f.model.submitPasskey(FakeSecretField())
            assertEquals(2, challenges.size)
            assertNotEquals(challenges[0].challengeId, challenges[1].challengeId)
            assertEquals(1, f.session.accepted.size)
        }

    @Test fun taskCancellationAfterAssertionNeverAcceptsOrCallsLogin() =
        runTest(timeout = 60.seconds) {
            val f =
                PasskeyTestFixture(
                    StandardTestDispatcher(testScheduler),
                    DeterministicAssertionProvider {
                        currentCoroutineContext().cancel()
                        testAssertion()
                    },
                )
            launch { f.model.submitPasskey(FakeSecretField()) }.join()
            assertTrue(f.session.accepted.isEmpty())
            assertTrue(f.routes.accepted.isEmpty())
            assertEquals(0, f.loginCalls)
            assertNull(f.model.form.state.value.banner)
            assertFalse(f.model.form.busy)
        }
}
