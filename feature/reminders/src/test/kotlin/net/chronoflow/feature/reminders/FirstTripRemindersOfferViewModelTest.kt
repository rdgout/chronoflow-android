@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.reminders

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.reminders.ReminderPermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class FirstTripRemindersOfferViewModelTest {
    private class Dependencies : TestRemindersDependencies() {
        val changes = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1).apply { tryEmit(Unit) }
        val results = mutableListOf<Boolean?>()
        var consumes = 0
        var afterConsume: () -> Unit = {}

        override fun settingsChanges() = changes

        override suspend fun consumeFirstTripOffer(permission: ReminderPermission): Boolean? {
            consumes++
            afterConsume()
            return results.removeFirst()
        }
    }

    @Test fun nullKeepsWaitingUntilSettingsArrive() =
        runTest(timeout = 60.seconds) {
            withOffer(listOf(null, null, true)) { vm, deps ->
                assertFalse(vm.showing.value)
                deps.changes.emit(Unit)
                runCurrent()
                assertFalse(vm.showing.value)
                deps.changes.emit(Unit)
                runCurrent()
                assertTrue(vm.showing.value)
                assertEquals(3, deps.consumes)
            }
        }

    @Test fun falseStopsWithoutShowing() =
        runTest(timeout = 60.seconds) {
            withOffer(listOf(false)) { vm, deps ->
                deps.changes.emit(Unit)
                runCurrent()
                assertFalse(vm.showing.value)
                assertEquals(1, deps.consumes)
            }
        }

    @Test fun trueShowsOnce() =
        runTest(timeout = 60.seconds) {
            withOffer(listOf(true)) { vm, deps ->
                vm.start()
                runCurrent()
                assertTrue(vm.showing.value)
                assertEquals(1, deps.consumes)
            }
        }

    @Test fun staleSessionNeverShows() =
        runTest(timeout = 60.seconds) {
            withOffer(
                listOf(true),
                configure = { afterConsume = { current = false } },
            ) { vm, _ -> assertFalse(vm.showing.value) }
        }

    @Test fun staleBeforeConsumeNeverConsumes() =
        runTest(timeout = 60.seconds) {
            withOffer(emptyList(), configure = { current = false }) { vm, deps ->
                assertFalse(vm.showing.value)
                assertEquals(0, deps.consumes)
            }
        }

    @Test fun dismissHides() =
        runTest(timeout = 60.seconds) {
            withOffer(listOf(true)) { vm, _ ->
                assertTrue(vm.showing.value)
                vm.dismiss()
                assertFalse(vm.showing.value)
            }
        }

    private suspend fun TestScope.withOffer(
        results: List<Boolean?>,
        configure: Dependencies.() -> Unit = {},
        block: suspend TestScope.(FirstTripRemindersOfferViewModel, Dependencies) -> Unit,
    ) {
        val deps =
            Dependencies().apply {
                this.results.addAll(results)
                configure()
            }
        val vm = FirstTripRemindersOfferViewModel(deps, StandardTestDispatcher(testScheduler))
        try {
            vm.start()
            runCurrent()
            block(vm, deps)
        } finally {
            withContext(NonCancellable) {
                vm.viewModelScope.coroutineContext.job
                    .cancelAndJoin()
            }
        }
    }
}
