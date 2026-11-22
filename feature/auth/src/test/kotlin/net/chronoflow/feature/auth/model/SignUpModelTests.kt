package net.chronoflow.feature.auth.model

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.datamock.MockMethod
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.api.AuthLanguage
import net.chronoflow.core.model.api.RegisterInput
import net.chronoflow.core.model.api.SessionResult
import net.chronoflow.core.model.auth.AuthenticationMethod
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.guest.GuestClaim
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.navigation.Route
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
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
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Enclosed::class)
class SignUpModelTests {
    class Behavior {
        @Test fun registrationUsesDeviceLanguageAndTheExistingSessionController() =
            runTest(timeout = 60.seconds) {
                val f = fixture(deviceName = "Test phone")
                var payload: RegisterInput? = null
                val source =
                    object : DataSource by f.source {
                        override suspend fun signUp(input: RegisterInput): SessionResult {
                            payload = input
                            return f.source.signUp(input)
                        }
                    }
                val signIn =
                    SignInModel(source, f.session, {
                        AUTH_DEVICE_ID
                    }, { "Test phone" }, accepted = f.routes::acceptedSession, onTwoFactor = {})
                val model = signIn.makeSignUpModel {}
                model.editName("Nieuw")
                model.editEmail("new@example.com")
                val password = FakeSecretField("NewPassword123!")
                val confirmation = FakeSecretField("NewPassword123!")
                var committed = false
                model.submit(password, confirmation, AuthLanguage.Nl) {
                    assertEquals("NewPassword123!", password.read())
                    assertEquals("NewPassword123!", confirmation.read())
                    assertEquals(1, f.session.accepted.size)
                    committed = true
                }
                assertTrue(committed)
                assertEquals(1, f.session.accepted.size)
                assertEquals(
                    AuthenticationMethod.SignUp,
                    f.routes.accepted
                        .single()
                        .second,
                )
                assertEquals(
                    Route.Verify,
                    AuthNavigation.route(
                        f.session.accepted
                            .single()
                            .account,
                    ),
                )
                assertEquals("Nieuw", model.name.value)
                assertEquals("new@example.com", model.email.value)
                assertTrue(password.cleared && confirmation.cleared)
                assertEquals(1, f.source.inbox().size)
                val encoded = ApiJson.response.encodeToString(RegisterInput.serializer(), checkNotNull(payload))
                assertTrue(encoded.contains("\"language\":\"nl\""))
                assertTrue(encoded.contains("\"device_name\":\"Test phone\""))
                assertTrue(encoded.contains("\"password_confirmation\""))
                assertFalse(
                    encoded.contains("draft_token") || encoded.contains("input_revision") ||
                        encoded.contains("review_revision") ||
                        encoded.contains("guest_secret"),
                )
            }

        @Test fun serverAloneValidatesAllFieldsAndTakenEmailUsesTheBanner() =
            runTest(timeout = 60.seconds) {
                val f = fixture()
                submit(f.model, "", "")
                assertEquals(
                    "Please fill this in.",
                    f.model.form.state.value.fields["name"]
                        ?.key,
                )
                f.model.editName("Name")
                f.model.editEmail("bad")
                submit(f.model)
                assertNotNull(f.model.form.state.value.fields["email"])
                f.model.editEmail("demo@example.com")
                submit(f.model)
                assertEquals(AuthBannerKind.EmailTaken, f.model.form.state.value.banner)
            }

        @Test fun uiTestSeedReturnsTheExpectedRegistrationBannerOnline() = seedBanner(false)

        @Test fun uiTestSeedReturnsTheExpectedRegistrationBannerOffline() = seedBanner(true)

        private fun seedBanner(offline: Boolean) =
            runTest(timeout = 60.seconds) {
                val source =
                    MockDataSource(
                        seed =
                            MockAppSeed.make(
                                Instant.parse("2026-11-18T00:00:00Z"),
                            ),
                        offline = offline,
                        delay = {
                            0.0
                        },
                        sleep = {},
                        io = StandardTestDispatcher(testScheduler),
                    )
                val model =
                    SignInModel(
                        source,
                        SessionAcceptor {
                            error("accepted failure")
                        },
                        { AUTH_DEVICE_ID },
                        { "test" },
                        accepted = {
                            _,
                            _,
                            ->
                        },
                        onTwoFactor = {},
                    ).makeSignUpModel {}
                model.editName("Traveller")
                val email = if (offline) "new@example.com" else "demo@example.com"
                model.editEmail(email)
                submit(model)
                assertEquals(
                    if (offline) AuthBannerKind.Offline else AuthBannerKind.EmailTaken,
                    model.form.state.value.banner,
                )
                assertTrue(
                    model.form.state.value.fields
                        .isEmpty(),
                )
                assertEquals("Traveller", model.name.value)
                assertEquals(email, model.email.value)
            }

