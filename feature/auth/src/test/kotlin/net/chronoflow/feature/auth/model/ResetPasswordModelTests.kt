package net.chronoflow.feature.auth.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.datamock.MockMail
import net.chronoflow.core.datamock.MockMethod
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.api.PasswordSignInInput
import net.chronoflow.core.model.api.ResetPasswordInput
import net.chronoflow.core.model.api.SignInResult
import net.chronoflow.core.model.auth.ResetPasswordLink
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import net.chronoflow.feature.auth.testing.FakeSecretField
import net.chronoflow.feature.auth.testing.GatedDataSource
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
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Enclosed::class)
class ResetPasswordModelTests {
    class Behavior {
        @Test fun mockMailResetClearsSecretsRevokesSessionsAndCannotBeReused() =
            runTest(timeout = 60.seconds) {
                val source = authTestSource(StandardTestDispatcher(testScheduler))
                source.signIn(PasswordSignInInput("demo@example.com", "Password123!", AUTH_DEVICE_ID, "test"))
                source.requestPasswordReset("demo@example.com")
                val input = (source.inbox().last() as MockMail.PasswordReset).input
                val link = ResetPasswordLink(input.token, input.email)
                val navigation = AuthNavigation()
                var backs = 0
                val model = ResetPasswordModel(link, source, navigation, { backs++ })
                submitAndCheckCleared(model)
                assertEquals(ResetPasswordModel.State.Done, model.state.value)
                model.backToSignIn()
                assertEquals(1, backs)
                val reused = ResetPasswordModel(link, source, navigation, {})
                submitAndCheckCleared(reused)
                assertEquals(ResetPasswordModel.State.Expired, reused.state.value)
                try {
                    source.fetchIdentity()
                    error("reset retained an old session")
                } catch (failure: DataFailure.Unauthorized) {
                    assertEquals(401, failure.status)
                }
            }

        @Test fun missingInvalidAndServerFieldMessagesUseDesignedStates() =
            runTest(timeout = 60.seconds) {
                val source = authTestSource(StandardTestDispatcher(testScheduler))
                val missing = ResetPasswordModel(ResetPasswordLink("", ""), source, AuthNavigation(), {})
                assertEquals(ResetPasswordModel.State.Expired, missing.state.value)
                val model =
                    ResetPasswordModel(
                        ResetPasswordLink("bad", "demo@example.com"),
                        source,
                        AuthNavigation(),
                        {},
                    )
                submitAndCheckCleared(model, "short", "short")
                assertEquals(ResetPasswordModel.State.Form, model.state.value)
                assertNotNull(model.form.state.value.fields["password"])
                submitAndCheckCleared(model, "NewPassword123!", "different")
                assertNotNull(model.form.state.value.fields["password_confirmation"])

                suspend fun failure(failure: DataFailure) {
                    source.failNext(MockMethod.ResetPassword, failure)
                    submitAndCheckCleared(model)
                    assertEquals(ResetPasswordModel.State.Form, model.state.value)
                    assertEquals(failure, model.form.state.value.failure)
                }
                failure(DataFailure.Offline)
                failure(DataFailure.Server(500))
                failure(DataFailure.Throttled(60.0, 429))
                submitAndCheckCleared(model)
                assertEquals(ResetPasswordModel.State.Expired, model.state.value)
                assertEquals("demo@example.com", model.recovery.email.value)
                model.recovery.editEmail("replacement@example.com")
                model.recovery.submit()
                assertEquals("replacement@example.com", model.recovery.sentEmail.value)
            }

        @Test fun cancelAndDuplicateSubmissionIgnoreLateCompletion() =
            runTest(timeout = 60.seconds) {
                val gate =
                    GatedDataSource(
                        authTestSource(StandardTestDispatcher(testScheduler)),
                        MockMethod.ResetPassword,
                    )
                val model =
                    ResetPasswordModel(
                        ResetPasswordLink("test", "demo@example.com"),
                        gate,
                        AuthNavigation(),
                        {},
                    )
                val password = FakeSecretField("NewPassword123!")
                val confirmation = FakeSecretField("NewPassword123!")
                val task = launch { model.submit(password, confirmation) }
                gate.entered.await()
                model.submit(password, confirmation)
                model.editPassword()
                assertEquals("NewPassword123!", password.read())
                model.cancel()
                task.cancel()
                gate.release()
                task.join()
                assertEquals(1, gate.entries)
                assertTrue(password.cleared && confirmation.cleared)
                assertNull(model.form.state.value.failure)
                assertFalse(model.form.busy)
                assertEquals(ResetPasswordModel.State.Form, model.state.value)
            }

