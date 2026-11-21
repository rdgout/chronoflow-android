@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.sync.FullPull
import net.chronoflow.core.sync.OutboxFlusher
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import net.chronoflow.feature.settings.support.SettingsFixtureTest
import net.chronoflow.feature.settings.support.settingsNow
import net.chronoflow.feature.settings.support.withSettingsFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class SettingsProfileTests : SettingsFixtureTest() {
    @Test fun validatesTrimsAndPreservesEditedDraftDuringPulls() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies()
            withProfile(deps) { vm ->
                assertEquals(
                    deps.account.value?.name,
                    vm.ui.value.pages.account
                        ?.name,
                )
                assertEquals(
                    deps.account.value?.email,
                    vm.ui.value.pages.account
                        ?.email,
                )
                vm.edit(" \n ")
                vm.submit()
                runCurrent()
                assertEquals(
                    ProfileError.EnterName,
                    vm.ui.value.editor
                        ?.error,
                )
                assertEquals(0, deps.saves)
                vm.edit("  My draft  ")
                deps.account.value = deps.account.value!!.copy(name = "Other device")
                runCurrent()
                assertEquals(
                    "  My draft  ",
                    vm.ui.value.editor
                        ?.name,
                )
                vm.submit()
                runCurrent()
                assertEquals(
                    "My draft",
                    vm.ui.value.editor
                        ?.name,
                )
                assertEquals(
                    "rick@example.com",
                    (OutboxPayload.change(deps.rows.value.single()) as OutboxChange.Profile).change.email,
                )
                assertTrue(
                    vm.ui.value.editor!!
                        .saved,
                )
                assertNull(
                    vm.ui.value.editor
                        ?.error,
                )
                deps.account.value = deps.account.value!!.copy(name = "Other device")
                runCurrent()
                assertFalse(
                    vm.ui.value.editor!!
                        .saved,
                )
                assertEquals(
                    "Other device",
                    vm.ui.value.editor
                        ?.name,
                )
            }
        }

    @Test fun sessionUpsertPreservesPendingProfileAcrossRelaunch() =
        runTest(timeout = 60.seconds) {
            withSettingsFixture { f ->
                val vm = f.profile()
                runCurrent()
                vm.edit("Pending")
                vm.submit()
                runCurrent()
                assertTrue(
                    vm.ui.value.editor!!
                        .saved,
                )
                f.reopen()
                FullPull(f.sql, f.source, now = { settingsNow }).run(f.epoch)
                assertEquals(
                    "Pending",
                    f.account
                        .values()
                        .first()
                        ?.name,
                )
                assertEquals(
                    "Pending",
                    (OutboxPayload.change(f.outbox.pending().single()) as OutboxChange.Profile).change.name,
                )
            }
        }

    @Test fun storageFailureKeepsDraftForRetry() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies().apply { save = { _, _ -> throw StoreFailure.Closed } }
            withProfile(deps) { vm ->
                vm.edit("Edited")
                vm.submit()
                runCurrent()
                assertEquals(
                    "Edited",
                    vm.ui.value.editor
                        ?.name,
                )
                assertEquals(
                    ProfileError.CouldNotSave,
                    vm.ui.value.editor
                        ?.error,
                )
                assertFalse(
                    vm.ui.value.editor!!
                        .busy,
                )
                assertFalse(
                    vm.ui.value.editor!!
                        .saved,
                )
                deps.save = { _, _ -> }
                vm.submit()
                runCurrent()
                assertTrue(
                    vm.ui.value.editor!!
                        .saved,
                )
            }
        }

    @Test fun offlineProfileIsDurablePendingPullCannotOverwriteAndRevokedPermitCannotWrite() =
        runTest(timeout = 60.seconds) {
            withSettingsFixture { f ->
                val vm = f.profile()
                runCurrent()
                vm.edit("Saved offline")
                vm.submit()
                runCurrent()
                assertTrue(
                    vm.ui.value.editor!!
                        .saved,
                )
                assertTrue(OutboxFlusher(f.sql, f.source, now = { settingsNow }).run(f.epoch).offline)
                FullPull(f.sql, f.source, now = { settingsNow }).run(f.epoch)
                assertEquals(
                    "Saved offline",
                    f.account
                        .values()
                        .first()
                        ?.name,
                )
                f.permit.invalidate()
                vm.edit("Stale session")
                vm.submit()
                runCurrent()
                assertFalse(
                    vm.ui.value.editor!!
                        .saved,
                )
                assertNull(
                    vm.ui.value.editor
                        ?.error,
                )
                assertEquals(
                    "Saved offline",
                    f.account
                        .values()
                        .first()
                        ?.name,
                )
                assertEquals(1, f.outbox.pending().size)
            }
        }

    @Test fun nameOf256CharsIsRejected() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies()
            withProfile(deps) { vm ->
                vm.edit("a".repeat(256))
                vm.submit()
                runCurrent()
                assertEquals(
                    ProfileError.EnterName,
                    vm.ui.value.editor
                        ?.error,
                )
                assertEquals(0, deps.saves)
            }
        }

    @Test fun notCurrentSessionDoesNotSaveOrShowAnError() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies()
            withProfile(deps) { vm ->
                deps.current = false
                vm.edit("Edited")
                vm.submit()
                runCurrent()
                assertEquals(0, deps.saves)
                assertEquals(
                    "Edited",
                    vm.ui.value.editor
                        ?.name,
                )
                assertNull(
                    vm.ui.value.editor
                        ?.error,
                )
                assertFalse(
                    vm.ui.value.editor!!
                        .busy,
                )
                assertFalse(
                    vm.ui.value.editor!!
                        .saved,
                )
            }
        }

    @Test fun onlyPullsAndCanonicalNamesReplaceTheField() =
        runTest(timeout = 60.seconds) {
            val account = FakeSettingsDependencies().account.value!!
            val model = SettingsProfileModel(account, save = {})
            model.receive(account.copy(name = "Pulled"))
            val pulled = checkNotNull(model.ui.value.nameReplacement)
            assertEquals(account.name, pulled.previous)
            assertEquals("Pulled", pulled.value)
            model.edit("  Edited  ")
            assertSame(pulled, model.ui.value.nameReplacement)
            model.receive(account.copy(name = "Another pull"))
            assertSame(pulled, model.ui.value.nameReplacement)
            assertEquals("  Edited  ", model.ui.value.name)
            model.submit()
            val canonical = checkNotNull(model.ui.value.nameReplacement)
            assertEquals(pulled.id + 1, canonical.id)
            assertEquals("  Edited  ", canonical.previous)
            assertEquals("Edited", canonical.value)
            model.edit("Next draft")
            assertSame(canonical, model.ui.value.nameReplacement)
        }

    @Test fun limitCountsCodePointsRatherThanUtf16Units() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies()
            withProfile(deps) { vm ->
                vm.edit("🌙".repeat(255))
                vm.submit()
                runCurrent()
                assertTrue(
                    vm.ui.value.editor!!
                        .saved,
                )
                assertEquals(1, deps.saves)
            }
        }
}

internal suspend fun TestScope.withProfile(
    deps: FakeSettingsDependencies,
    block: suspend (ProfileViewModel) -> Unit,
) {
    val vm = ProfileViewModel(deps, SavedStateHandle(), StandardTestDispatcher(testScheduler))
    val store = ViewModelStore().apply { put("profile", vm) }
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