        @Test fun failuresPreserveNameEmailAndClearPasswordsIncludingKeychainFailureOffline() =
            dataFailure(DataFailure.Offline)

        @Test fun failuresPreserveNameEmailAndClearPasswordsIncludingKeychainFailureServer() =
            dataFailure(DataFailure.Server(500))

        @Test fun failuresPreserveNameEmailAndClearPasswordsIncludingKeychainFailureThrottle() =
            dataFailure(DataFailure.Throttled(60.0, 429))

        @Test fun failuresPreserveNameEmailAndClearPasswordsIncludingKeychainFailureConfirmation() =
            dataFailure(
                DataFailure.Validation(
                    mapOf("password_confirmation" to FieldMessage("The passwords don't match yet.")),
                    422,
                ),
            )

        private fun dataFailure(failure: DataFailure) =
            runTest(timeout = 60.seconds) {
                val f = fixture().filled()
                f.source.failNext(MockMethod.SignUp, failure)
                submit(f.model)
                assertEquals(failure, f.model.form.state.value.failure)
                assertEquals("Name", f.model.name.value)
                assertEquals("new@example.com", f.model.email.value)
                f.session.failure = SessionFailure.CredentialWrite
                submit(f.model)
                assertEquals(SessionFailure.CredentialWrite, f.model.sessionFailure)
                assertEquals(AuthBannerKind.Server, f.model.form.state.value.banner)
                assertNotNull(f.model.takeAnnouncement { it.key })
            }

        @Test fun createdAccountAfterSessionFailureCanRecoverThroughSignInCredentialsUnavailable() =
            recovery(SessionFailure.CredentialsUnavailable)

        @Test fun createdAccountAfterSessionFailureCanRecoverThroughSignInDatabaseUnavailable() =
            recovery(SessionFailure.DatabaseUnavailable)

        @Test fun createdAccountAfterSessionFailureCanRecoverThroughSignInCredentialWrite() =
            recovery(SessionFailure.CredentialWrite)

        @Test fun createdAccountAfterSessionFailureCanRecoverThroughSignInInvalidResult() =
            recovery(SessionFailure.InvalidResult)

        @Test fun createdAccountAfterSessionFailureCanRecoverThroughSignInSuperseded() =
            recovery(
                SessionFailure.Superseded,
            )

        private fun recovery(failure: SessionFailure) =
            runTest(timeout = 60.seconds) {
                val f = fixture().filled()
                f.session.failure = failure
                submit(f.model)
                assertEquals(failure, f.model.sessionFailure)
                assertEquals(AuthBannerKind.Server, f.model.form.state.value.banner)
                assertTrue(f.routes.accepted.isEmpty())
                submit(f.model)
                assertNull(f.model.sessionFailure)
                assertEquals(AuthBannerKind.EmailTaken, f.model.form.state.value.banner)
                assertEquals(1, f.source.inbox().size)
                f.model.backToSignIn()
                assertEquals(1, f.backs)
                f.session.failure = null
                f.signIn.editEmail(f.model.email.value)
                f.signIn.submitPassword(FakeSecretField("NewPassword123!")) {}
                assertEquals(1, f.session.accepted.size)
                assertEquals(
                    Route.Verify,
                    AuthNavigation.route(
                        f.session.accepted
                            .single()
                            .account,
                    ),
                )
            }

        @Test fun emailTakenPreservesOtherFieldErrorsInTheSameResponseSymbol() =
            mixed(
                "password",
                "Use at least one symbol.",
            )

        @Test fun emailTakenPreservesOtherFieldErrorsInTheSameResponseConfirmation() =
            mixed("password_confirmation", "The passwords don't match yet.")

