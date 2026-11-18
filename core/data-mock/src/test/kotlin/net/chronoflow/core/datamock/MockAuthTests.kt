package net.chronoflow.core.datamock

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.api.TwoFactorInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class MockAuthTests {
    /**
     * Empty, unseeded non-debug clamp probes preserve iOS's 401 assertion
     * without paying for unused fixture rebases.
     */
    @Test fun latencyOfflineAndFailOnce() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val source = mockSource(clock)
            source.setOffline(true)
            source.failNext(MockMethod.SignIn, DataFailure.Server(503))
            assertEquals(DataFailure.Offline, failure { source.signIn(passwordInput()) })
            source.setOffline(false)
            assertEquals(DataFailure.Server(503), failure { source.signIn(passwordInput()) })
            login(source)
            assertEquals(3, clock.delays.size)
            assertTrue(clock.delays.all { it in 0.3..0.8 })

            suspend fun clamp(value: Double) {
                val clamped =
                    MockDataSource(now = clock::now, delay = {
                        value
                    }, sleep = clock::sleep, debugAppSeed = false, io = Dispatchers.Unconfined, zone = mockZone)
                assertEquals(DataFailure.Unauthorized(401), failure { clamped.fetchIdentity() })
            }
            clamp(-1.0)
            clamp(0.3)
            clamp(0.8)
            clamp(10.0)
            assertEquals(listOf(0.3, 0.3, 0.8, 0.8), clock.delays.takeLast(4))
        }

    @Test fun fiveAttemptsThenWindowReset() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val source = mockSource(clock)
            repeat(5) { login(source) }
            assertEquals(DataFailure.Throttled(60.0, 429), failure { login(source) })
            clock.advance(60.0)
            login(source)
            failure { source.signIn(passwordInput(password = "wrong")) }
        }

    @Test fun emptyTwoFactorProofReturnsBothRequiredWithoutErrorsCodeEmpty() = emptyProof(false, "")

    @Test fun emptyTwoFactorProofReturnsBothRequiredWithoutErrorsCodeWhitespace() = emptyProof(false, " \n")

    @Test fun emptyTwoFactorProofReturnsBothRequiredWithoutErrorsRecoveryEmpty() = emptyProof(true, "")

    @Test fun emptyTwoFactorProofReturnsBothRequiredWithoutErrorsRecoveryWhitespace() = emptyProof(true, " \n")

    private fun emptyProof(
        recovery: Boolean,
        value: String,
    ) = runTest(timeout = 60.seconds) {
        val expected =
            DataFailure.Validation(
                mapOf(
                    "code" to FieldMessage("Enter the 6-digit code from your authenticator app."),
                    "recovery_code" to
                        FieldMessage(
                            "Enter one of the recovery codes you saved when you set up two-factor authentication.",
                        ),
                ),
                422,
            )
        assertEquals(
            expected,
            failure {
                mockSource().completeTwoFactor(
                    TwoFactorInput(
                        "unused",
                        if (recovery) TwoFactorInput.Proof.RecoveryCode(value) else TwoFactorInput.Proof.Code(value),
                    ),
                )
            },
        )
    }

    @Test fun twoFactorSingleUseAndRecoveryConsumption() =
        runTest(timeout = 60.seconds) {
            val source = mockSource()
            val challenge =
                (source.signIn(passwordInput(email = "twofactor@example.com")) as SignInResult.TwoFactor).challengeId
            source.completeTwoFactor(TwoFactorInput(challenge, TwoFactorInput.Proof.RecoveryCode(fixtureRecovery)))
            failure { source.completeTwoFactor(TwoFactorInput(challenge, TwoFactorInput.Proof.Code("123456"))) }
            val next =
                (
                    source.signIn(
                        passwordInput(email = "twofactor@example.com"),
                    ) as SignInResult.TwoFactor
                ).challengeId
            failure {
                source.completeTwoFactor(
                    TwoFactorInput(next, TwoFactorInput.Proof.RecoveryCode(fixtureRecovery)),
                )
            }
            assertEquals(
                twoFactorID,
                source.completeTwoFactor(TwoFactorInput(next, TwoFactorInput.Proof.Code("123456"))).account.id,
            )
        }

    @Test fun twoFactorExpiryAndFiveFailures() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val source = mockSource(clock)
            val challenge =
                (source.signIn(passwordInput(email = "twofactor@example.com")) as SignInResult.TwoFactor).challengeId
            repeat(
                5,
            ) { failure { source.completeTwoFactor(TwoFactorInput(challenge, TwoFactorInput.Proof.Code("000000"))) } }
            clock.advance(60.0)
            failure { source.completeTwoFactor(TwoFactorInput(challenge, TwoFactorInput.Proof.Code("123456"))) }
            val next =
                (
                    source.signIn(
                        passwordInput(email = "twofactor@example.com"),
                    ) as SignInResult.TwoFactor
                ).challengeId
            clock.advance(300.0)
            failure { source.completeTwoFactor(TwoFactorInput(next, TwoFactorInput.Proof.Code("123456"))) }
        }

    @Test fun registerDuplicateAndUnverifiedGate() =
        runTest(timeout = 60.seconds) {
            val source = mockSource()
            val session = source.signUp(registration())
            assertFalse(session.account.hasVerifiedEmail)
            assertEquals("new@example.com", source.fetchIdentity().email)
            assertEquals(DataFailure.EmailUnverified(403), failure { source.fetchAccount() })
            assertEquals(DataFailure.EmailUnverified(403), failure { source.fetchTrips() })
            failure { source.signUp(registration("NEW@example.com")) }
            assertEquals(1, source.inbox().size)
        }

    @Test fun forgotNonEnumeratingResetRevokesAllTokensAndOnce() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val store = MemoryMockStateStore()
            val source = mockSource(clock, store)
            val old = login(source)
            login(source, device = twoFactorID)
            source.requestPasswordReset("absent@example.com")
            assertTrue(source.inbox().isEmpty())
            source.requestPasswordReset("demo@example.com")
            val input =
                (source.inbox().last() as MockMail.PasswordReset).input.copy(
                    password = newPassword,
                    passwordConfirmation = newPassword,
                )
            source.resetPassword(input)
            assertEquals(DataFailure.Unauthorized(401), failure { source.fetchIdentity() })
            val restoredOld = mockSource(clock, store, bearer = { old.token })
            assertEquals(DataFailure.Unauthorized(401), failure { restoredOld.fetchIdentity() })
            failure { source.resetPassword(input) }
            assertTrue(source.signIn(passwordInput(password = newPassword)) is SignInResult.Session)
        }

    @Test fun resetExpiryAndConfirmation() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val source = mockSource(clock)
            source.requestPasswordReset("demo@example.com")
            val input =
                (source.inbox().last() as MockMail.PasswordReset).input.copy(
                    password = newPassword,
                    passwordConfirmation = "mismatch",
                )
            assertEquals(
                DataFailure.Validation(
                    mapOf("password_confirmation" to FieldMessage("The passwords don't match yet.")),
                    422,
                ),
                failure { source.resetPassword(input) },
            )
            clock.advance(3600.0)
            assertEquals(
                DataFailure.Validation(
                    mapOf("token" to FieldMessage("This password reset link is invalid or has expired.")),
                    422,
                ),
                failure { source.resetPassword(input.copy(passwordConfirmation = newPassword)) },
            )
        }

    @Test fun verificationResendTamperExpiryAndAccountBinding() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val source = mockSource(clock)
            source.signUp(registration())
            assertEquals(0, source.verificationStatus().resendAfterSeconds)
            assertEquals(DataFailure.Throttled(60.0, 429), failure { source.resendVerificationEmail() })
            val signed = (source.inbox().last() as MockMail.Verification).input
            failure { source.verifyEmail(signed.copy(hash = "tampered")) }
            failure { source.verifyEmail(signed.copy(signature = "tampered")) }
            login(source)
            failure { source.verifyEmail(signed) }
            assertTrue(
                source.signIn(passwordInput(email = "new@example.com", password = newPassword)) is SignInResult.Session,
            )
            clock.advance(3601.0)
            failure { source.verifyEmail(signed) }
            assertEquals(60, source.resendVerificationEmail().resendAfterSeconds)
            val fresh = (source.inbox().last() as MockMail.Verification).input
            assertTrue(source.verifyEmail(fresh).hasVerifiedEmail)
            assertTrue(source.verifyEmail(fresh).hasVerifiedEmail)
            source.fetchAccount()
        }

    @Test fun passkeySingleUseInvalidDoesConsumeAndMalformedDoesNot() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val source = mockSource(clock)
            val options = source.passkeyOptions()
            assertEquals(64, options.challengeId.length)
            assertEquals(43, options.options.challenge.length)
            assertEquals(demoID, source.passkeyLogin(passkeyInput(options)).account.id)
            failure { source.passkeyLogin(passkeyInput(options)) }
            val failed = source.passkeyOptions()
            failure { source.passkeyLogin(passkeyInput(failed, signature = "YmFk")) }
            failure { source.passkeyLogin(passkeyInput(failed)) }
            clock.advance(60.0)
            val valid = source.passkeyOptions()
            val input = passkeyInput(valid)
            failure { source.passkeyLogin(input.copy(credential = input.credential.copy(rawId = "AQ=="))) }
            assertEquals(demoID, source.passkeyLogin(passkeyInput(valid)).account.id)
        }

    @Test fun passkeyExpiryAndBypassesTwoFactorForUnverified() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val seed =
                scenarioState().updateAccount(twoFactorID) {
                    it.copy(snapshot = it.snapshot.with(hasVerifiedEmail = false))
                }
            val source = mockSource(clock, seed = seed)
            assertFalse(source.passkeyLogin(passkeyInput(source.passkeyOptions(), "AQ")).account.hasVerifiedEmail)
            assertEquals(twoFactorID, source.fetchIdentity().id)
            val expired = source.passkeyOptions()
            clock.advance(120.0)
            assertEquals(
                DataFailure.Validation(
                    mapOf("challengeId" to FieldMessage("The passkey request expired. Please try again.")),
                    422,
                ),
                failure { source.passkeyLogin(passkeyInput(expired)) },
            )
        }

    @Test fun passkeyLimitsAndDefaultProviderRejects() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val source = mockSource(clock)
            repeat(10) { source.passkeyOptions() }
            assertEquals(DataFailure.Throttled(60.0, 429), failure { source.passkeyOptions() })
            clock.advance(60.0)
            val options = source.passkeyOptions()
            repeat(5) { failure { source.passkeyLogin(passkeyInput(options, signature = "YmFk")) } }
            assertEquals(DataFailure.Throttled(60.0, 429), failure { source.passkeyLogin(passkeyInput(options)) })
            val denied =
                MockDataSource(
                    seed = scenarioState(),
                    sleep = clock::sleep,
                    io = Dispatchers.Unconfined,
                    zone = mockZone,
                )
            val challenge = denied.passkeyOptions()
            assertEquals(
                DataFailure.Validation(
                    mapOf("credential" to FieldMessage("We couldn't sign you in with that passkey. Please try again.")),
                    422,
                ),
                failure { denied.passkeyLogin(passkeyInput(challenge)) },
            )
        }

    @Test fun passkeyHourlyFailureLimiterCountsOnlyValidationFailures() =
        runTest(timeout = 60.seconds) {
            val clock = MockClock()
            val source = mockSource(clock)
            val options = source.passkeyOptions()
            repeat(4) {
                repeat(5) { failure { source.passkeyLogin(passkeyInput(options, signature = "YmFk")) } }
                clock.advance(60.0)
            }
            assertEquals(DataFailure.Throttled(3360.0, 429), failure { source.passkeyLogin(passkeyInput(options)) })
            clock.advance(3360.0)
            source.passkeyLogin(passkeyInput(source.passkeyOptions()))
        }

    @Test fun passkeyIPLimitSpansCredentials() =
        runTest(timeout = 60.seconds) {
            val source = mockSource()
            val options = source.passkeyOptions()
            repeat(20) { index ->
                val input = passkeyInput(options)
                val encoded = base64Url(byteArrayOf(index.toByte()))
                failure {
                    source.passkeyLogin(
                        input.copy(
                            credential =
                                input.credential.copy(
                                    id = encoded,
                                    rawId = encoded,
                                    response = input.credential.response.copy(signature = "YmFk"),
                                ),
                        ),
                    )
                }
            }
            val next = passkeyInput(options)
            assertEquals(
                DataFailure.Throttled(60.0, 429),
                failure {
                    source.passkeyLogin(next.copy(credential = next.credential.copy(id = "FA", rawId = "FA")))
                },
            )
        }

    @Test fun newPasswordPolicyAndDeviceValidation() =
        runTest(timeout = 60.seconds) {
            val source = mockSource()

            suspend fun invalid(value: String) {
                failure { source.signUp(registration().copy(password = value, passwordConfirmation = value)) }
            }
            invalid("short")
            invalid("alllowercase12!")
            invalid("ALLUPPERCASE12!")
            invalid("NoNumbersHere!")
            invalid("NoSymbolsHere12")
            val invalid = mockSource()
            failure { invalid.signUp(registration().copy(deviceID = "invalid")) }
            failure { invalid.signUp(registration().copy(deviceName = "a".repeat(256))) }
            failure { invalid.signUp(registration().copy(name = "")) }
            failure { invalid.requestPasswordReset("invalid") }
        }

    @Test fun canceledMockCallNeverChangesState() =
        runTest(timeout = 60.seconds) {
            val source = mockSource()
            val task =
                async {
                    currentCoroutineContext().cancel()
                    source.signUp(registration())
                }
            try {
                task.await()
                fail("expected cancellation")
            } catch (_: CancellationException) {
            }
            assertTrue(source.inbox().isEmpty())
        }

    /** Unicode password policy coverage (new). */
    @Test fun passwordPolicyUsesUnicodeCategories() =
        runTest(timeout = 60.seconds) {
            val source = mockSource()
            source.signUp(registration().copy(password = "Äbcdefghijk1!", passwordConfirmation = "Äbcdefghijk1!"))
            assertEquals(
                DataFailure.Validation(
                    mapOf("password" to FieldMessage("Use both uppercase and lowercase letters.")),
                    422,
                ),
                failure {
                    mockSource().signUp(
                        registration().copy(password = "ÄBCDEFGHIJK1!", passwordConfirmation = "ÄBCDEFGHIJK1!"),
                    )
                },
            )
        }
}
