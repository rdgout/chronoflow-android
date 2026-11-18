package net.chronoflow.feature.auth.model

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.datamock.MockMethod
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.EmailVerificationStatus
import net.chronoflow.core.model.api.SignedVerificationInput
import net.chronoflow.core.model.device.DeviceIdentityFailure
import net.chronoflow.core.model.session.SessionFailure
import net.chronoflow.core.model.store.StoreFailure
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
class VerifyEmailModelTests {
    class Behavior {
        @Test fun signedMockMailPersistsOnceAndRoutesOnlyForMatchingAccountEmailEpoch() =
            runTest(timeout = 60.seconds) {
                val f = registered()
                val model = f.model()
                model.receiveLink(f.input)
                model.activate()
                assertEquals(listOf(f.context), f.stored)
                assertEquals(1, f.routes)
                assertEquals(VerifyState.Waiting, model.state)
                assertTrue(f.source.fetchIdentity().hasVerifiedEmail)
                model.cancel()
                model.activate()
                assertEquals(1, f.routes)
                assertEquals(1, f.stored.size)
            }

        @Test fun invalidExpiredForeignAndTamperedLinksUseFailedAndRetryChecksStatusMalformed() = invalid { null }

        @Test fun invalidExpiredForeignAndTamperedLinksUseFailedAndRetryChecksStatusTampered() =
            invalid {
                it.copy(signature = "0".repeat(64))
            }

        @Test fun invalidExpiredForeignAndTamperedLinksUseFailedAndRetryChecksStatusExpired() =
            invalid { it.copy(expires = 1) }

        @Test fun invalidExpiredForeignAndTamperedLinksUseFailedAndRetryChecksStatusForeign() =
            invalid {
                it.copy(id = "0199a3c0-0000-7000-8000-000000000099")
            }

        private fun invalid(input: (SignedVerificationInput) -> SignedVerificationInput?) =
            runTest(timeout = 60.seconds) {
                val f = registered()
                val model = f.model()
                model.receiveLink(input(f.input))
                model.activate()
                assertEquals(VerifyState.Failed, model.state)
                model.check()
                assertEquals(VerifyState.Waiting, model.state)
                assertTrue(model.uiState.value.hasStatus)
                assertTrue(f.stored.isEmpty())
                assertEquals(0, f.routes)
            }

        @Test fun resendDeadlineSurvivesIdentityRefreshActivationAndBackgroundTime() =
            runTest(timeout = 60.seconds) {
                val f = registered()
                val model = f.model()
                model.activate()
                assertTrue(model.canResend)
                model.resend()
                assertEquals(AuthBannerKind.Locked, model.form.state.value.banner)
                assertEquals(60, model.remaining)
                assertFalse(model.canResend)
                model.activate()
                assertEquals(60, model.remaining)
                f.clock.advance(42.0)
                model.activate()
                assertEquals(18, model.remaining)
                model.resend()
                assertEquals(1, f.source.inbox().size)
                f.clock.advance(18.0)
                model.tick()
                assertTrue(model.canResend)
                model.resend()
                assertEquals(60, model.remaining)
                assertNull(model.form.state.value.failure)
                assertEquals(2, f.source.inbox().size)
            }

        @Test fun activationAfterWebVerificationAndEmailAppReturnObserveTheServer() =
            runTest(timeout = 60.seconds) {
                val f = registered()
                val model = f.model()
                model.activate()
                assertEquals(0, f.routes)
                f.source.verifyEmail(f.input)
                model.activate()
                assertEquals(1, f.routes)
                var opened = 0
                model.openEmail {
                    opened++
                    false
                }
                assertEquals(1, opened)
                assertEquals(1, f.routes)
            }

        @Test fun staleSessionOrEmailAndCanceledResponsesCannotPersistOrRouteSignedOut() = stale { null }

        @Test fun staleSessionOrEmailAndCanceledResponsesCannotPersistOrRouteAccount() =
            stale {
                it.copy(accountId = "0199a3c0-0000-7000-8000-000000000099")
            }

        @Test fun staleSessionOrEmailAndCanceledResponsesCannotPersistOrRouteEmail() =
            stale {
                it.copy(email = "changed@example.com")
            }

        @Test fun staleSessionOrEmailAndCanceledResponsesCannotPersistOrRouteEpoch() =
            stale {
                it.copy(
                    epoch =
                        it.epoch + 1,
                )
            }