        @Test fun emailTakenPreservesOtherFieldErrorsInTheSameResponseLeak() =
            mixed("password", "This password has appeared in a data leak. Choose a different password.")

        private fun mixed(
            field: String,
            key: String,
        ) = runTest(timeout = 60.seconds) {
            val f = fixture()
            val remaining = mapOf("name" to FieldMessage("Please fill this in."), field to FieldMessage(key))
            f.source.failNext(
                MockMethod.SignUp,
                DataFailure.Validation(
                    remaining + ("email" to FieldMessage("This email address is already in use.")),
                    422,
                ),
            )
            f.model.editEmail("demo@example.com")
            submit(f.model)
            assertEquals(AuthBannerKind.EmailTaken, f.model.form.state.value.banner)
            assertEquals(remaining, f.model.form.state.value.fields)
            assertEquals("demo@example.com", f.model.email.value)
        }

        @Test fun unchangedFieldCallbacksPreserveMixedFailureUntilAnEditName() = unchanged("name")

        @Test fun unchangedFieldCallbacksPreserveMixedFailureUntilAnEditEmail() = unchanged("email")

        private fun unchanged(field: String) =
            runTest(timeout = 60.seconds) {
                val model = fixture().model
                val fields =
                    mapOf("email" to FieldMessage("This email address is already in use.")) +
                        if (field == "name") mapOf("name" to FieldMessage("Please fill this in.")) else emptyMap()
                val failure = DataFailure.Validation(fields, 422)
                model.form.receive(failure)
                val edit = if (field == "name") model::editName else model::editEmail
                edit("")
                assertEquals(failure, model.form.state.value.failure)
                assertEquals(AuthBannerKind.EmailTaken, model.form.state.value.banner)
                edit("changed")
                assertNull(model.form.state.value.failure)
                assertNull(model.form.state.value.banner)
                assertNull(model.form.state.value.fields[field])
            }

        @Test fun emailTakenKeepsBannerWhenOtherMessagesNeedTheFallback() {
            val mapped =
                AuthFailureMapping(
                    DataFailure.Validation(
                        mapOf(
                            "email" to FieldMessage("This email address is already in use."),
                            "password" to FieldMessage("unknown English"),
                            "device_id" to FieldMessage("Please fill this in."),
                            "name" to FieldMessage("Please fill this in."),
                        ),
                        422,
                    ),
                    setOf("name", "email", "password", "password_confirmation"),
                    AuthFormContext.SignUp,
                )
            assertEquals(AuthBannerKind.EmailTaken, mapped.banner)
            assertEquals(mapOf("name" to FieldMessage("Please fill this in.")), mapped.fields)
        }

        @Test fun oneToggleControlsBothPasswordsAndBusyCancellationStopsAcceptance() =
            runTest(timeout = 60.seconds) {
                val entered = CompletableDeferred<Unit>()
                val gate = CompletableDeferred<String>()
                var identities = 0
                val f =
                    fixture(identity = {
                        identities++
                        entered.complete(Unit)
                        gate.await()
                    }).filled()
                f.model.togglePassword()
                val password = FakeSecretField("NewPassword123!")
                val confirmation = FakeSecretField("NewPassword123!")
                val job = launch { f.model.submit(password, confirmation, AuthLanguage.En) {} }
                entered.await()
                f.model.submit(password, confirmation, AuthLanguage.En) {}
                f.model.editName("ignored")
                f.model.editEmail("ignored")
                f.model.togglePassword()
                assertEquals("Name", f.model.name.value)
                assertTrue(f.model.showsPassword.value)
                f.model.cancel()
                job.cancel()
                gate.complete(AUTH_DEVICE_ID)
                job.join()
                assertEquals(1, identities)
                assertTrue(f.session.accepted.isEmpty())
                assertNull(f.model.form.state.value.banner)
                assertTrue(password.cleared && confirmation.cleared)
                assertFalse(f.model.showsPassword.value)
            }

