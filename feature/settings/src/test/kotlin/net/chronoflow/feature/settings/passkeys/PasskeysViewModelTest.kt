@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.passkeys

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.api.ApiPasskey
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.passkey.DeterministicRegistrationProvider
import net.chronoflow.core.model.passkey.PasskeyCapability
import net.chronoflow.core.plan.CopyLineFormatter
import net.chronoflow.core.plan.PlanClock
import net.chronoflow.core.testing.FakePlanClock
import net.chronoflow.feature.settings.SettingsDependencies
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import net.chronoflow.feature.settings.support.SettingsFixture
import net.chronoflow.feature.settings.testing.FakeSecretField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import kotlin.time.Duration.Companion.seconds

class PasskeysViewModelTest {
    @Test fun clockIsCollectedOnlyWhileMetadataHasSubscribersWithAStopGracePeriod() =
        runTest(timeout = 60.seconds) {
            val deps = enabledDependencies()
            val clock = ObservedClock(deps.clock)
            val observed =
                object : SettingsDependencies by deps {
                    override val clock = clock
                }
            withModel(observed) { vm ->
                assertEquals(0, clock.collections)
                assertEquals(0, clock.active)
                var subscription = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.meta.collect() }
                runCurrent()
                assertEquals(1, clock.collections)
                assertEquals(1, clock.active)
                subscription.cancelAndJoin()
                advanceTimeBy(4_999)
                runCurrent()
                assertEquals(1, clock.active)
                subscription = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.meta.collect() }
                runCurrent()
                assertEquals(1, clock.collections)
                subscription.cancelAndJoin()
                advanceTimeBy(5_000)
                runCurrent()
                assertEquals(0, clock.active)
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.meta.collect() }
                runCurrent()
                assertEquals(2, clock.collections)
                assertEquals(1, clock.active)
            }
        }

    @Test fun metadataRefreshesAtLocalMidnightWithoutReformattingWithinTheDay() =
        runTest(timeout = 60.seconds) {
            val server =
                PasskeyServer().apply {
                    passkeys = listOf(ApiPasskey(1, "Laptop", null, "2026-10-02T08:00:00Z", "2026-10-31T22:50:00Z"))
                }
            var formats = 0
            val deps =
                enabledDependencies().apply {
                    security = server::perform
                    zone = ZoneId.of("Europe/Amsterdam")
                    clock.advance(Instant.parse("2026-10-31T22:58:00Z"))
                    makeFormatter = {
                        formats++
                        CopyLineFormatter(SettingsFixture.catalog)
                    }
                }
            withModel(deps) { vm ->
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.meta.collect() }
                runCurrent()
                assertTrue(vm.meta.value.isEmpty())
                assertEquals(0, formats)
                vm.setLanguage(Language.En) { key, args -> key.replace("{{date}}", args["date"].orEmpty()) }
                runCurrent()
                assertEquals("Added 2 Oct 2026 · used today", vm.meta.value[1L])
                assertEquals(1, formats)
                deps.clock.advance(Instant.parse("2026-10-31T22:59:00Z"))
                runCurrent()
                assertEquals("Added 2 Oct 2026 · used today", vm.meta.value[1L])
                assertEquals(1, formats)
                deps.clock.advance(Instant.parse("2026-10-31T23:00:00Z"))
                runCurrent()
                assertEquals("Added 2 Oct 2026 · used 31 Oct", vm.meta.value[1L])
                assertEquals(2, formats)
                assertEquals(listOf(AccountSecurityChange.ListPasskeys), server.calls)
            }
        }

    @Test fun disabledCapabilityDoesNotStartPagesOrLoadPasskeys() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies(backgroundScope)
            var calls = 0
            deps.security = {
                calls++
                error("Capability is disabled")
            }
            withModel(deps) { vm ->
                vm.reload()
                vm.resume()
                runCurrent()
                assertFalse(vm.passkeysEnabled)
                assertNull(vm.ui.value.pages.account)
                assertFalse(vm.refreshing.value)
                assertEquals(0, calls)
            }
        }

    @Test fun loadingSkipsOverlappingReloadsAndClearsTheIndicator() =
        runTest(timeout = 60.seconds) {
            val deps = enabledDependencies()
            val returned = CompletableDeferred<AccountSecurityResult>()
            var calls = 0
            deps.security = {
                calls++
                returned.await()
            }
            withModel(deps) { vm ->
                assertTrue(vm.refreshing.value)
                vm.reload()
                vm.reload()
                runCurrent()
                assertEquals(1, calls)
                returned.complete(AccountSecurityResult.Passkeys(emptyList()))
                runCurrent()
                assertFalse(vm.refreshing.value)
                assertEquals(emptyList<Any>(), vm.ui.value.security.passkeys)
                vm.reload()
                runCurrent()
                assertEquals(2, calls)
            }
        }

    @Test fun resumeRetriesAnOfflineListButDoesNothingWhenOnline() =
        runTest(timeout = 60.seconds) {
            val server = PasskeyServer().apply { failure = DataFailure.Offline }
            val deps = enabledDependencies().apply { security = server::perform }
            withModel(deps) { vm ->
                assertTrue(vm.ui.value.security.offline)
                server.failure = null
                vm.resume()
                runCurrent()
                assertFalse(vm.ui.value.security.offline)
                assertEquals(server.passkeys, vm.ui.value.security.passkeys)
                assertEquals(2, server.calls.size)
                vm.resume()
                runCurrent()
                assertEquals(2, server.calls.size)
            }
        }

    @Test fun busySubmissionIsSkippedAndSuccessFeedbackWaitsForAcknowledgement() =
        runTest(timeout = 60.seconds) {
            val server = PasskeyServer()
            val returned = CompletableDeferred<Unit>()
            val deps =
                enabledDependencies().apply {
                    security = { change ->
                        if (change is AccountSecurityChange.RemovePasskey) returned.await()
                        server.perform(change)
                    }
                }
            withModel(deps) { vm ->
                vm.event(PasskeysEvent.Remove(server.passkeys.first()))
                val sent = FakeSecretField("pw")
                val skipped = FakeSecretField("pw")
                vm.event(PasskeysEvent.Submit(sent))
                runCurrent()
                assertTrue(vm.ui.value.security.busy)
                vm.event(PasskeysEvent.Submit(skipped))
                runCurrent()
                assertTrue(sent.cleared)
                assertFalse(skipped.cleared)
                assertNull(vm.ui.value.feedback)
                returned.complete(Unit)
                runCurrent()
                assertEquals(1, server.calls.filterIsInstance<AccountSecurityChange.RemovePasskey>().size)
                assertFalse(vm.ui.value.security.busy)
                assertNull(vm.ui.value.security.prompt)
                val feedback = checkNotNull(vm.ui.value.feedback)
                assertEquals("Passkey removed", feedback.key)
                vm.feedbackPresented(feedback.id - 1)
                runCurrent()
                assertNotNull(vm.ui.value.feedback)
                vm.feedbackPresented(feedback.id)
                runCurrent()
                assertNull(vm.ui.value.feedback)
            }
        }

    @Test fun dependencyFailureIsMappedAndStaleSessionStaysSilent() =
        runTest(timeout = 60.seconds) {
            val deps = enabledDependencies().apply { security = PasskeyServer()::perform }
            withModel(deps) { vm ->
                assertEquals(
                    deps.account.value?.email,
                    vm.ui.value.pages.account
                        ?.email,
                )
                deps.security = { throw java.io.IOException() }
                vm.event(PasskeysEvent.Add)
                vm.event(PasskeysEvent.Submit(FakeSecretField("pw")))
                runCurrent()
                assertEquals(
                    "We couldn't register that passkey. Please try again.",
                    vm.ui.value.feedback
                        ?.key,
                )
                vm.clearSecrets()
                deps.security = { throw kotlinx.coroutines.CancellationException() }
                vm.event(PasskeysEvent.Add)
                vm.event(PasskeysEvent.Submit(FakeSecretField("pw")))
                runCurrent()
                assertFalse(vm.ui.value.security.busy)
                assertNull(vm.ui.value.security.message)
                assertNull(vm.ui.value.feedback)
            }
        }

    private fun TestScope.enabledDependencies() =
        FakeSettingsDependencies(backgroundScope).apply {
            passkeyCapability = PasskeyCapability(true)
            passkeyRegistration = DeterministicRegistrationProvider()
        }

    private suspend fun TestScope.withModel(
        deps: SettingsDependencies,
        block: suspend (PasskeysViewModel) -> Unit,
    ) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val vm = PasskeysViewModel(deps, dispatcher, dispatcher)
        val store = ViewModelStore().apply { put("passkeys", vm) }
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

    private class ObservedClock(
        private val source: FakePlanClock,
    ) : PlanClock {
        var collections = 0
        var active = 0
        override val now get() = source.now

        override fun values() =
            flow {
                collections++
                active++
                try {
                    emitAll(source.values())
                } finally {
                    active--
                }
            }
    }
}