        private fun stale(next: (VerificationContext) -> VerificationContext?) =
            runTest(timeout = 60.seconds) {
                val f = registered()
                val model = f.model()
                f.current = next(f.context)
                model.receiveLink(f.input)
                model.check()
                assertFalse(model.uiState.value.hasStatus)
                assertTrue(f.stored.isEmpty())
                assertEquals(0, f.routes)
            }

        @Test fun staleSessionOrEmailAndCanceledResponsesCannotPersistOrRouteCanceled() =
            runTest(timeout = 60.seconds) {
                val entered = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                val f = registered()
                val source =
                    object : DataSource by f.source {
                        override suspend fun verificationStatus(): EmailVerificationStatus {
                            entered.complete(Unit)
                            release.await()
                            return f.source.verificationStatus()
                        }
                    }
                val model = f.model(source = source)
                val job = launch { model.check() }
                entered.await()
                model.cancel()
                job.cancel()
                release.complete(Unit)
                job.join()
                assertFalse(model.uiState.value.hasStatus)
                assertNull(model.form.state.value.failure)
                assertTrue(f.stored.isEmpty())
                assertEquals(0, f.routes)
            }

        @Test fun responseArrivingAfterEmailChangeCannotWriteAndStaleBackCannotDiscard() =
            runTest(timeout = 60.seconds) {
                val gate = TestGate()
                val f = registered(sleep = { gate.wait() })
                val model = f.model()
                model.receiveLink(f.input)
                gate.armed = true
                val job = launch { model.check() }
                gate.entered.await()
                f.current = f.context.copy(email = "changed@example.com")
                gate.released.complete(Unit)
                job.join()
                assertFalse(model.uiState.value.hasStatus)
                model.signOut()
                assertEquals(0, f.discarded)
                assertTrue(model.isActive)
                assertTrue(f.stored.isEmpty())
            }

        @Test fun serverOfflineUnverifiedFailureDoesNotLoopAndBackExplicitlyDiscardsOffline() =
            failedCheck(DataFailure.Offline)

        @Test fun serverOfflineUnverifiedFailureDoesNotLoopAndBackExplicitlyDiscardsServer() =
            failedCheck(DataFailure.Server(503))

        @Test fun serverOfflineUnverifiedFailureDoesNotLoopAndBackExplicitlyDiscardsUnverified() =
            failedCheck(DataFailure.EmailUnverified(403))

        private fun failedCheck(failure: DataFailure) =
            runTest(timeout = 60.seconds) {
                val f = registered()
                val model =
                    f.model(discard = {
                        f.discarded++
                        f.source.signOut()
                    })
                f.source.failNext(MockMethod.VerificationStatus, failure)
                model.check()
                assertEquals(VerifyState.Failed, model.state)
                assertEquals(
                    if (failure ==
                        DataFailure.Offline
                    ) {
                        AuthBannerKind.Offline
                    } else {
                        AuthBannerKind.Server
                    },
                    model.form.state.value.banner,
                )
                assertNotNull(model.takeAnnouncement { it.key })
                assertNull(model.takeAnnouncement { it.key })
                model.signOut()
                assertEquals(1, f.discarded)
                assertFalse(model.isActive)
                try {
                    f.source.fetchIdentity()
                    error("old token survived")
                } catch (failure: DataFailure.Unauthorized) {
                    assertEquals(401, failure.status)
                }
            }

        @Test fun signOutFailureIsAnnouncedOncePerAttemptAndReplacedByTheNextFailureCheck() = replaceSignOut(false)

        @Test fun signOutFailureIsAnnouncedOncePerAttemptAndReplacedByTheNextFailureResend() = replaceSignOut(true)

        private fun replaceSignOut(resend: Boolean) =
            runTest(timeout = 60.seconds) {
                val f = registered()
                val model = f.model(discard = { throw DataFailure.Offline })
                model.activate()

                suspend fun fail() {
                    model.signOut()
                    assertEquals(DataFailure.Offline, model.uiState.value.signOutFailure)
                    assertTrue(model.isActive)
                    assertEquals("Needs a connection", model.takeAnnouncement { it.key })
                    assertNull(model.takeAnnouncement { it.key })
                }
                fail()
                fail()
                f.source.failNext(
                    if (resend) MockMethod.ResendVerificationEmail else MockMethod.VerificationStatus,
                    DataFailure.Server(503),
                )
                if (resend) model.resend() else model.check()
                assertNull(model.uiState.value.signOutFailure)
                assertEquals(AuthBannerKind.Server, model.form.state.value.banner)
                assertNotNull(model.takeAnnouncement { it.key })
            }

