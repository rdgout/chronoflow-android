@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.reminders

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
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class RemindersViewModelTest {
    @Test fun setEnabledIgnoredUnlessGranted() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                deps.authorization = ReminderPermission.Undetermined
                vm.refreshPermission()
                runCurrent()
                vm.setEnabled(false)
                deps.authorization = ReminderPermission.Blocked
                vm.refreshPermission()
                runCurrent()
                vm.setEnabled(false)
                deps.authorization = ReminderPermission.Unsupported
                vm.refreshPermission()
                runCurrent()
                vm.setEnabled(false)
                runCurrent()
                assertTrue(deps.patches.isEmpty())
            }
        }

    @Test fun setLeadRejectsOtherMinutes() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                vm.setLead(45)
                vm.setLead(0)
                runCurrent()
                assertTrue(deps.patches.isEmpty())
                vm.setLead(15)
                runCurrent()
                assertEquals(listOf(ReminderSettings.leadPatch(15)), deps.patches)
            }
        }

    @Test fun setLeadNeedsStoredSettings() =
        runTest(timeout = 60.seconds) {
            withModel {
                vm,
                deps,
                ->
                deps.settings.value = null
                runCurrent()
                vm.setLead(15)
                runCurrent()
                assertTrue(deps.patches.isEmpty())
            }
        }

    @Test fun busyBlocksReentry() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                val gate = CompletableDeferred<Unit>()
                deps.enqueue = { gate.await() }
                vm.setEnabled(false)
                runCurrent()
                assertTrue(vm.state.value.busy)
                vm.setEnabled(true)
                vm.setLead(15)
                vm.requestPermission()
                runCurrent()
                gate.complete(Unit)
                runCurrent()
                assertEquals(1, deps.patches.size)
                assertEquals(0, deps.requests)
            }
        }

    @Test fun askWithSystemDialogEmitsOneLaunchAndStaysBusy() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                deps.start = PermissionRequestStart.SystemDialog
                deps.authorization = ReminderPermission.Undetermined
                vm.refreshPermission()
                runCurrent()
                vm.requestPermission()
                runCurrent()
                assertTrue(vm.state.value.busy)
                val id = checkNotNull(vm.permissionLaunches.value)
                vm.requestPermission()
                runCurrent()
                assertEquals(id, vm.permissionLaunches.value)
                assertEquals(1, deps.requests)
                vm.acknowledgePermissionLaunch(id)
                assertNull(vm.permissionLaunches.value)
                deps.authorization = ReminderPermission.Granted
                vm.onPermissionResult()
                runCurrent()
                assertEquals(listOf(ReminderSettings.enabledPatch(true)), deps.patches)
                assertEquals(2, deps.reschedules)
                assertFalse(vm.state.value.busy)
                assertNotNull(vm.permissionRequestsFinished.value)
                vm.onPermissionResult()
                runCurrent()
                assertEquals(1, deps.patches.size)
            }
        }

    @Test fun askDeniedEndsBlockedWithoutWrite() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                deps.start = PermissionRequestStart.SystemDialog
                vm.requestPermission()
                runCurrent()
                deps.authorization = ReminderPermission.Blocked
                vm.onPermissionResult()
                runCurrent()
                assertEquals(ReminderPermission.Blocked, vm.state.value.permission)
                assertTrue(deps.patches.isEmpty())
                assertFalse(vm.state.value.busy)
            }
        }

    @Test fun askWithoutDialogFinishesAtOnce() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                vm.requestPermission()
                runCurrent()
                assertNull(vm.permissionLaunches.value)
                assertEquals(listOf(ReminderSettings.enabledPatch(true)), deps.patches)
                assertFalse(vm.state.value.busy)
                val id = checkNotNull(vm.permissionRequestsFinished.value)
                vm.acknowledgePermissionFinished(id)
                assertNull(vm.permissionRequestsFinished.value)
            }
        }

    @Test fun resumeWhileBusySkipsAutoEnable() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                deps.authorization = ReminderPermission.Undetermined
                vm.refreshPermission()
                runCurrent()
                deps.start = PermissionRequestStart.SystemDialog
                vm.requestPermission()
                runCurrent()
                deps.authorization = ReminderPermission.Granted
                vm.refreshPermission()
                runCurrent()
                assertTrue(deps.patches.isEmpty())
                vm.onPermissionResult()
                runCurrent()
                assertEquals(1, deps.patches.size)
            }
        }

    @Test fun returnFromBlockedEnablesOnce() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                deps.authorization = ReminderPermission.Blocked
                vm.refreshPermission()
                runCurrent()
                deps.authorization = ReminderPermission.Granted
                vm.refreshPermission()
                runCurrent()
                vm.refreshPermission()
                runCurrent()
                assertEquals(listOf(ReminderSettings.enabledPatch(true)), deps.patches)
            }
        }

    @Test fun firstReadNeverWrites() =
        runTest(timeout = 60.seconds) {
            withModel { _, deps -> assertTrue(deps.patches.isEmpty()) }
        }

    @Test fun cancellationNeverSetsFailure() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                deps.enqueue = { throw StaleSessionException() }
                vm.setEnabled(false)
                runCurrent()
                assertFalse(vm.failed)
                assertFalse(vm.state.value.busy)
                assertTrue(deps.patches.isEmpty())
            }
        }

    @Test fun checkedFailureKeepsTheOfferOpenAndResetsBusy() =
        runTest(timeout = 60.seconds) {
            withModel {
                vm,
                deps,
                ->
                deps.enqueue = { throw IOException() }
                vm.requestPermission()
                runCurrent()
                assertTrue(vm.failed)
                assertFalse(vm.state.value.busy)
            }
        }

    private suspend fun TestScope.withModel(
        block: suspend TestScope.(RemindersViewModel, TestRemindersDependencies) -> Unit,
    ) {
        val deps = TestRemindersDependencies()
        val vm = RemindersViewModel(deps, StandardTestDispatcher(testScheduler))
        try {
            vm.start()
            runCurrent()
            block(vm, deps)
        } finally {
            withContext(NonCancellable) {
                vm.stop()
                vm.viewModelScope.coroutineContext.job
                    .cancelAndJoin()
            }
        }
    }
}
