@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.plan

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
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.sync.SyncEvent
import net.chronoflow.core.sync.SyncResult
import net.chronoflow.core.sync.SyncState
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class PlanViewModelTest {
    @Test fun finishedEventFromAnotherAccountDoesNotSetOffline() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                deps.events.emit(
                    SyncEvent.Finished(
                        AccountID.of("019a562b-8000-7000-8000-000000000072"),
                        deps.snapshot.epoch,
                        SyncResult(SyncState.Offline, 0),
                    ),
                )
                runCurrent()
                assertFalse(vm.ui.value.plan.offline)
            }
        }

    @Test fun finishedEventFromStaleEpochDoesNotSetOffline() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                deps.events.emit(finished(deps, SyncState.Offline, deps.snapshot.epoch - 1))
                runCurrent()
                assertFalse(vm.ui.value.plan.offline)
            }
        }

    @Test fun matchingFinishedEventSetsAndClearsOffline() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                deps.events.emit(finished(deps, SyncState.Offline))
                runCurrent()
                assertTrue(vm.ui.value.plan.offline)
                deps.events.emit(SyncEvent.Started(AccountID.of(deps.snapshot.session.accountId), deps.snapshot.epoch))
                runCurrent()
                assertTrue(vm.ui.value.plan.offline)
                deps.events.emit(finished(deps, SyncState.Idle))
                runCurrent()
                assertFalse(vm.ui.value.plan.offline)
            }
        }

    @Test fun renewLineWaitsForLanguageAndUsesTheSelectedLanguage() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies(backgroundScope).apply { plan(unlimited = true, date = true) }
            val formatter = deps.makeFormatter
            var formats = 0
            deps.makeFormatter = {
                formats++
                formatter()
            }
            withModel(deps) { vm, _ ->
                assertTrue(vm.ui.value.plan.unlimited)
                assertNull(vm.ui.value.line)
                assertEquals(0, formats)
                vm.setLanguage(Language.En)
                runCurrent()
                assertEquals(
                    SettingsPlanLine("Renews on {{date}} · {{interval}}", "14 Nov 2026", "yearly"),
                    vm.ui.value.line,
                )
                assertEquals(1, formats)
                vm.setLanguage(Language.Nl)
                runCurrent()
                assertEquals(
                    "14 nov 2026",
                    vm.ui.value.line
                        ?.date,
                )
            }
        }

    @Test fun unlimitedWithoutADateDoesNotFormatALine() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies(backgroundScope).apply { plan(unlimited = true, date = false) }
            val formatter = deps.makeFormatter
            var formats = 0
            deps.makeFormatter = {
                formats++
                formatter()
            }
            withModel(deps) { vm, _ ->
                vm.setLanguage(Language.En)
                runCurrent()
                assertTrue(vm.ui.value.plan.unlimited)
                assertNull(vm.ui.value.line)
                assertEquals(0, formats)
            }
        }

    @Test fun creditsWithADateDoNotFormatASubscriptionLine() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies(backgroundScope).apply { plan(unlimited = false, date = true) }
            val formatter = deps.makeFormatter
            var formats = 0
            deps.makeFormatter = {
                formats++
                formatter()
            }
            withModel(deps) { vm, _ ->
                vm.setLanguage(Language.En)
                runCurrent()
                assertEquals(2, vm.ui.value.plan.credits)
                assertNull(vm.ui.value.line)
                assertEquals(0, formats)
            }
        }

    @Test fun cancelledSubscriptionUsesTheEndsLine() =
        runTest(timeout = 60.seconds) {
            val deps =
                FakeSettingsDependencies(backgroundScope).apply {
                    settings.value =
                        listOf(
                            SettingsRecord("planCredits", """{"unlimited":true}"""),
                            SettingsRecord("billing", """{"subscription":{"canResume":true,"endsAt":"2026-11-14"}}"""),
                        )
                }
            withModel(deps) { vm, _ ->
                vm.setLanguage(Language.En)
                runCurrent()
                assertEquals(
                    "Ends on {{date}}. After that, you can buy credits per trip.",
                    vm.ui.value.line
                        ?.key,
                )
                assertEquals(
                    "14 Nov 2026",
                    vm.ui.value.line
                        ?.date,
                )
            }
        }

    @Test fun secondRefreshIsSkippedUntilTheFirstReturns() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                val returned = CompletableDeferred<Unit>()
                var calls = 0
                deps.refresh = {
                    calls++
                    returned.await()
                }
                vm.refresh()
                vm.refresh()
                runCurrent()
                assertEquals(1, calls)
                assertTrue(vm.ui.value.refreshing)
                vm.refresh()
                runCurrent()
                assertEquals(1, calls)
                returned.complete(Unit)
                runCurrent()
                assertFalse(vm.ui.value.refreshing)
                vm.refresh()
                runCurrent()
                assertEquals(2, calls)
                assertFalse(vm.ui.value.refreshing)
            }
        }

    @Test fun refreshFailureStopsTheIndicatorAndAllowsRetry() =
        runTest(timeout = 60.seconds) {
            withModel { vm, deps ->
                deps.refresh = { throw java.io.IOException() }
                vm.refresh()
                runCurrent()
                assertFalse(vm.ui.value.refreshing)
                var retried = false
                deps.refresh = { retried = true }
                vm.refresh()
                runCurrent()
                assertTrue(retried)
                assertFalse(vm.ui.value.refreshing)
            }
        }

    private fun finished(
        deps: FakeSettingsDependencies,
        state: SyncState,
        epoch: Long = deps.snapshot.epoch,
    ) = SyncEvent.Finished(AccountID.of(deps.snapshot.session.accountId), epoch, SyncResult(state, 0))

    private fun FakeSettingsDependencies.plan(
        unlimited: Boolean,
        date: Boolean,
    ) {
        settings.value =
            listOf(
                SettingsRecord("planCredits", if (unlimited) """{"unlimited":true}""" else """{"credits":2}"""),
                SettingsRecord(
                    "billing",
                    if (date) {
                        """{"subscription":{"tier":"annual","renewsAt":"2026-11-14"}}"""
                    } else {
                        """{"subscription":{}}"""
                    },
                ),
            )
    }

    private suspend fun TestScope.withModel(
        deps: FakeSettingsDependencies = FakeSettingsDependencies(backgroundScope),
        block: suspend (PlanViewModel, FakeSettingsDependencies) -> Unit,
    ) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val vm = PlanViewModel(deps, dispatcher, dispatcher)
        val store = ViewModelStore().apply { put("plan", vm) }
        try {
            runCurrent()
            block(vm, deps)
        } finally {
            withContext(NonCancellable) {
                store.clear()
                vm.viewModelScope.coroutineContext.job
                    .cancelAndJoin()
            }
        }
    }
}