        @Test fun resetNeverTouchesTheSession() =
            runTest(timeout = 60.seconds) {
                var sessionCalls = 0
                val source =
                    object : DataSource by authTestSource(StandardTestDispatcher(testScheduler)) {
                        override suspend fun resetPassword(input: ResetPasswordInput) {}

                        override suspend fun signIn(input: PasswordSignInInput): SignInResult {
                            sessionCalls++
                            error("unexpected sign-in")
                        }
                    }
                val model =
                    ResetPasswordModel(
                        ResetPasswordLink("valid", "demo@example.com"),
                        source,
                        AuthNavigation(),
                        {},
                    )
                submitAndCheckCleared(model)
                assertEquals(ResetPasswordModel.State.Done, model.state.value)
                val broken =
                    object : DataSource by source {
                        override suspend fun resetPassword(input: ResetPasswordInput): Unit = throw DataFailure.Offline
                    }
                val failed = ResetPasswordModel(model.link, broken, AuthNavigation(), {})
                submitAndCheckCleared(failed)
                assertEquals(AuthBannerKind.Offline, failed.form.state.value.banner)
                assertEquals(0, sessionCalls)
            }

        @Test fun prepareResetDropsTheChallenge() =
            runTest(timeout = 60.seconds) {
                val signIn = challenged(twoFactorSource(StandardTestDispatcher(testScheduler)))
                val old = checkNotNull(signIn.twoFactor.value)
                old.editCode("123")
                signIn.prepareReset()
                assertNull(signIn.navigation.challengeID)
                assertNull(signIn.twoFactor.value)
                old.editCode("456")
                assertEquals("", old.code)
            }
    }

    @RunWith(Parameterized::class)
    class UnexpectedFailures(
        private val failure: Throwable,
    ) {
        @Test fun everyFailureClearsBothSecretsAndShowsTheServerBanner() =
            runTest(timeout = 60.seconds) {
                val source =
                    object : DataSource by authTestSource(StandardTestDispatcher(testScheduler)) {
                        override suspend fun resetPassword(input: ResetPasswordInput): Unit = throw failure
                    }
                val model =
                    ResetPasswordModel(
                        ResetPasswordLink("token", "demo@example.com"),
                        source,
                        AuthNavigation(),
                        {},
                    )
                submitAndCheckCleared(model)
                assertEquals(AuthBannerKind.Server, model.form.state.value.banner)
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun cases() =
                listOf(
                    SessionFailure.CredentialsUnavailable,
                    DeviceIdentityFailure.InvalidPersistedIdentity,
                    StoreFailure.Closed,
                    IOException(),
                    GeneralSecurityException(),
                    IllegalStateException(),
                )
        }
    }

    @RunWith(Parameterized::class)
    class Validation(
        private val name: String,
        private val field: String,
        private val key: String,
        private val expired: Boolean,
    ) {
        @Test fun onlyTheInvalidTokenKeySelectsExpired() =
            runTest(timeout = 60.seconds) {
                val base = authTestSource(StandardTestDispatcher(testScheduler))
                base.failNext(MockMethod.ResetPassword, DataFailure.Validation(mapOf(field to FieldMessage(key)), 422))
                val model =
                    ResetPasswordModel(
                        ResetPasswordLink("token", "demo@example.com"),
                        base,
                        AuthNavigation(),
                        {},
                    )
                submitAndCheckCleared(model)
                val expected = if (expired) ResetPasswordModel.State.Expired else ResetPasswordModel.State.Form
                assertEquals(expected, model.state.value)
                if (!expired) assertEquals(AuthBannerKind.Server, model.form.state.value.banner)
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun cases(): List<Array<Any>> =
                listOf(
                    arrayOf("token", "token", "This password reset link is invalid or has expired.", true),
                    arrayOf(
                        "invalid-key-on-hidden-field",
                        "other",
                        "This password reset link is invalid or has expired.",
                        true,
                    ),
                    arrayOf("other-hidden", "token", "Please fill this in.", false),
                    arrayOf("untranslated", "password", "Untranslated server text", false),
                )
        }
    }
}

private suspend fun submitAndCheckCleared(
    model: ResetPasswordModel,
    value: String = "NewPassword123!",
    confirmationValue: String = value,
) {
    val password = FakeSecretField(value)
    val confirmation = FakeSecretField(confirmationValue)
    model.submit(password, confirmation)
    assertTrue(password.cleared && confirmation.cleared)
    assertEquals("", password.read())
    assertEquals("", confirmation.read())
    assertFalse(model.form.busy)
}
