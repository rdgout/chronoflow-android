package net.chronoflow.feature.settings.preferences

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.settings.bodyclock.json
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PreferencesRemindersRowTest(
    private val permission: ReminderPermission,
    language: AppLanguage,
) : SettingsScreenTest(language = language) {
    @Test fun knownReminderStateAddsTheSubtitleToTheDestinationRow() {
        show {
            PreferencesScreen(
                PreferencesUiState(
                    editor = SettingsPreferencesModel.State(json("{}")),
                    reminders = ReminderSettings(leadMin = 15),
                    reminderPermission = permission,
                ),
                {
                },
            )
        }
        compose.onNodeWithTag("settings.preferences.reminders").performScrollTo()
        compose.onNodeWithTag("settings.preferences.reminders.subtitle", true).assertExists()
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "permission={0}-language={1}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf(ReminderPermission.Granted, AppLanguage.Nl),
                arrayOf(ReminderPermission.Blocked, AppLanguage.En),
            )
    }
}