        @Test fun offlineResendShowsAndAnnouncesTheConnectionBanner() =
            runTest(timeout = 60.seconds) {
                val f = registered()
                val model = f.model()
                model.activate()
                f.source.setOffline(true)
                model.resend()
                assertEquals(AuthBannerKind.Offline, model.form.state.value.banner)
                assertFalse(model.form.busy)
                assertTrue(model.canResend)
                assertEquals("Needs a connection", model.takeAnnouncement { it.key })
            }

        @Test fun repeatedSignOutFailuresEachShowAndAnnounceTheConnectionBanner() =
            runTest(timeout = 60.seconds) {
                val f = registered()
                val model = f.model(discard = { throw DataFailure.Offline })

                suspend fun attempt() {
                    model.signOut()
                    assertEquals(AuthBannerKind.Offline, model.form.state.value.banner)
                    assertEquals(DataFailure.Offline, model.uiState.value.signOutFailure)
                    assertTrue(model.isActive)
                    assertEquals("Needs a connection", model.takeAnnouncement { it.key })
                    assertNull(model.takeAnnouncement { it.key })
                }
                attempt()
                attempt()
            }

        @Test fun newerLinkDuringAnAwaitIsCheckedAfterTheOldRequestUnlocks() =
            runTest(timeout = 60.seconds) {
                val gate = TestGate()
                val f = registered(sleep = { gate.wait() })
                val model = f.model()
                gate.armed = true
                val job = launch { model.check() }
                gate.entered.await()
                model.receiveLink(f.input)
                gate.armed = false
                gate.released.complete(Unit)
                job.join()
                runCurrent()
                assertEquals(listOf(f.context), f.stored)
                assertEquals(1, f.routes)
            }
    }

    @RunWith(Parameterized::class)
    class SessionFailures(
        private val failure: SessionFailure,
        private val action: String,
    ) {
        @Test fun sessionFailuresAreVisibleOnCheckActivationResendAndSignOut() =
            runTest(timeout = 60.seconds) {
                val f = registered()
                var shouldFail = true
                val model =
                    f.model(persist = {
                        if (shouldFail) throw failure
                        true
                    }, discard = { throw failure })
                model.activate()
                f.source.verifyEmail(f.input)
                when (action) {
                    "activate" -> model.activate()
                    "resend" -> model.resend()
                    "signOut" -> model.signOut()
                    else -> model.check()
                }
                assertEquals(failure, model.uiState.value.sessionFailure)
                assertEquals(AuthBannerKind.Server, model.form.state.value.banner)
                assertEquals(0, f.routes)
                assertNotNull(model.takeAnnouncement { it.key })
                shouldFail = false
                model.check()
                assertNull(model.uiState.value.sessionFailure)
                assertNull(model.form.state.value.failure)
                assertEquals(1, f.routes)
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
                    listOf("check", "activate", "resend", "signOut").map { arrayOf(failure, it) }
                }
        }
    }

    @RunWith(Parameterized::class)
    class UnexpectedFailures(
        private val failure: Throwable,
        private val action: String,
    ) {
        @Test fun checkedFailuresHaveVisibleFeedbackAndUnlockTheForm() =
            runTest(timeout = 60.seconds) {
                val f = registered()
                val source =
                    object : DataSource by f.source {
                        override suspend fun verificationStatus(): EmailVerificationStatus = throw failure

                        override suspend fun resendVerificationEmail(): EmailVerificationStatus = throw failure
                    }
                val model = f.model(source = source, discard = { throw failure })
                if (action == "resend") {
                    model.receiveLink(null)
                    model.check()
                }
                when (action) {
                    "signOut" -> model.signOut()
                    "resend" -> model.resend()
                    else -> model.check()
                }
                assertEquals(AuthBannerKind.Server, model.form.state.value.banner)
                assertFalse(model.form.busy)
                assertNotNull(model.takeAnnouncement { it.key })
            }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}-{1}")
            fun cases(): List<Array<Any>> =
                listOf(
                    DeviceIdentityFailure.InvalidPersistedIdentity,
                    StoreFailure.Closed,
                    IOException(),
                    GeneralSecurityException(),
                    IllegalStateException(),
                ).flatMap { failure ->
                    listOf("check", "resend", "signOut").map { arrayOf(failure, it) }
                }
        }
    }
}
