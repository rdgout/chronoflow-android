package net.chronoflow.feature.auth.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.datamock.MockMethod
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.auth.AuthenticationMethod
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.navigation.Route
import net.chronoflow.feature.auth.testing.FakeSessionController
import net.chronoflow.feature.auth.testing.RecordingRoutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(
    ExperimentalCoroutinesApi::class,
)
class TwoFactorModelTests {
    private suspend fun emptyProof(
        recovery: Boolean,
        io: kotlinx.coroutines.CoroutineDispatcher,
    ) {
        val model =
            challenged(
                twoFactorSource(
                    io,
                ),
            ).twoFactor.value!!
        if (
            recovery
        ) {
            model.toggleMode()
        }
        model.submit()
        val messages =
            mapOf(
                "code" to
                    FieldMessage(
                        "Enter the 6-digit code from your authenticator app.",
                    ),
                "recovery_code" to
                    FieldMessage(
                        "Enter one of the recovery codes you saved when you set up two-factor authentication.",
                    ),
            )
        assertEquals(
            DataFailure.Validation(
                messages,
                422,
            ),
            model.form.state.value.failure,
        )
        assertEquals(
            mapOf(
                model.field to
                    messages.getValue(
                        model.field,
                    ),
            ),
            model.form.state.value.fields,
        )
        assertNull(
            model.form.state.value.banner,
        )
        assertEquals(
            "",
            model.code,
        )
        assertFalse(
            model.form.busy,
        )
        assertEquals(
            messages
                .getValue(
                    model.field,
                ).key,
            model.form.takeAnnouncement { it.key },
        )
        assertNull(
            model.form.takeAnnouncement { it.key },
        )
    }

