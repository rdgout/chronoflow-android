package net.chronoflow.feature.auth.model

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.datamock.MockMethod
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.auth.AuthenticationMethod
import net.chronoflow.core.model.passkey.PasskeyCapability
import net.chronoflow.core.model.passkey.PasskeyFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.Fixtures
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import net.chronoflow.feature.auth.testing.AuthTestFixture
import net.chronoflow.feature.auth.testing.FakeSecretField
import net.chronoflow.feature.auth.testing.FakeSessionController
import net.chronoflow.feature.auth.testing.RecordingRoutes
import net.chronoflow.feature.auth.testing.authTestSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Enclosed::class)
class SignInModelTests {
    @RunWith(Parameterized::class)
    class Verified(
        private val verified: Boolean,
    ) {
        @Test
        fun verifiedAndUnverifiedAcceptOnceAndClearSecrets() {
            runTest(timeout = 60.seconds) {
                val fixture = AuthTestFixture(StandardTestDispatcher(testScheduler), verified = verified)
                val model = fixture.model
                val secret = FakeSecretField()
                assertEquals("", model.email.value)
                model.editEmail("demo@example.com")
                model.submitPassword(secret) {}
                assertEquals(1, fixture.session.accepted.size)
                assertEquals(1, fixture.routes.accepted.size)
                val expected = if (verified) Route.Today else Route.Verify
                assertEquals(
                    expected,
                    AuthNavigation.route(
                        fixture.routes.accepted
                            .single()
                            .first.account,
                    ),
                )
                assertTrue(secret.cleared)
                assertEquals("", secret.read())
                assertEquals("demo@example.com", model.email.value)
                assertFalse(model.form.busy)
            }
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "verified={0}")
            fun cases() = listOf(true, false)
        }
    }

    @RunWith(Parameterized::class)
    class StoreFailures(
        private val failure: SessionFailure,
        private val method: AuthenticationMethod,
    ) {
        @Test
        fun storeFailuresShowAndAnnounceTheServerBannerWithoutRouting() {
            runTest(timeout = 60.seconds) {
                val fixture =
                    AuthTestFixture(
                        StandardTestDispatcher(testScheduler),
                        capability = true,
                        passkey = { sessionResult() },
                    )
                fixture.session.failure = failure
                fixture.model.editEmail("demo@example.com")
                val secret = FakeSecretField()
                if (method == AuthenticationMethod.Password) {
                    fixture.model.submitPassword(secret) {}
                } else {
                    fixture.model.submitPasskey(secret)
                }
                assertTrue(fixture.routes.accepted.isEmpty())
                assertEquals(AuthBannerKind.Server, fixture.model.form.state.value.banner)
                assertTrue(secret.cleared)
                assertEquals(failure, fixture.model.sessionFailure)
                assertNotNull(fixture.model.form.takeAnnouncement { it.key })
                assertFalse(fixture.model.form.busy)
            }
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}-{1}")
            fun cases(): List<Array<Any>> =
                listOf(
                    SessionFailure.CredentialsUnavailable,
                    SessionFailure.DatabaseUnavailable,
                    SessionFailure.CredentialWrite,
                    SessionFailure.InvalidResult,
                    SessionFailure.Superseded,
                ).flatMap { failure ->
                    listOf(
                        arrayOf(failure, AuthenticationMethod.Password),
                        arrayOf(failure, AuthenticationMethod.Passkey),
                    )
                }
        }
    }

    class Behavior {
        @Test fun clearSecretsCancelsTheTwoFactorModel() =
            runTest(timeout = 60.seconds) {
                val signIn = challenged(twoFactorSource(StandardTestDispatcher(testScheduler)))
                val old = signIn.twoFactor.value!!
                old.editCode("123456")
                signIn.clearSecrets()
                old.editCode("1")
                assertEquals("", old.code)
                assertNull(signIn.twoFactor.value)
                assertNull(signIn.navigation.challengeID)
                assertEquals("twofactor@example.com", signIn.email.value)
            }

        @Test
        fun twoFactorNeverAcceptsSessionAndBackDiscardsChallenge() {
            runTest(timeout = 60.seconds) {
                val fixture = AuthTestFixture(StandardTestDispatcher(testScheduler), twoFactor = true)
                val secret = FakeSecretField()
                fixture.model.editEmail("demo@example.com")
                fixture.model.submitPassword(secret) {}
                assertEquals(listOf("twoFactor"), fixture.routes.actions)
                assertNotNull(fixture.model.navigation.challengeID)
                assertTrue(fixture.session.accepted.isEmpty())
                assertTrue(secret.cleared)
                fixture.model.navigation.discardChallenge()
                assertNull(fixture.model.navigation.challengeID)
            }
        }

        @Test
        fun blankCredentialsThrottleServerAndOfflinePreserveEmailAndClearPassword() {
            runTest(timeout = 60.seconds) {
                val fixture = AuthTestFixture(StandardTestDispatcher(testScheduler))
                val model = fixture.model
                val secret = FakeSecretField("")
                model.submitPassword(secret) {}
                assertEquals(
                    "Please fill this in.",
                    model.form.state.value.fields["email"]
                        ?.key,
                )
                model.editEmail("retained@example.com")
                secret.value = "wrong"
                model.submitPassword(secret) {}
                assertEquals(AuthBannerKind.Credentials, model.form.state.value.banner)
                assertTrue(
                    model.form.state.value.fields
                        .isEmpty(),
                )
                assertEquals("", secret.read())

                suspend fun attempt(failure: DataFailure) {
                    fixture.source.failNext(MockMethod.SignIn, failure)
                    model.editPassword()
                    assertNull(model.form.state.value.banner)
                    secret.value = "another attempt"
                    model.submitPassword(secret) {}
                    assertEquals(failure, model.form.state.value.failure)
                    assertEquals("", secret.read())
                    assertEquals("retained@example.com", model.email.value)
                }
                attempt(DataFailure.Server(500))
                attempt(DataFailure.Throttled(40.0, 429))
                attempt(DataFailure.Offline)
                assertEquals(AuthBannerKind.Offline, model.form.state.value.banner)
            }
        }

        @Test
        fun unchangedEmailPreservesServerFeedbackUntilTheValueChanges() {
            runTest(timeout = 60.seconds) {
                val model = AuthTestFixture(StandardTestDispatcher(testScheduler)).model
                model.editEmail("demo@example.com")
                val message = FieldMessage("Please fill this in.")
                val failure = DataFailure.Validation(mapOf("password" to message, "device_id" to message), 422)
                model.form.receive(failure)
                model.editEmail(model.email.value)
                assertEquals(AuthBannerKind.Server, model.form.state.value.banner)
                assertEquals(message, model.form.state.value.fields["password"])
                assertEquals(failure, model.form.state.value.failure)
                model.editEmail("changed@example.com")
                assertNull(model.form.state.value.banner)
                assertNull(model.form.state.value.failure)
            }
        }

        @Test
        fun concurrentAttemptsAndEditingWhileBusyAreIgnored() {
            runTest(timeout = 60.seconds) {
                val gate = CompletableDeferred<String>()
                val fixture =
                    AuthTestFixture(
                        StandardTestDispatcher(testScheduler),
                        capability = true,
                        identity = { gate.await() },
                    )
                val model = fixture.model
                model.editEmail("demo@example.com")
                val secret = FakeSecretField()
                val attempt = launch { model.submitPassword(secret) {} }
                runCurrent()
                assertTrue(model.form.busy)
                val ignored = FakeSecretField()
                model.submitPassword(ignored) {}
                model.submitPasskey(ignored)
                model.editEmail("changed")
                model.editPassword()
                assertFalse(ignored.cleared)
                gate.complete(AUTH_DEVICE_ID)
                attempt.join()
                assertEquals(1, fixture.session.accepted.size)
                assertEquals("demo@example.com", model.email.value)
                assertTrue(secret.cleared)
            }
        }

        @Test
        fun accountGatePrecedenceUsesAvailableAccountRecord() {
            val account = sessionResult().account
            assertEquals(
                Route.Verify,
                AuthNavigation.route(account.copy(hasVerifiedEmail = false, walkthroughSeen = false)),
            )
            assertEquals(
                Route.Terms,
                AuthNavigation.route(
                    account.copy(
                        hasVerifiedEmail = true,
                        policies = AccountPolicies("2", "2", "1"),
                        walkthroughSeen = false,
                    ),
                ),
            )
            assertEquals(
                Route.Walkthrough(false),
                AuthNavigation.route(
                    account.copy(
                        hasVerifiedEmail = true,
                        policies = AccountPolicies("2", "2", "2"),
                        walkthroughSeen = false,
                    ),
                ),
            )
            assertEquals(
                Route.Today,
                AuthNavigation.route(
                    account.copy(
                        hasVerifiedEmail = true,
                        policies = AccountPolicies("2", "2", "2"),
                        walkthroughSeen = true,
                    ),
                ),
            )
        }

        @Test
        fun autofillIsCommittedBeforeThePasswordIsCleared() {
            runTest(timeout = 60.seconds) {
                val order = mutableListOf<String>()
                val fixture = AuthTestFixture(StandardTestDispatcher(testScheduler))
                fixture.session.onAccept = { order += "accept" }
                val model =
                    SignInModel(fixture.source, fixture.session, { AUTH_DEVICE_ID }, { "test" }, accepted = { _, _ ->
                        order +=
                            "accepted"
                    }, onTwoFactor = {})
                model.editEmail("demo@example.com")
                val secret = FakeSecretField(onClear = { order += "clear" })
                model.submitPassword(secret) {
                    assertEquals("Password123!", secret.read())
                    order += "commitAutofill"
                }
                assertEquals(listOf("accept", "commitAutofill", "clear", "accepted", "clear"), order)
                order.clear()
                secret.value = "wrong"
                model.submitPassword(secret) { order += "commitAutofill" }
                assertEquals(listOf("clear"), order)
            }
        }

        @Test
        fun passkeyCancelIsSilent() {
            runTest(timeout = 60.seconds) {
                val fixture =
                    AuthTestFixture(
                        StandardTestDispatcher(testScheduler),
                        capability = true,
                        passkey = { throw PasskeyFailure.Canceled },
                    )
                val secret = FakeSecretField()
                fixture.model.submitPasskey(secret)
                assertNull(fixture.model.form.state.value.banner)
                assertNull(fixture.model.form.takeAnnouncement { it.key })
                assertFalse(fixture.model.form.busy)
                assertTrue(secret.cleared)
            }
        }

        @Test
        fun passkeyIsANoOpWhileTheCapabilityIsOff() {
            runTest(timeout = 60.seconds) {
                var calls = 0
                val fixture =
                    AuthTestFixture(StandardTestDispatcher(testScheduler), passkey = {
                        calls++
                        sessionResult()
                    })
                val secret = FakeSecretField()
                fixture.model.submitPasskey(secret)
                assertEquals(0, calls)
                assertFalse(fixture.model.form.busy)
                assertFalse(secret.cleared)
            }
        }

        @Test
        fun unavailablePasskeyShowsTheServerBanner() {
            runTest(timeout = 60.seconds) {
                val fixture = AuthTestFixture(StandardTestDispatcher(testScheduler), capability = true)
                val secret = FakeSecretField()
                fixture.model.submitPasskey(secret)
                assertEquals(AuthBannerKind.Server, fixture.model.form.state.value.banner)
                assertTrue(secret.cleared)
            }
        }

        @Test
        fun clearSecretsKeepsTheEmail() {
            runTest(timeout = 60.seconds) {
                val model = AuthTestFixture(StandardTestDispatcher(testScheduler)).model
                model.editEmail("demo@example.com")
                model.navigation.twoFactor("artificial-challenge")
                model.form.receive(DataFailure.Offline)
                model.clearSecrets()
                assertEquals("demo@example.com", model.email.value)
                assertNull(model.navigation.challengeID)
                assertNull(model.form.state.value.failure)
            }
        }

        @Test
        fun cancellationRethrowsAndUnlocksTheForm() {
            runTest(timeout = 60.seconds) {
                val gate = CompletableDeferred<String>()
                val fixture = AuthTestFixture(StandardTestDispatcher(testScheduler), identity = { gate.await() })
                val secret = FakeSecretField()
                val request = launch { fixture.model.submitPassword(secret) {} }
                runCurrent()
                assertTrue(fixture.model.form.busy)
                request.cancel()
                request.join()
                assertTrue(request.isCancelled)
                assertFalse(fixture.model.form.busy)
                assertTrue(secret.cleared)
                assertNull(fixture.model.form.state.value.banner)
            }
        }
    }
}

private fun sessionResult(): SessionResult =
    ApiJson.response.decodeFromString(SessionResult.serializer(), Fixtures.text("auth/session.json"))
