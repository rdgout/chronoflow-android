@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.onboarding.terms

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.session.SignOutResult
import net.chronoflow.feature.onboarding.OnboardingSession
import net.chronoflow.feature.onboarding.OnboardingSessions
import net.chronoflow.feature.onboarding.support.FakeOnboardingSessions
import net.chronoflow.feature.onboarding.support.withOnboardingFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class TermsViewModelTests {
    @Test fun malformedPoliciesRetryAfter100MillisecondsAndKeepLastGoodState() =
        runTest(timeout = 60.seconds) {
            val initial = AccountPolicies("1", "1")
            val recovered = AccountPolicies("2", "2")
            var subscriptions = 0
            val fake = FakeOnboardingSessions().session
            val session =
                OnboardingSession(
                    fake.snapshot,
                    fake.enqueue,
                    flow {
                        subscriptions++
                        emit(if (subscriptions == 1) initial else recovered)
                        if (subscriptions == 1) {
                            emit(ApiJson.response.decodeFromString(AccountPolicies.serializer(), "{}"))
                        }
                        awaitCancellation()
                    },
                    fake.signOut,
                )
            val model = TermsViewModel(OnboardingSessions { flowOf(session) }, StandardTestDispatcher(testScheduler))
            val store = ViewModelStore().apply { put("terms", model) }
            val observed = mutableListOf<TermsUiState?>()
            backgroundScope.launch { model.state.collect { observed += it } }
            try {
                runCurrent()
                assertEquals(initial, model.state.value?.policies)
                assertFalse(model.state.value!!.failed)
                advanceTimeBy(99)
                runCurrent()
                assertEquals(1, subscriptions)
                assertEquals(initial, model.state.value?.policies)
                advanceTimeBy(1)
                runCurrent()
                assertEquals(2, subscriptions)
                assertEquals(recovered, observed.last()?.policies)
                assertTrue(model.state.value!!.acceptEnabled)
                assertFalse(model.state.value!!.failed)
            } finally {
                withContext(NonCancellable) {
                    store.clear()
                    model.viewModelScope.coroutineContext.job
                        .cancelAndJoin()
                }
            }
        }

    @Test fun sessionChangeStopsPolicyRetriesAndClearsTheOldState() =
        runTest(timeout = 60.seconds) {
            val fake = FakeOnboardingSessions().session
            val policies = AccountPolicies("1", "1")
            var subscriptions = 0
            var writes = 0
            val session =
                OnboardingSession(
                    fake.snapshot,
                    { writes++ },
                    flow {
                        subscriptions++
                        emit(policies)
                        emit(ApiJson.response.decodeFromString(AccountPolicies.serializer(), "{}"))
                    },
                    fake.signOut,
                )
            val sessions = MutableStateFlow<OnboardingSession?>(session)
            val model = TermsViewModel(OnboardingSessions { sessions }, StandardTestDispatcher(testScheduler))
            val store = ViewModelStore().apply { put("terms", model) }
            backgroundScope.launch { model.state.collect {} }
            try {
                runCurrent()
                assertEquals(policies, model.state.value?.policies)
                advanceTimeBy(50)
                sessions.value =
                    OnboardingSession(
                        fake.snapshot.copy(epoch = fake.snapshot.epoch + 1),
                        fake.enqueue,
                        fake.policies,
                        fake.signOut,
                    )
                runCurrent()
                assertNull(model.state.value)
                model.accept()
                advanceTimeBy(1_000)
                runCurrent()
                assertEquals(1, subscriptions)
                assertEquals(0, writes)
                assertNull(model.state.value)
            } finally {
                withContext(NonCancellable) {
                    store.clear()
                    model.viewModelScope.coroutineContext.job
                        .cancelAndJoin()
                }
            }
        }

    @Test fun acceptDisabledUntilPoliciesLoadAndWhenAlreadyAccepted() =
        runTest(timeout = 60.seconds) {
            val fake = FakeOnboardingSessions()
            withModel(fake) { model ->
                assertFalse(model.state.value!!.acceptEnabled)
                fake.policies.value = AccountPolicies("1", "1", "1")
                runCurrent()
                assertFalse(model.state.value!!.acceptEnabled)
                fake.policies.value = AccountPolicies("1", "1")
                runCurrent()
                assertTrue(model.state.value!!.acceptEnabled)
            }
        }

    @Test fun failedSaveShowsBannerAndTryAgain() =
        runTest(timeout = 60.seconds) {
            val fake = FakeOnboardingSessions(enqueue = { throw IllegalStateException() })
            fake.policies.value = AccountPolicies("1", "1")
            withModel(fake) { model ->
                model.accept()
                runCurrent()
                assertTrue(model.state.value!!.failed)
                assertTrue(model.state.value!!.acceptEnabled)
                assertFalse(model.state.value!!.completed)
            }
        }

    @Test fun busyAcceptanceDisablesBothButtonsAndIgnoresRepeatedTaps() =
        runTest(timeout = 60.seconds) {
            val gate = CompletableDeferred<Unit>()
            var writes = 0
            val fake =
                FakeOnboardingSessions(enqueue = {
                    writes++
                    gate.await()
                })
            fake.policies.value = AccountPolicies("1", "1")
            withModel(fake) { model ->
                model.accept()
                runCurrent()
                assertFalse(model.state.value!!.acceptEnabled)
                assertFalse(model.state.value!!.signOutEnabled)
                model.accept()
                runCurrent()
                assertEquals(1, writes)
                gate.complete(Unit)
                runCurrent()
                assertTrue(model.state.value!!.completed)
            }
        }

    @Test fun signOutBusyDisablesBothButtons() =
        runTest(timeout = 60.seconds) {
            val gate = CompletableDeferred<SignOutResult>()
            var calls = 0
            val fake =
                FakeOnboardingSessions(signOut = {
                    calls++
                    gate.await()
                })
            fake.policies.value = AccountPolicies("1", "1")
            withModel(fake) { model ->
                model.signOut()
                runCurrent()
                assertTrue(model.state.value!!.signOutBusy)
                assertFalse(model.state.value!!.acceptEnabled)
                assertFalse(model.state.value!!.signOutEnabled)
                model.signOut()
                model.accept()
                runCurrent()
                assertEquals(1, calls)
                gate.complete(SignOutResult.SignedOut)
                runCurrent()
                assertFalse(model.state.value!!.signOutBusy)
            }
        }

    @Test fun signOutFailureShowsMessage() =
        runTest(timeout = 60.seconds) {
            withModel(FakeOnboardingSessions(signOut = { throw IllegalStateException() })) { model ->
                model.signOut()
                runCurrent()
                assertTrue(model.state.value!!.signOutFailed)
                assertFalse(model.state.value!!.signOutBusy)
                assertTrue(model.state.value!!.signOutEnabled)
            }
        }

    @Test fun needsConfirmationKeepsCountAndStayClearsIt() =
        runTest(timeout = 60.seconds) {
            var calls = 0
            val fake =
                FakeOnboardingSessions(signOut = {
                    calls++
                    SignOutResult.NeedsConfirmation(1)
                })
            withModel(fake) { model ->
                model.signOut(discardPending = true)
                runCurrent()
                assertEquals(0, calls)
                model.signOut()
                runCurrent()
                assertEquals(1, model.state.value!!.pendingSignOutCount)
                model.stay()
                runCurrent()
                assertNull(model.state.value!!.pendingSignOutCount)
            }
        }

    @Test fun signOutFromTermsUsesExistingPendingConfirmationStayAndDiscard() =
        runTest(timeout = 60.seconds) {
            withOnboardingFixture(termsPending = true) { f ->
                f.outbox.enqueue(
                    OutboxChange.Settings(
                        SettingsSection.Preferences,
                        JsonObject(mapOf("time_format" to JsonPrimitive("12"))),
                    ),
                    f.permit,
                )
                val model = TermsViewModel(OnboardingSessions { flowOf(f.session()) }, f.io)
                val store = ViewModelStore().apply { put("terms", model) }
                try {
                    runCurrent()
                    model.signOut()
                    runCurrent()
                    assertEquals(1, model.state.value!!.pendingSignOutCount)
                    assertNotNull(f.controller.activeSession())
                    model.stay()
                    runCurrent()
                    assertNull(model.state.value!!.pendingSignOutCount)
                    model.signOut()
                    runCurrent()
                    model.signOut(discardPending = true)
                    runCurrent()
                    assertNull(f.controller.activeSession())
                } finally {
                    withContext(NonCancellable) {
                        store.clear()
                        model.viewModelScope.coroutineContext.job
                            .cancelAndJoin()
                    }
                }
            }
        }

    private suspend fun TestScope.withModel(
        fake: FakeOnboardingSessions,
        block: suspend (TermsViewModel) -> Unit,
    ) {
        val model = TermsViewModel(fake, StandardTestDispatcher(testScheduler))
        val store = ViewModelStore().apply { put("terms", model) }
        try {
            runCurrent()
            block(model)
        } finally {
            withContext(NonCancellable) {
                store.clear()
                model.viewModelScope.coroutineContext.job
                    .cancelAndJoin()
            }
        }
    }
}
