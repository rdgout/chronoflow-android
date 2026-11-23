package net.chronoflow.feature.settings.preferences

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.SettingsPagesState
import net.chronoflow.feature.settings.bodyclock.json
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PreferencesScreenTest : SettingsScreenTest() {
    private fun state(failure: Boolean = false) =
        PreferencesUiState(
            editor = SettingsPreferencesModel.State(json("{}"), failure = failure),
        )

    @Test fun exposesChoicesAndCategorySwitchesByTag() {
        show { PreferencesScreen(state(), {}) }
        compose.onNodeWithTag("settings.preferences.screen").assertExists()
        compose.onNodeWithTag("settings.preferences.theme.system").assertExists()
        compose.onNodeWithTag("settings.preferences.theme.light").assertExists()
        compose.onNodeWithTag("settings.preferences.theme.dark").assertExists()
        compose.onNodeWithTag("settings.preferences.timeFormat.24").assertExists()
        compose.onNodeWithTag("settings.preferences.timeFormat.12").assertExists()
        compose.onNodeWithTag("settings.preferences.Sleep").assertExists()
        compose.onNodeWithTag("settings.preferences.Light").assertExists()
        compose.onNodeWithTag("settings.preferences.Caffeine").assertExists()
        compose.onNodeWithTag("settings.preferences.Travel").assertExists()
    }

    @Test fun linksEmitDistinctEventsAndRemindersHasNoSubtitle() {
        val events = mutableListOf<PreferencesEvent>()
        show { PreferencesScreen(state(), { events += it }) }
        compose.onNodeWithTag("settings.preferences.reminders").performScrollTo().performClick()
        compose.onNodeWithTag("settings.preferences.replay").performScrollTo().performClick()
        assertEquals(listOf(PreferencesEvent.Reminders, PreferencesEvent.Replay), events)
        compose.onNodeWithTag("settings.preferences.reminders.subtitle", true).assertDoesNotExist()
    }

    @Test fun failureShowsTheErrorLine() {
        show { PreferencesScreen(state(failure = true), {}) }
        compose.onNodeWithTag("settings.preferences.failure", true).assertExists()
    }

    @Test fun noSectionShowsStoredState() {
        show { PreferencesScreen(PreferencesUiState(pages = SettingsPagesState(settingsLoaded = true)), {}) }
        compose.onNodeWithTag("settings.missing").assertExists()
        compose.onNodeWithTag("settings.preferences.theme.system").assertDoesNotExist()
    }
}
