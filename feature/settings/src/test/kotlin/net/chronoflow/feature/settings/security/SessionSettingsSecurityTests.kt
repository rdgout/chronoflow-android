@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.security

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.feature.settings.support.SettingsFixtureTest
import net.chronoflow.feature.settings.support.withSettingsFixture
import net.chronoflow.feature.settings.testing.FakeSecretField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SessionSettingsSecurityTests : SettingsFixtureTest() {
    @Test fun disposingTheEntryStillAppliesConfirmationAndRequestsSync() = finishConfirmationAfterEntryDisposal(false)

    @Test fun cancellingTheSessionStopsConfirmationBeforeStoreApplyAndSync() =
        finishConfirmationAfterEntryDisposal(
            true,
        )

    private fun finishConfirmationAfterEntryDisposal(cancelSession: Boolean) =
        runTest(timeout = 60.seconds) {
            withSettingsFixture(mutationsOffline = false) { f ->
                val entered = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                f.syncSource =
                    object : DataSource by f.source {
                        override suspend fun accountSecurity(change: AccountSecurityChange): AccountSecurityResult {
                            if (change is AccountSecurityChange.ConfirmTwoFactor) {
                                entered.complete(Unit)
                                release.await()
                            }
                            return f.source.accountSecurity(change)
                        }
                    }
                val vm = SecurityViewModel(f.deps, f.io, f.io, f.io)
                f.store.put("security", vm)
                f.viewModels += vm
                runCurrent()
                vm.showPrompt(SettingsSecurityPrompt.Enable)
                vm.submitPrompt(FakeSecretField("password"))
                runCurrent()
                assertNotNull(vm.state.value.setup)
                val syncRequests = f.securitySyncRequests
                assertEquals(false, SecuritySummary.from(f.settings.settings())?.twoFactorEnabled)

                vm.confirm(FakeSecretField("123456"))
                entered.await()
                f.store.clear()
                vm.viewModelScope.coroutineContext.job
                    .cancelAndJoin()
                assertTrue(vm.viewModelScope.coroutineContext.job.isCancelled)
                if (cancelSession) {
                    f.securityScope.coroutineContext.job
                        .cancelAndJoin()
                }
                release.complete(Unit)
                runCurrent()

                assertEquals(!cancelSession, SecuritySummary.from(f.settings.settings())?.twoFactorEnabled)
                assertEquals(syncRequests + if (cancelSession) 0 else 1, f.securitySyncRequests)
                assertNull(vm.state.value.setup)
                assertTrue(
                    vm.state.value.codes
                        .isEmpty(),
                )
                assertNull(vm.state.value.prompt)
                assertNull(vm.state.value.message)
                assertFalse(vm.state.value.busy)
            }
        }

    @Test fun clearingSecretsDuringConfirmationStillAppliesTheStoreAndDiscardsLateUiResults() =
        runTest(timeout = 60.seconds) {
            withSettingsFixture(mutationsOffline = false) { f ->
                val entered = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                f.syncSource =
                    object : DataSource by f.source {
                        override suspend fun accountSecurity(change: AccountSecurityChange): AccountSecurityResult {
                            if (change is AccountSecurityChange.ConfirmTwoFactor) {
                                entered.complete(Unit)
                                release.await()
                            }
                            return f.source.accountSecurity(change)
                        }
                    }
                val dependencies = f.deps
                var confirmationCompleted = false
                val observed =
                    object : net.chronoflow.feature.settings.SettingsDependencies by dependencies {
                        override suspend fun performSecurity(change: AccountSecurityChange): AccountSecurityResult {
                            val result = dependencies.performSecurity(change)
                            if (change is AccountSecurityChange.ConfirmTwoFactor) confirmationCompleted = true
                            return result
                        }
                    }
                val vm = SecurityViewModel(observed, f.io, f.io, f.io)
                f.store.put("security", vm)
                f.viewModels += vm
                runCurrent()
                vm.showPrompt(SettingsSecurityPrompt.Enable)
                vm.submitPrompt(FakeSecretField("password"))
                runCurrent()
                assertNotNull(vm.state.value.setup)
                assertEquals(false, SecuritySummary.from(f.settings.settings())?.twoFactorEnabled)

                vm.confirm(FakeSecretField("123456"))
                entered.await()
                vm.clearSecrets()
                release.complete(Unit)
                runCurrent()

                assertTrue("performSecurity must finish after its store apply and sync request", confirmationCompleted)
                assertEquals(true, SecuritySummary.from(f.settings.settings())?.twoFactorEnabled)
                assertNull(vm.state.value.setup)
                assertTrue(
                    vm.state.value.codes
                        .isEmpty(),
                )
                assertNull(vm.state.value.prompt)
                assertNull(vm.state.value.message)
                assertNull(vm.ui.value.feedback)
                assertFalse(vm.state.value.busy)
            }
        }

    @Test fun disableSnapshotClearsSecretsAndObservedEnabledStateForIntroduction() = disable(retainSetup = true)

    @Test fun disableSnapshotClearsSecretsAndObservedEnabledStateForIntroductionAfterConfirmation() =
        disable(retainSetup = false)

    private fun disable(retainSetup: Boolean) =
        runTest(timeout = 60.seconds) {
            withSettingsFixture(mutationsOffline = false) { f ->
                val changes = mutableListOf<AccountSecurityChange>()
                f.syncSource =
                    object : DataSource by f.source {
                        override suspend fun accountSecurity(change: AccountSecurityChange): AccountSecurityResult {
                            changes += change
                            return f.source.accountSecurity(change)
                        }
                    }
                val pages =
                    net.chronoflow.feature.settings
                        .SettingsPagesModel(f.deps, backgroundScope)
                pages.start()
                runCurrent()
                val model = SettingsSecurityModel(f.deps::performSecurity) { f.clock.now }
                model.showPrompt(SettingsSecurityPrompt.Enable)
                model.submitPrompt(FakeSecretField("password"))
                assertNotNull(model.state.value.setup)
                if (!retainSetup) {
                    model.confirm(FakeSecretField("123456"))
                    assertTrue(
                        model.state.value.codes
                            .isNotEmpty(),
                    )
                }
                val cached = f.settings.settings().first { it.section == "security" }
                val value =
                    net.chronoflow.core.model.api.ApiJson.response
                        .parseToJsonElement(cached.value)
                        as kotlinx.serialization.json.JsonObject
                f.settings.upsertSecurity(
                    kotlinx.serialization.json.JsonObject(
                        value + ("twoFactorEnabled" to kotlinx.serialization.json.JsonPrimitive(true)),
                    ),
                    f.permit,
                )
                runCurrent()
                assertEquals(true, SecuritySummary.from(pages.ui.value.settings)?.twoFactorEnabled)
                model.showPrompt(SettingsSecurityPrompt.Disable)
                model.submitPrompt(FakeSecretField("password"))
                runCurrent()
                assertEquals(AccountSecurityChange.DisableTwoFactor("password"), changes.last())
                assertNull(model.state.value.prompt)
                assertFalse(model.state.value.busy)
                assertNull(model.state.value.setup)
                assertTrue(
                    model.state.value.codes
                        .isEmpty(),
                )
                assertNull(model.state.value.message)
                assertTrue(
                    model.state.value.errors
                        .isEmpty(),
                )
                val summary = SecuritySummary.from(f.settings.settings())
                assertEquals(false, summary?.twoFactorEnabled)
                assertEquals(false, SecuritySummary.from(pages.ui.value.settings)?.twoFactorEnabled)
                pages.stop()
            }
        }

    @Test fun staleSessionThrowsStaleSessionException() =
        runTest(timeout = 60.seconds) {
            withSettingsFixture(mutationsOffline = false) { f ->
                val entered = CompletableDeferred<Unit>()
                val response = CompletableDeferred<AccountSecurityResult>()
                f.syncSource =
                    object : DataSource by f.source {
                        override suspend fun accountSecurity(change: AccountSecurityChange): AccountSecurityResult {
                            entered.complete(Unit)
                            return response.await()
                        }
                    }
                val request =
                    async {
                        try {
                            f.deps.performSecurity(AccountSecurityChange.EnableTwoFactor("password"))
                            false
                        } catch (_: StaleSessionException) {
                            true
                        }
                    }
                entered.await()
                f.controller.signOut(f.snapshot, discardPending = true)
                response.complete(AccountSecurityResult.Changed)
                assertTrue(request.await())
            }
        }
}