        @Test fun committedSessionRoutesAfterCancel() =
            runTest(timeout = 60.seconds) {
                val entered = CompletableDeferred<Unit>()
                val finish = CompletableDeferred<Unit>()
                var routed = false
                val f = fixture()
                val signIn =
                    SignInModel(f.source, f.session, { AUTH_DEVICE_ID }, { "test" }, accepted = {
                        _,
                        _,
                        ->
                        entered.complete(Unit)
                        finish.await()
                        routed =
                            true
                    }, onTwoFactor = {})
                val model = signIn.makeSignUpModel {}
                model.editName("Name")
                model.editEmail("new@example.com")
                val password = FakeSecretField("NewPassword123!")
                val confirmation = FakeSecretField("NewPassword123!")
                val job = launch { model.submit(password, confirmation, AuthLanguage.En) {} }
                entered.await()
                job.cancel()
                finish.complete(Unit)
                job.join()
                assertTrue(routed)
                assertTrue(password.cleared && confirmation.cleared)
                assertFalse(model.form.busy)
            }
    }

    class Guest {
        @Test fun guestSignupRequiresALiveClaimBeforeCreatingAnAccount() =
            runTest(timeout = 60.seconds) {
                val f = fixture().filled()
                val password = FakeSecretField("NewPassword123!")
                val confirmation = FakeSecretField("NewPassword123!")
                val outcome = f.model.submit(password, confirmation, AuthLanguage.En, requiresGuestClaim = true) {}
                assertEquals(SignUpModel.Outcome.GuestReviewExpired, outcome)
                assertEquals(GuestSignupState.Review, f.model.guestState.value)
                assertTrue(password.cleared && confirmation.cleared)
                assertTrue(f.routes.accepted.isEmpty())
                assertTrue(f.source.inbox().isEmpty())
            }

        @Test fun guestSignupDistinguishesARequestFailureFromAnAccountAwaitingAcceptance() =
            runTest(timeout = 60.seconds) {
                val f = fixture().filled()
                assertEquals(GuestSignupState.Ready, f.model.guestState.value)
                f.source.failNext(MockMethod.SignUp, DataFailure.Server(503))
                submit(f.model)
                assertEquals(GuestSignupState.Failed, f.model.guestState.value)
                f.session.failure = SessionFailure.CredentialWrite
                submit(f.model)
                assertEquals(GuestSignupState.Retry, f.model.guestState.value)
                assertEquals(SessionFailure.CredentialWrite, f.model.sessionFailure)
            }

        @Test fun expiredGuestClaimShowsReviewAndClearsPasswords() =
            runTest(timeout = 60.seconds) {
                val f = fixture()
                var expired = 0
                val model =
                    SignUpModel(
                        f.source,
                        f.session,
                        { AUTH_DEVICE_ID },
                        { "test" },
                        f.routes::acceptedSession,
                        {},
                        claim = { GuestClaim("trip", 1, 1, "secret") },
                        guestReviewExpired = { expired++ },
                    )
                f.source.failNext(MockMethod.SignUp, DataFailure.ReviewExpired)
                val password = FakeSecretField("NewPassword123!")
                val confirmation = FakeSecretField("NewPassword123!")
                assertEquals(
                    SignUpModel.Outcome.GuestReviewExpired,
                    model.submit(password, confirmation, AuthLanguage.En, requiresGuestClaim = true) {},
                )
                assertEquals(GuestSignupState.Review, model.guestState.value)
                assertTrue(password.cleared && confirmation.cleared)
                assertEquals(1, expired)
                assertNull(model.form.state.value.banner)
            }

        @Test fun claimFieldsAreSentOnlyWithAClaim() =
            runTest(timeout = 60.seconds) {
                checkClaim(GuestClaim("token", 4, 7, "secret"))
                checkClaim(null)
            }

        private suspend fun TestScope.checkClaim(claim: GuestClaim?) {
            val f = fixture()
            var recorded: RegisterInput? = null
            val source =
                object : DataSource by f.source {
                    override suspend fun signUp(input: RegisterInput): SessionResult {
                        recorded = input
                        return f.source.signUp(
                            input.copy(
                                draftToken = null,
                                inputRevision = null,
                                reviewRevision = null,
                                guestSecret = null,
                            ),
                        )
                    }
                }
            val model =
                SignUpModel(
                    source,
                    f.session,
                    { AUTH_DEVICE_ID },
                    { "test" },
                    f.routes::acceptedSession,
                    {},
                    claim = { claim },
                )
            model.editName("Traveller")
            model.editEmail("new@example.com")
            submit(model)
            assertEquals(claim?.draftToken, checkNotNull(recorded).draftToken)
            assertEquals(claim?.inputRevision, checkNotNull(recorded).inputRevision)
            assertEquals(claim?.reviewRevision, checkNotNull(recorded).reviewRevision)
            assertEquals(claim?.guestSecret, checkNotNull(recorded).guestSecret)
        }

