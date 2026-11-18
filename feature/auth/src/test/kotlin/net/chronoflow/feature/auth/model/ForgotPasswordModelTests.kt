package net.chronoflow.feature.auth.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.datamock.MockMethod
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.feature.auth.testing.GatedDataSource
import net.chronoflow.feature.auth.testing.authTestSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class ForgotPasswordModelTests {
    class Behavior {
        @Test fun mockMailDeliveryUsesTheSubmittedRecipientEvenAfterLaterEdits() =
            runTest(timeout = 60.seconds) {
                val recipients = mutableListOf<String>()
                val model =
                    ForgotPasswordModel(
                        authTestSource(StandardTestDispatcher(testScheduler)),
                        AuthNavigation(),
                        {},
                        "unknown@example.com",
                        mail = {
                            recipients += it
                            false
                        },
                    )
                model.submit()
                model.editEmail("demo@example.com")
                model.openEmail { error("mock mail opened a real app") }
                assertEquals(listOf("unknown@example.com"), recipients)
                assertEquals("unknown@example.com", model.sentEmail.value)
            }

        @Test fun existingAndUnknownAddressesHaveIdenticalSuccessWithoutASession() =
            runTest(timeout = 60.seconds) {
                val source = authTestSource(StandardTestDispatcher(testScheduler))

                suspend fun checkAddress(email: String) {
                    val navigation = AuthNavigation().apply { forgot() }
                    val model = ForgotPasswordModel(source, navigation, {})
                    model.editEmail(email)
                    model.submit()
                    assertEquals(email, model.sentEmail.value)
                    assertNull(model.form.state.value.failure)
                    assertFalse(model.form.busy)
                    assertNull(navigation.challengeID)
                    model.editEmail("later@example.com")
                    model.submit()
                    assertEquals("later@example.com", model.email.value)
                    assertEquals(email, model.sentEmail.value)
                }
                checkAddress("demo@example.com")
                checkAddress("unknown@example.com")
                assertEquals(1, source.inbox().size)
                try {
                    source.fetchIdentity()
                    error("unexpected session")
                } catch (failure: DataFailure.Unauthorized) {
                    assertEquals(401, failure.status)
                }
            }

        @Test
        fun busyLockIgnoresEditsAndDuplicateTapOrReturnAndCapturesSubmittedEmail() {
            runTest(timeout = 60.seconds) {
                val gate =
                    GatedDataSource(
                        authTestSource(StandardTestDispatcher(testScheduler)),
                        MockMethod.RequestPasswordReset,
                    )
                val model = ForgotPasswordModel(gate, AuthNavigation(), {}, "unknown@example.com")
                val request = launch { model.submit() }
                gate.entered.await()
                assertTrue(model.form.busy)
                assertEquals(AuthFormModel.Action.SendResetLink, model.form.state.value.pending)
                model.editEmail("changed@example.com")
                model.submit()
                assertEquals("unknown@example.com", model.email.value)
                assertNull(model.sentEmail.value)
                gate.release()
                request.join()
                assertEquals(1, gate.entries)
                assertEquals("unknown@example.com", model.sentEmail.value)
                assertFalse(model.form.busy)
            }
        }

        @Test fun cancellationAndBackIgnoreLateCompletionCancel() = lateCompletion(false)

        @Test fun cancellationAndBackIgnoreLateCompletionBack() = lateCompletion(true)

        private fun lateCompletion(back: Boolean) =
            runTest(timeout = 60.seconds) {
                val gate =
                    GatedDataSource(
                        authTestSource(StandardTestDispatcher(testScheduler)),
                        MockMethod.RequestPasswordReset,
                    )
                val navigation = AuthNavigation().apply { forgot("expired@example.com") }
                var backs = 0
                val model = ForgotPasswordModel(gate, navigation, { backs++ }, navigation.forgotEmail)
                val request = launch { model.submit() }
                gate.entered.await()
                if (back) model.backToSignIn() else request.cancel()
                gate.release()
                request.join()
                assertNull(model.sentEmail.value)
                assertNull(model.form.state.value.failure)
                assertFalse(model.form.busy)
                assertEquals(if (back) 1 else 0, backs)
                if (back) assertEquals("", navigation.forgotEmail)
            }

        @Test fun emailAppMissingHandlerPreservesTheSentNoticeAndBackReturnsToSignIn() =
            runTest(timeout = 60.seconds) {
                var opened = 0
                var backs = 0
                val model =
                    ForgotPasswordModel(
                        authTestSource(StandardTestDispatcher(testScheduler)),
                        AuthNavigation(),
                        { backs++ },
                        "unknown@example.com",
                    )
                val open = {
                    opened++
                    false
                }
                model.openEmail(open)
                assertEquals(0, opened)
                model.submit()
                model.openEmail(open)
                assertEquals(1, opened)
                assertEquals("unknown@example.com", model.sentEmail.value)
                assertNull(model.form.state.value.failure)
                model.backToSignIn()
                model.openEmail(open)
                assertEquals(1, opened)
                assertEquals(1, backs)
            }

        @Test fun expiredResetCanPrefillANewFormWithoutReusingSentState() =
            runTest(timeout = 60.seconds) {
                val source = authTestSource(StandardTestDispatcher(testScheduler))
                val navigation = AuthNavigation().apply { forgot("expired+tag@example.com") }
                val model = ForgotPasswordModel(source, navigation, {}, navigation.forgotEmail)
                assertEquals("expired+tag@example.com", model.email.value)
                assertNull(model.sentEmail.value)
                model.submit()
                model.backToSignIn()
                navigation.forgot()
                val fresh = ForgotPasswordModel(source, navigation, {}, navigation.forgotEmail)
                assertEquals("", fresh.email.value)
                assertNull(fresh.sentEmail.value)
                assertNull(fresh.form.state.value.failure)
            }
    }

    @RunWith(Parameterized::class)
    class Failures(
        private val name: String,
        private val failure: DataFailure,
        private val banner: AuthBannerKind?,
    ) {
        @Test fun fieldServerThrottleOfflineAndHiddenFailuresKeepTheFormAndEmail() =
            runTest(timeout = 60.seconds) {
                val source = authTestSource(StandardTestDispatcher(testScheduler))
                val model = ForgotPasswordModel(source, AuthNavigation(), {}, "retained@example.com")
                source.failNext(MockMethod.RequestPasswordReset, failure)
                model.submit()
                assertEquals(failure, model.form.state.value.failure)
                assertEquals(banner, model.form.state.value.banner)
                assertEquals("retained@example.com", model.email.value)
                assertNull(model.sentEmail.value)
                assertFalse(model.form.busy)
                val state = model.form.state.value
                model.editEmail(model.email.value)
                assertEquals(state, model.form.state.value)
                if (banner == AuthBannerKind.Locked) {
                    assertEquals(73.0, model.form.state.value.retryAfterSeconds!!, 0.0)
                    assertEquals(
                        "Too many requests. Wait a few minutes before you ask for another link.",
                        model.form.takeAnnouncement { it.key },
                    )
                    assertNull(model.form.takeAnnouncement { it.key })
                }
                model.editEmail("edited@example.com")
                assertNull(model.form.state.value.failure)
                assertNull(model.form.state.value.banner)
                assertTrue(
                    model.form.state.value.fields
                        .isEmpty(),
                )
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun cases(): List<Array<Any?>> =
                listOf(
                    arrayOf(
                        "email",
                        DataFailure.Validation(mapOf("email" to FieldMessage("Please fill this in.")), 422),
                        null,
                    ),
                    arrayOf("server", DataFailure.Server(503), AuthBannerKind.Server),
                    arrayOf("throttle", DataFailure.Throttled(73.0, 429), AuthBannerKind.Locked),
                    arrayOf("offline", DataFailure.Offline, AuthBannerKind.Offline),
                    arrayOf(
                        "hidden",
                        DataFailure.Validation(mapOf("hidden" to FieldMessage("Please fill this in.")), 422),
                        AuthBannerKind.Server,
                    ),
                    arrayOf(
                        "unknown",
                        DataFailure.Validation(mapOf("email" to FieldMessage("Untranslated server text")), 422),
                        AuthBannerKind.Server,
                    ),
                )
        }
    }

    @RunWith(Parameterized::class)
    class EmailValidation(
        private val name: String,
        private val email: String,
        private val key: String,
    ) {
        @Test fun serverValidatesBlankAndMalformedEmailWithoutClientValidation() =
            runTest(timeout = 60.seconds) {
                val model =
                    ForgotPasswordModel(
                        authTestSource(StandardTestDispatcher(testScheduler)),
                        AuthNavigation(),
                        {},
                        email,
                    )
                model.submit()
                assertEquals(
                    key,
                    model.form.state.value.fields["email"]
                        ?.key,
                )
                assertNull(model.form.state.value.banner)
                assertNull(model.sentEmail.value)
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun cases() =
                listOf(
                    arrayOf("empty", "", "Please fill this in."),
                    arrayOf("whitespace", "   ", "Please fill this in."),
                    arrayOf("malformed", "invalid", "Enter a valid email address, like name@example.com."),
                )
        }
    }

    @RunWith(Parameterized::class)
    class UnexpectedFailures(
        private val failure: Throwable,
    ) {
        @Test fun checkedAndRuntimeFailuresShowTheServerBanner() =
            runTest(timeout = 60.seconds) {
                val base = authTestSource(StandardTestDispatcher(testScheduler))
                val source =
                    object : DataSource by base {
                        override suspend fun requestPasswordReset(email: String): Unit = throw failure
                    }
                val model = ForgotPasswordModel(source, AuthNavigation(), {})
                model.submit()
                assertEquals(AuthBannerKind.Server, model.form.state.value.banner)
                assertFalse(model.form.busy)
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
}
