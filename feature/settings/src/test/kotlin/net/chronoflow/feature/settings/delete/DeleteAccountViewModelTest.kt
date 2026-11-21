@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.delete

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import net.chronoflow.feature.settings.testing.FakeSecretField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class DeleteAccountViewModelTest {
    @Test fun resetUsesTheObservedAccountEmailAndPublishesItsMessage() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies(backgroundScope)
            var sent: AccountSecurityChange? = null
            deps.security = {
                sent = it
                AccountSecurityResult.Changed
            }
            withDeleteAccountModel(deps) { vm ->
                assertEquals(deps.account.value?.email, vm.ui.value.email)
                vm.event(DeleteAccountEvent.Confirm)
                vm.event(DeleteAccountEvent.Reset)
                runCurrent()
                assertEquals(AccountSecurityChange.RequestPasswordReset(vm.ui.value.email), sent)
                assertEquals("Check your inbox.", vm.ui.value.reset.message)
                vm.stop()
                runCurrent()
                assertEquals(SettingsDeleteModel.Stage.Confirmation, vm.ui.value.delete.stage)
                assertNull(vm.ui.value.reset.message)
            }
        }

    @Test fun deletionBlocksAnotherSubmitAndResetWhileItRuns() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies(backgroundScope)
            val returned = CompletableDeferred<Unit>()
            var deletes = 0
            var resets = 0
            deps.delete = {
                deletes++
                returned.await()
            }
            deps.security = {
                resets++
                AccountSecurityResult.Changed
            }
            withDeleteAccountModel(deps) { vm ->
                vm.event(DeleteAccountEvent.Confirm)
                vm.event(DeleteAccountEvent.Submit(FakeSecretField("pw")))
                runCurrent()
                assertTrue(vm.ui.value.busy)
                val skipped = FakeSecretField("pw")
                vm.event(DeleteAccountEvent.Submit(skipped))
                vm.event(DeleteAccountEvent.Reset)
                runCurrent()
                assertEquals(1, deletes)
                assertEquals(0, resets)
                assertFalse(skipped.cleared)
                returned.complete(Unit)
                runCurrent()
                assertFalse(vm.ui.value.busy)
                assertEquals(SettingsDeleteModel.Stage.Deleted, vm.ui.value.delete.stage)
            }
        }

    @Test fun deleteAndResetUseTheLaterRetryDeadlineFromTheInjectedClock() =
        runTest(timeout = 60.seconds) {
            val deps =
                FakeSettingsDependencies(backgroundScope).apply {
                    delete = {
                        throw net.chronoflow.core.model.DataFailure
                            .Throttled(30.0, 429)
                    }
                    security = {
                        throw net.chronoflow.core.model.DataFailure
                            .Throttled(60.0, 429)
                    }
                }
            withDeleteAccountModel(deps) { vm ->
                vm.event(DeleteAccountEvent.Confirm)
                vm.event(DeleteAccountEvent.Submit(FakeSecretField("pw")))
                runCurrent()
                assertEquals(deps.clock.now.plusSeconds(30), vm.ui.value.retryAt)
                vm.event(DeleteAccountEvent.Reset)
                runCurrent()
                assertEquals(deps.clock.now.plusSeconds(60), vm.ui.value.retryAt)
                assertFalse(vm.ui.value.busy)
            }
        }

    @Test fun resetFailureIsMappedWithoutClosingThePasswordStage() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies(backgroundScope).apply { security = { throw java.io.IOException() } }
            withDeleteAccountModel(deps) { vm ->
                vm.event(DeleteAccountEvent.Confirm)
                vm.event(DeleteAccountEvent.Reset)
                runCurrent()
                assertEquals("Something went wrong", vm.ui.value.reset.message)
                assertEquals(SettingsDeleteModel.Stage.Password, vm.ui.value.delete.stage)
                assertFalse(vm.ui.value.busy)
            }
        }

    @Test fun resetBlocksDeletionUntilItReturns() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies(backgroundScope)
            val returned = CompletableDeferred<AccountSecurityResult>()
            var deletes = 0
            deps.security = { returned.await() }
            deps.delete = { deletes++ }
            withDeleteAccountModel(deps) { vm ->
                vm.event(DeleteAccountEvent.Confirm)
                vm.event(DeleteAccountEvent.Reset)
                runCurrent()
                assertTrue(vm.ui.value.busy)
                val field = FakeSecretField("pw")
                vm.event(DeleteAccountEvent.Submit(field))
                runCurrent()
                assertEquals(0, deletes)
                assertFalse(field.cleared)
                returned.complete(AccountSecurityResult.Changed)
                runCurrent()
                assertFalse(vm.ui.value.busy)
                vm.event(DeleteAccountEvent.Submit(field))
                runCurrent()
                assertEquals(1, deletes)
            }
        }
}

internal suspend fun TestScope.withDeleteAccountModel(
    deps: FakeSettingsDependencies,
    block: suspend (DeleteAccountViewModel) -> Unit,
) {
    val vm = DeleteAccountViewModel(deps, StandardTestDispatcher(testScheduler))
    val store = ViewModelStore().apply { put("delete", vm) }
    try {
        runCurrent()
        block(vm)
    } finally {
        withContext(NonCancellable) {
            store.clear()
            vm.viewModelScope.coroutineContext.job
                .cancelAndJoin()
        }
    }
}
