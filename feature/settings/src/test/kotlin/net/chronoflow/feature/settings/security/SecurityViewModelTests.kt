@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.security

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.ApiTwoFactorSetup
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import net.chronoflow.feature.settings.testing.FakeSecretField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SecurityViewModelTests {
    @Test fun clearingSecretsAllowsANewSubmissionWhileTheOldRequestFinishes() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                val first = CompletableDeferred<AccountSecurityResult>()
                val second = CompletableDeferred<AccountSecurityResult>()
                var calls = 0
                deps.security = { if (++calls == 1) first.await() else second.await() }
                vm.showPrompt(SettingsSecurityPrompt.Enable)
                vm.submitPrompt(FakeSecretField("password"))
                runCurrent()
                assertTrue(vm.state.value.busy)

                vm.clearSecrets()
                assertFalse(vm.state.value.busy)
                vm.showPrompt(SettingsSecurityPrompt.Enable)
                vm.submitPrompt(FakeSecretField("password"))
                runCurrent()
                assertEquals(2, calls)
                assertTrue(vm.state.value.busy)

                first.complete(AccountSecurityResult.Setup(ApiTwoFactorSetup("OLD", "otpauth://old")))
                runCurrent()
                assertNull(vm.state.value.setup)
                assertTrue(vm.state.value.busy)
                assertEquals(SettingsSecurityPrompt.Enable, vm.state.value.prompt)

                val latest = ApiTwoFactorSetup("NEW", "otpauth://new")
                second.complete(AccountSecurityResult.Setup(latest))
                runCurrent()
                assertEquals(latest, vm.state.value.setup)
                assertFalse(vm.state.value.busy)
                assertNull(vm.state.value.prompt)
            }
        }

    @Test fun successClearsAllFieldsAndHoldsFeedbackUntilAcknowledged() =
        runTest(timeout = 60.seconds) {
            withModel { vm, _ ->
                val current = FakeSecretField("password")
                val new = FakeSecretField("New password1!")
                val confirmation = FakeSecretField("New password1!")
                vm.changePassword(current, new, confirmation)
                runCurrent()
                assertTrue(current.cleared && new.cleared && confirmation.cleared)
                val feedback = checkNotNull(vm.ui.value.feedback)
                assertEquals("Password updated", feedback.key)
                vm.feedbackPresented(feedback.id - 1)
                runCurrent()
                assertNotNull(vm.ui.value.feedback)
                vm.feedbackPresented(feedback.id)
                runCurrent()
                assertNull(vm.ui.value.feedback)
            }
        }

    @Test fun offlineSubmissionClearsFieldsAndShowsFeedback() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                deps.security = { throw DataFailure.Offline }
                val current = FakeSecretField("password")
                vm.changePassword(current, FakeSecretField("new"), FakeSecretField("new"))
                runCurrent()
                assertTrue(current.cleared)
                assertEquals(
                    "Needs a connection.",
                    vm.ui.value.feedback
                        ?.key,
                )
                assertFalse(vm.state.value.busy)
            }
        }

    @Test fun countdownUnlocksAtTheInjectedClockDeadline() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                deps.security = { throw DataFailure.Throttled(2.0, 429) }
                vm.changePassword(FakeSecretField("password"), FakeSecretField("new"), FakeSecretField("new"))
                runCurrent()
                assertNotNull(vm.state.value.retryAt)
                deps.clock.advance(deps.clock.now.plusSeconds(2))
                advanceTimeBy(2000)
                runCurrent()
                assertNull(vm.state.value.retryAt)
            }
        }

    @Test fun staleSessionRemainsSilent() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                deps.security = { throw StaleSessionException() }
                vm.changePassword(FakeSecretField("password"), FakeSecretField("new"), FakeSecretField("new"))
                runCurrent()
                assertNull(vm.ui.value.feedback)
                assertFalse(vm.state.value.busy)
            }
        }

    @Test fun ioFailureBecomesFeedback() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                deps.security = { throw java.io.IOException("test") }
                vm.changePassword(FakeSecretField("password"), FakeSecretField("new"), FakeSecretField("new"))
                runCurrent()
                assertEquals(
                    "Something went wrong",
                    vm.ui.value.feedback
                        ?.key,
                )
            }
        }

    @Test fun foreignRequestCancellationRemainsSilent() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                deps.security = { throw kotlinx.coroutines.CancellationException() }
                vm.changePassword(FakeSecretField("password"), FakeSecretField("new"), FakeSecretField("new"))
                runCurrent()
                assertNull(vm.state.value.message)
                assertNull(vm.ui.value.feedback)
                assertFalse(vm.state.value.busy)
            }
        }

    private suspend fun TestScope.withModel(block: suspend (SecurityViewModel, FakeSettingsDependencies) -> Unit) {
        val deps = FakeSettingsDependencies(backgroundScope)
        val dispatcher = StandardTestDispatcher(testScheduler)
        val vm = SecurityViewModel(deps, dispatcher, dispatcher, dispatcher)
        val store = ViewModelStore().apply { put("security", vm) }
        try {
            runCurrent()
            block(vm, deps)
        } finally {
            kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                store.clear()
                vm.viewModelScope.coroutineContext.job
                    .cancelAndJoin()
            }
        }
    }
}