        @Test fun submitResetsTheGuestStateFirst() =
            runTest(timeout = 60.seconds) {
                val f = fixture()
                lateinit var model: SignUpModel
                var sent = false
                val source =
                    object : DataSource by f.source {
                        override suspend fun signUp(input: RegisterInput): SessionResult {
                            assertEquals(GuestSignupState.Ready, model.guestState.value)
                            sent = true
                            throw DataFailure.Offline
                        }
                    }
                model =
                    SignUpModel(
                        source,
                        f.session,
                        { AUTH_DEVICE_ID },
                        { "test" },
                        f.routes::acceptedSession,
                        {},
                        claim = { GuestClaim("token", 4, 7, "secret") },
                        initialGuestState = GuestSignupState.Review,
                    )
                submit(model)
                assertTrue(sent)
            }

        @Test fun fieldOnlyValidationKeepsReady() =
            runTest(timeout = 60.seconds) {
                val f = fixture()
                f.source.failNext(
                    MockMethod.SignUp,
                    DataFailure.Validation(mapOf("name" to FieldMessage("Please fill this in.")), 422),
                )
                val outcome = f.model.submit(FakeSecretField(), FakeSecretField(), AuthLanguage.En) {}
                assertNull(outcome)
                assertEquals(GuestSignupState.Ready, f.model.guestState.value)
                assertNull(f.model.form.state.value.banner)
                assertNotNull(f.model.form.state.value.fields["name"])
            }
    }

    @RunWith(Parameterized::class)
    class WeakPasswords(
        private val value: String,
    ) {
        @Test fun serverAloneValidatesAllFieldsAndTakenEmailUsesTheBanner() =
            runTest(timeout = 60.seconds) {
                val f = fixture().filled()
                submit(f.model, value, value)
                assertNotNull(f.model.form.state.value.fields["password"])
                assertTrue(f.session.accepted.isEmpty())
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun cases() = listOf("short", "lowercase123456!", "NoNumbersInThis!", "NoSymbols123456")
        }
    }

    @RunWith(Parameterized::class)
    class UnexpectedFailures(
        private val failure: Throwable,
    ) {
        @Test fun everySubmitFailureClearsSecretsAndShowsABanner() =
            runTest(timeout = 60.seconds) {
                val f = fixture(identity = { throw failure }).filled()
                submit(f.model)
                assertEquals(AuthBannerKind.Server, f.model.form.state.value.banner)
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun cases() =
                listOf(
                    DeviceIdentityFailure.InvalidPersistedIdentity,
                    StoreFailure.Closed,
                    IOException(),
                    GeneralSecurityException(),
                    IllegalStateException(),
                )
        }
    }
}

private class SignUpFixture(
    val source: MockDataSource,
    val session: FakeSessionController,
    val signIn: SignInModel,
) {
    val routes = RecordingRoutes()
    var backs = 0
    val model = signIn.makeSignUpModel { backs++ }

    fun filled() =
        apply {
            model.editName("Name")
            model.editEmail("new@example.com")
        }
}

private fun TestScope.fixture(
    deviceName: String = "test",
    identity: suspend () -> String = { AUTH_DEVICE_ID },
): SignUpFixture {
    val source = authTestSource(StandardTestDispatcher(testScheduler))
    val session = FakeSessionController()
    lateinit var f: SignUpFixture
    val signIn =
        SignInModel(source, session, identity, { deviceName }, accepted = {
            r,
            m,
            ->
            f.routes.acceptedSession(r, m)
        }, onTwoFactor = {})
    f = SignUpFixture(source, session, signIn)
    return f
}

private suspend fun submit(
    model: SignUpModel,
    value: String = "NewPassword123!",
    confirmed: String = value,
) {
    val password = FakeSecretField(value)
    val confirmation = FakeSecretField(confirmed)
    model.submit(password, confirmation, AuthLanguage.En) {}
    assertTrue(password.cleared && confirmation.cleared)
    assertEquals("", password.read())
    assertEquals("", confirmation.read())
    assertFalse(model.form.busy)
}
