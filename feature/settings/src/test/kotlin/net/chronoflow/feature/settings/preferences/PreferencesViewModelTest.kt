@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.feature.settings.preferences

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
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.config.ThemeChoice
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.navigation.Route
import net.chronoflow.feature.settings.bodyclock.json
import net.chronoflow.feature.settings.support.FakeSettingsDependencies
import net.chronoflow.feature.settings.support.FakeThemeDependencies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.seconds

class PreferencesViewModelTest {
    @Test fun travelToggleSavesTheCompleteGroupAndOnlyItsChangedKey() =
        runTest(timeout = 60.seconds) {
            val deps = preferencesDeps()
            withPreferences(deps) { vm ->
                vm.setShown(false, SettingsTimelineGroup.all[3])
                runCurrent()
                val change = OutboxPayload.change(deps.rows.value.single()) as OutboxChange.Settings
                assertEquals(SettingsSection.Preferences, change.section)
                assertEquals(
                    json("""{"hidden_categories":["flight","layover","transfer","movement_break","zone_change"]}"""),
                    change.patch,
                )
                assertTrue(
                    vm.ui.value.editor!!
                        .saved,
                )
                assertEquals(0, deps.requests)
            }
        }

    @Test fun timeFormatSavesOnlyItsChangedKey() =
        runTest(timeout = 60.seconds) {
            val deps = preferencesDeps()
            withPreferences(deps) { vm ->
                vm.setTimeFormat("12")
                runCurrent()
                assertEquals(
                    json("""{"time_format":"12"}"""),
                    (OutboxPayload.change(deps.rows.value.single()) as OutboxChange.Settings).patch,
                )
            }
        }

    @Test fun themeIsDeviceOnly() =
        runTest(timeout = 60.seconds) {
            val deps = preferencesDeps()
            val theme = FakeThemeDependencies()
            withPreferences(deps, theme) { vm ->
                vm.setTheme("dark")
                runCurrent()
                assertEquals(listOf(ThemeChoice.Dark), theme.choices)
                assertEquals(ThemeChoice.Dark, vm.ui.value.theme)
                assertTrue(deps.rows.value.isEmpty())
            }
        }

    @Test fun incomingOldValueDuringPendingSaveKeepsTheEdit() =
        runTest(timeout = 60.seconds) {
            val deps = preferencesDeps()
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val save = deps.saveSettings
            deps.saveSettings = { section, patch ->
                entered.complete(Unit)
                release.await()
                save(section, patch)
            }
            withPreferences(deps) { vm ->
                vm.setTimeFormat("12")
                entered.await()
                deps.settings.value = listOf(SettingsRecord("preferences", """{"timeFormat":"24","server":true}"""))
                runCurrent()
                assertEquals(
                    "12",
                    vm.ui.value.editor!!
                        .timeFormat,
                )
                release.complete(Unit)
                runCurrent()
                assertTrue(
                    vm.ui.value.editor!!
                        .saved,
                )
            }
        }

    @Test fun failedLocalSaveShowsFailure() =
        runTest(timeout = 60.seconds) {
            val deps = preferencesDeps().apply { saveSettings = { _, _ -> throw StoreFailure.Closed } }
            withPreferences(deps) { vm ->
                vm.setTimeFormat("12")
                runCurrent()
                assertTrue(vm.ui.value.failure)
                assertEquals(
                    "12",
                    vm.ui.value.editor!!
                        .timeFormat,
                )
            }
        }

    @Test fun themeFailureShowsFailure() =
        runTest(timeout = 60.seconds) {
            val theme = FakeThemeDependencies().apply { failure = IOException() }
            withPreferences(preferencesDeps(), theme) { vm ->
                vm.setTheme("dark")
                runCurrent()
                assertTrue(vm.ui.value.failure)
                theme.failure = null
                vm.setTheme("dark")
                runCurrent()
                assertFalse(vm.ui.value.failure)
            }
        }

    @Test fun staleSessionEnqueuesNothingAndFailureStaysFalse() =
        runTest(timeout = 60.seconds) {
            val deps = preferencesDeps().apply { current = false }
            withPreferences(deps) { vm ->
                vm.setTimeFormat("12")
                runCurrent()
                assertTrue(deps.rows.value.isEmpty())
                assertFalse(vm.ui.value.failure)
                assertFalse(
                    vm.ui.value.editor!!
                        .saved,
                )
            }
        }

    @Test fun replayTwiceStartsFreshAfterSkipOrDone() =
        runTest(timeout = 60.seconds) {
            val deps = preferencesDeps()
            withPreferences(deps) { vm ->
                vm.setTimeFormat("12")
                runCurrent()
                assertEquals(listOf("settings"), deps.rows.value.map { it.kind })
                val change = OutboxPayload.change(deps.rows.value.single()) as OutboxChange.Settings
                assertEquals(SettingsSection.Preferences, change.section)
                assertEquals(Route.Walkthrough(replay = true), PreferencesEvent.Replay.destination())
                assertEquals(Route.Reminders, PreferencesEvent.Reminders.destination())
            }
        }

    @Test fun missingOrMalformedSectionUsesTheStoredStateUntilAnObjectArrives() =
        runTest(timeout = 60.seconds) {
            val deps = FakeSettingsDependencies()
            withPreferences(deps) { vm ->
                assertTrue(vm.ui.value.pages.settingsLoaded)
                assertNull(vm.ui.value.editor)
                deps.settings.value = listOf(SettingsRecord("preferences", "[1]"))
                runCurrent()
                assertNull(vm.ui.value.editor)
                deps.settings.value = listOf(SettingsRecord("preferences", "{}"))
                runCurrent()
                assertEquals(
                    "24",
                    vm.ui.value.editor!!
                        .timeFormat,
                )
            }
        }
}

private fun preferencesDeps() =
    FakeSettingsDependencies().apply {
        settings.value = listOf(SettingsRecord("preferences", """{"timeFormat":"24","hiddenCategories":[]}"""))
    }

internal suspend fun TestScope.withPreferences(
    deps: FakeSettingsDependencies,
    theme: FakeThemeDependencies = FakeThemeDependencies(),
    block: suspend (PreferencesViewModel) -> Unit,
) {
    val vm = PreferencesViewModel(deps, theme, StandardTestDispatcher(testScheduler))
    val store = ViewModelStore().apply { put("preferences", vm) }
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