    @Test fun emptyProofShowsOnlyTheActiveFieldErrorCode() =
        runTest(
            timeout = 60.seconds,
        ) {
            emptyProof(
                false,
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    @Test fun emptyProofShowsOnlyTheActiveFieldErrorRecovery() =
        runTest(
            timeout = 60.seconds,
        ) {
            emptyProof(
                true,
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    private suspend fun latestCode(
        verified: Boolean,
        io: kotlinx.coroutines.CoroutineDispatcher,
    ) {
        val session =
            FakeSessionController()
        val routes =
            RecordingRoutes()
        val signIn =
            challenged(
                twoFactorSource(
                    io,
                    verified,
                    "654321",
                ),
                session,
                routes,
            )
        val model = signIn.twoFactor.value!!
        assertTrue(
            session.accepted.isEmpty(),
        )
        assertTrue(
            routes.accepted.isEmpty(),
        )
        model.submit()
        assertEquals(
            "Enter the 6-digit code from your authenticator app.",
            model.form.state.value.fields["code"]
                ?.key,
        )
        model.editCode(
            "123456",
        )
        model.submit()
        assertEquals(
            REJECTED,
            model.form.state.value.fields["code"]
                ?.key,
        )
        assertEquals(
            "",
            model.code,
        )
        assertNull(
            model.form.state.value.banner,
        )
        assertFalse(
            model.form.busy,
        )
        model.editCode(
            "654321",
        )
        model.submit()
        model.submit()
        assertEquals(
            1,
            session.accepted.size,
        )
        assertEquals(
            1,
            routes.accepted.size,
        )
        assertEquals(
            AuthenticationMethod.TwoFactor,
            routes.accepted.single().second,
        )
        assertEquals(
            if (
                verified
            ) {
                Route.Today
            } else {
                Route.Verify
            },
            AuthNavigation.route(
                routes.accepted
                    .single()
                    .first.account,
            ),
        )
        assertEquals(
            "",
            model.code,
        )
        assertNull(
            signIn.navigation.challengeID,
        )
    }

    @Test fun latestCodeAcceptsThroughPasswordSessionPathVerified() =
        runTest(
            timeout = 60.seconds,
        ) {
            latestCode(
                true,
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    @Test fun latestCodeAcceptsThroughPasswordSessionPathUnverified() =
        runTest(
            timeout = 60.seconds,
        ) {
            latestCode(
                false,
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    @Test fun numericInputFiltersPastesAndAutoFillWithoutSubmitting() =
        runTest(
            timeout = 60.seconds,
        ) {
            val session =
                FakeSessionController()
            val model =
                challenged(
                    twoFactorSource(
                        StandardTestDispatcher(
                            testScheduler,
                        ),
                    ),
                    session,
                ).twoFactor.value!!
            model.editCode(
                "a12 ٣b345678",
            )
            assertEquals(
                "123456",
                model.code,
            )
            assertTrue(
                session.accepted.isEmpty(),
            )
            assertFalse(
                model.form.busy,
            )
            model.toggleMode()
            model.editCode(
                RECOVERY_PROOF,
            )
            assertEquals(
                RECOVERY_PROOF,
                model.code,
            )
        }

    @Test fun recoveryIsSingleUseAndModeToggleClearsFeedback() =
        runTest(
            timeout = 60.seconds,
        ) {
            val source =
                twoFactorSource(
                    StandardTestDispatcher(
                        testScheduler,
                    ),
                )
            val session =
                FakeSessionController()
            val model =
                challenged(
                    source,
                    session,
                ).twoFactor.value!!
            model.submit()
            model.toggleMode()
            assertEquals(
                TwoFactorModel.Mode.Recovery,
                model.mode,
            )
            assertEquals(
                "",
                model.code,
            )
            assertTrue(
                model.form.state.value.fields
                    .isEmpty(),
            )
            model.submit()
            assertEquals(
                "Enter one of the recovery codes you saved when you set up two-factor authentication.",
                model.form.state.value.fields[model.field]
                    ?.key,
            )
            model.editCode(
                "wrong",
            )
            model.submit()
            assertEquals(
                "",
                model.code,
            )
            assertNotNull(
                model.form.state.value.fields[model.field],
            )
            assertNull(
                model.form.state.value.banner,
            )
            model.editCode(
                RECOVERY_PROOF,
            )
            model.submit()
            assertEquals(
                1,
                session.accepted.size,
            )
            val next =
                challenged(
                    source,
                    session,
                ).twoFactor.value!!
            next.toggleMode()
            next.editCode(
                RECOVERY_PROOF,
            )
            next.submit()
            assertEquals(
                "This recovery code was not accepted. Try another saved code.",
                next.form.state.value.fields[next.field]
                    ?.key,
            )
            assertEquals(
                "",
                next.code,
            )
            assertNull(
                next.form.state.value.banner,
            )
            next.toggleMode()
            assertEquals(
                TwoFactorModel.Mode.Code,
                next.mode,
            )
            assertTrue(
                next.form.state.value.fields
                    .isEmpty(),
            )
        }

    private suspend fun expiry(
        expired: Boolean,
        io: kotlinx.coroutines.CoroutineDispatcher,
    ) {
        val clock =
            TestClock()
        val session =
            FakeSessionController()
        val model =
            challenged(
                twoFactorSource(
                    io,
                    now = clock::now,
                ),
                session,
            ).twoFactor.value!!
        if (
            expired
        ) {
            clock.advance(
                300,
            )
        } else {
            suspend fun wrong() {
                model.editCode(
                    "000000",
                )
                model.submit()
            }
            wrong()
            wrong()
            wrong()
            wrong()
            wrong()
            clock.advance(
                60,
            )
        }
        model.editCode(
            "123456",
        )
        model.submit()
        assertEquals(
            REJECTED,
            model.form.state.value.fields["code"]
                ?.key,
        )
        assertNull(
            model.form.state.value.banner,
        )
        assertEquals(
            "",
            model.code,
        )
        assertFalse(
            model.form.busy,
        )
        assertTrue(
            session.accepted.isEmpty(),
        )
    }

    @Test fun expiryAndFiveFailuresKeepTheReturnedFieldStateExpired() =
        runTest(
            timeout = 60.seconds,
        ) {
            expiry(
                true,
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    @Test fun expiryAndFiveFailuresKeepTheReturnedFieldStateExhausted() =
        runTest(
            timeout = 60.seconds,
        ) {
            expiry(
                false,
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    private suspend fun failure(
        failure: DataFailure,
        banner: AuthBannerKind,
        io: kotlinx.coroutines.CoroutineDispatcher,
    ) {
        val source =
            twoFactorSource(
                io,
            )
        val model =
            challenged(
                source,
            ).twoFactor.value!!
        model.editCode(
            "123456",
        )
        source.failNext(
            MockMethod.CompleteTwoFactor,
            failure,
        )
        model.submit()
        assertEquals(
            failure,
            model.form.state.value.failure,
        )
        assertEquals(
            "",
            model.code,
        )
        assertFalse(
            model.form.busy,
        )
        assertEquals(
            banner,
            model.form.state.value.banner,
        )
        if (
            banner == AuthBannerKind.Locked
        ) {
            assertEquals(
                73.0,
                model.form.state.value.retryAfterSeconds,
            )
        }
        model.editCode(
            "1",
        )
        assertNull(
            model.form.state.value.banner,
        )
    }

    @Test fun throttleServerOfflineAndHiddenFieldsUseKeptDefaultsThrottle() =
        runTest(
            timeout = 60.seconds,
        ) {
            failure(
                DataFailure.Throttled(
                    73.0,
                    429,
                ),
                AuthBannerKind.Locked,
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    @Test fun throttleServerOfflineAndHiddenFieldsUseKeptDefaultsServer() =
        runTest(
            timeout = 60.seconds,
        ) {
            failure(
                DataFailure.Server(
                    503,
                ),
                AuthBannerKind.Server,
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    @Test fun throttleServerOfflineAndHiddenFieldsUseKeptDefaultsOffline() =
        runTest(
            timeout = 60.seconds,
        ) {
            failure(
                DataFailure.Offline,
                AuthBannerKind.Offline,
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    @Test fun throttleServerOfflineAndHiddenFieldsUseKeptDefaultsChallenge() =
        runTest(
            timeout = 60.seconds,
        ) {
            failure(
                DataFailure.Validation(
                    mapOf(
                        "challengeId" to
                            FieldMessage(
                                "Please fill this in.",
                            ),
                    ),
                    422,
                ),
                AuthBannerKind.Server,
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    @Test fun throttleServerOfflineAndHiddenFieldsUseKeptDefaultsHiddenProof() =
        runTest(
            timeout = 60.seconds,
        ) {
            failure(
                DataFailure.Validation(
                    mapOf(
                        "recovery_code" to
                            FieldMessage(
                                "Please fill this in.",
                            ),
                    ),
                    422,
                ),
                AuthBannerKind.Server,
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    @Test fun doubleSubmitBusyEditsToggleAndBackDiscardLateSuccess() =
        runTest(
            timeout = 60.seconds,
        ) {
            val gate =
                TestGate()
            val session =
                FakeSessionController()
            val routes =
                RecordingRoutes()
            val signIn =
                challenged(
                    twoFactorSource(
                        StandardTestDispatcher(
                            testScheduler,
                        ),
                        sleep = {
                            gate.wait()
                        },
                    ),
                    session,
                    routes,
                )
            val model = signIn.twoFactor.value!!
            model.editCode(
                "123456",
            )
            gate.armed = true
            val attempt =
                launch {
                    model.submit()
                }
            gate.entered.await()
            model.submit()
            model.editCode(
                "654321",
            )
            model.toggleMode()
            assertEquals(
                "123456",
                model.code,
            )
            assertEquals(
                TwoFactorModel.Mode.Code,
                model.mode,
            )
            assertTrue(
                model.form.busy,
            )
            model.cancel()
            assertEquals(
                "",
                model.code,
            )
            assertNull(
                signIn.navigation.challengeID,
            )
            assertEquals(
                1,
                routes.backToSignIn,
            )
            gate.released.complete(
                Unit,
            )
            attempt.join()
            assertTrue(
                session.accepted.isEmpty(),
            )
            assertFalse(
                model.form.busy,
            )
            assertNull(
                model.form.state.value.failure,
            )
            signIn.navigation.twoFactor(
                "new-challenge",
            )
            model.cancel()
            assertEquals(
                "new-challenge",
                signIn.navigation.challengeID,
            )
            assertEquals(
                1,
                routes.backToSignIn,
            )
        }

    @Test fun taskCancellationClearsCodeWithoutPresentationOrAcceptance() =
        runTest(
            timeout = 60.seconds,
        ) {
            val gate =
                TestGate()
            val session =
                FakeSessionController()
            val source =
                twoFactorSource(
                    StandardTestDispatcher(
                        testScheduler,
                    ),
                    sleep = {
                        gate.wait()
                    },
                )
            val model =
                challenged(
                    source,
                    session,
                ).twoFactor.value!!
            model.editCode(
                "123456",
            )
            gate.armed = true
            val attempt =
                launch {
                    model.submit()
                }
            gate.entered.await()
            attempt.cancel()
            gate.released.complete(
                Unit,
            )
            attempt.join()
            assertEquals(
                "",
                model.code,
            )
            assertNull(
                model.form.state.value.failure,
            )
            assertFalse(
                model.form.busy,
            )
            assertTrue(
                session.accepted.isEmpty(),
            )
        }

    @Test fun missingChallengeReturnsToSignInAndNewNavigationCannotRestoreSecrets() =
        runTest(
            timeout = 60.seconds,
        ) {
            val source =
                twoFactorSource(
                    StandardTestDispatcher(
                        testScheduler,
                    ),
                )
            val session =
                FakeSessionController()
            val routes =
                RecordingRoutes()
            val signIn =
                challenged(
                    source,
                    session,
                    routes,
                )
            val model = signIn.twoFactor.value!!
            model.editCode(
                "123456",
            )
            signIn.navigation.discardChallenge()
            model.submit()
            assertEquals(
                1,
                routes.backToSignIn,
            )
            assertEquals(
                "",
                model.code,
            )
            val fresh =
                AuthNavigation()
            var freshLeaves = 0
            val missing =
                TwoFactorModel(
                    source,
                    fresh,
                    {
                        fail(
                            "Missing challenge must not accept",
                        )
                    },
                    { freshLeaves++ },
                )
            missing.validateChallenge()
            assertEquals(
                1,
                freshLeaves,
            )
            assertNull(
                fresh.challengeID,
            )
            assertEquals(
                "",
                missing.code,
            )
            assertFalse(
                fresh.twoFactor(
                    " \n",
                ),
            )
            assertNull(
                fresh.challengeID,
            )
            assertTrue(
                session.accepted.isEmpty(),
            )
        }

    private suspend fun sessionFailure(
        failure: SessionFailure,
        io: kotlinx.coroutines.CoroutineDispatcher,
    ) {
        val session =
            FakeSessionController().also { it.failure = failure }
        val routes =
            RecordingRoutes()
        val model =
            challenged(
                twoFactorSource(
                    io,
                ),
                session,
                routes,
            ).twoFactor.value!!
        model.editCode(
            "123456",
        )
        model.submit()
        assertEquals(
            failure,
            model.sessionFailure,
        )
        assertEquals(
            AuthBannerKind.Server,
            model.form.state.value.banner,
        )
        assertEquals(
            "",
            model.code,
        )
        assertNotNull(
            model.form.takeAnnouncement { it.key },
        )
        assertTrue(
            session.accepted.isEmpty(),
        )
        assertTrue(
            routes.accepted.isEmpty(),
        )
    }

    @Test fun sessionStorageFailureRemainsTypedAndShowsTheServerBannerCredentialsUnavailable() =
        runTest(
            timeout = 60.seconds,
        ) {
            sessionFailure(
                SessionFailure.CredentialsUnavailable,
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    @Test fun sessionStorageFailureRemainsTypedAndShowsTheServerBannerDatabaseUnavailable() =
        runTest(
            timeout = 60.seconds,
        ) {
            sessionFailure(
                SessionFailure.DatabaseUnavailable,
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    @Test fun sessionStorageFailureRemainsTypedAndShowsTheServerBannerCredentialWrite() =
        runTest(
            timeout = 60.seconds,
        ) {
            sessionFailure(
                SessionFailure.CredentialWrite,
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    @Test fun sessionStorageFailureRemainsTypedAndShowsTheServerBannerInvalidResult() =
        runTest(
            timeout = 60.seconds,
        ) {
            sessionFailure(
                SessionFailure.InvalidResult,
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    @Test fun sessionStorageFailureRemainsTypedAndShowsTheServerBannerSuperseded() =
        runTest(
            timeout = 60.seconds,
        ) {
            sessionFailure(
                SessionFailure.Superseded,
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    private suspend fun backend(
        recovery: Boolean,
        key: String,
        displayed: String,
        io: kotlinx.coroutines.CoroutineDispatcher,
    ) {
        val model =
            challenged(
                twoFactorSource(
                    io,
                ),
            ).twoFactor.value!!
        if (
            recovery
        ) {
            model.toggleMode()
        }
        val failure =
            DataFailure.Validation(
                mapOf(
                    model.field to
                        FieldMessage(
                            key,
                        ),
                ),
                422,
            )
        model.form.receive(
            failure,
        )
        assertEquals(
            failure,
            model.form.state.value.failure,
        )
        assertNull(
            model.form.state.value.banner,
        )
        assertEquals(
            displayed,
            model.form.state.value.fields[model.field]
                ?.key,
        )

        fun announce() {
            model.form.receive(
                DataFailure.Throttled(
                    40.0,
                    429,
                ),
            )
            assertNotNull(
                model.form.takeAnnouncement { it.key },
            )
            assertNull(
                model.form.takeAnnouncement { it.key },
            )
            model.toggleMode()
            assertNull(
                model.form.takeAnnouncement { it.key },
            )
            assertTrue(
                model.form.state.value.fields
                    .isEmpty(),
            )
        }
        announce()
        announce()
    }

    @Test fun backendMessagesLocalizeOnlyUnderTheDisplayedModeAndAnnouncementsAreConsumedOnceCodeMissing() =
        runTest(
            timeout = 60.seconds,
        ) {
            backend(
                false,
                "Enter the 6-digit code from your authenticator app.",
                "Enter the 6-digit code from your authenticator app.",
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    @Test fun backendMessagesLocalizeOnlyUnderTheDisplayedModeAndAnnouncementsAreConsumedOnceCodeRejected() =
        runTest(
            timeout = 60.seconds,
        ) {
            backend(
                false,
                "The authentication code was not accepted. Try again.",
                "That code didn't work. Codes change every 30 seconds, so try the newest one.",
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    @Test fun backendMessagesLocalizeOnlyUnderTheDisplayedModeAndAnnouncementsAreConsumedOnceRecoveryMissing() =
        runTest(
            timeout = 60.seconds,
        ) {
            backend(
                true,
                "Enter one of the recovery codes you saved when you set up two-factor authentication.",
                "Enter one of the recovery codes you saved when you set up two-factor authentication.",
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    @Test fun backendMessagesLocalizeOnlyUnderTheDisplayedModeAndAnnouncementsAreConsumedOnceRecoveryRejected() =
        runTest(
            timeout = 60.seconds,
        ) {
            backend(
                true,
                "This recovery code was not accepted. Try another saved code.",
                "This recovery code was not accepted. Try another saved code.",
                StandardTestDispatcher(
                    testScheduler,
                ),
            )
        }

    @Test fun unchangedProofDoesNotClearFeedbackAndStateDescriptionRedactsTheProof() =
        runTest(
            timeout = 60.seconds,
        ) {
            val model =
                challenged(
                    twoFactorSource(
                        StandardTestDispatcher(
                            testScheduler,
                        ),
                    ),
                ).twoFactor.value!!
            model.editCode(
                "123456",
            )
            assertFalse(
                model.state.value.toString().contains(
                    "123456",
                ),
            )
            model.form.receive(
                DataFailure.Server(
                    500,
                ),
            )
            model.editCode(
                "123456",
            )
            assertEquals(
                AuthBannerKind.Server,
                model.form.state.value.banner,
            )
        }

    companion object {
        private const val REJECTED = "That code didn't work. Codes change every 30 seconds, so try the newest one."
    }
}
