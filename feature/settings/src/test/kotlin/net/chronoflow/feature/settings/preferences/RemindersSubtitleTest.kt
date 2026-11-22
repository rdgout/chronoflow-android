package net.chronoflow.feature.settings.preferences

import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class RemindersSubtitleTest(
    private val name: String,
    private val settings: ReminderSettings?,
    private val permission: ReminderPermission?,
    private val expected: RemindersSubtitle?,
) {
    @Test fun subtitleReflectsPermissionAndStoredSettings() {
        assertEquals(expected, remindersSubtitle(settings, permission))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any?>> =
            listOf(
                arrayOf(
                    "no-settings",
                    null,
                    ReminderPermission.Granted,
                    null,
                ),
                arrayOf(
                    "no-permission",
                    ReminderSettings(),
                    null,
                    null,
                ),
                arrayOf(
                    "on-default",
                    ReminderSettings(),
                    ReminderPermission.Granted,
                    RemindersSubtitle.On(30),
                ),
                arrayOf(
                    "on-sixty",
                    ReminderSettings(leadMin = 60),
                    ReminderPermission.Granted,
                    RemindersSubtitle.On(60),
                ),
                arrayOf(
                    "off",
                    ReminderSettings().settingEnabled(false),
                    ReminderPermission.Granted,
                    RemindersSubtitle.Off,
                ),
                arrayOf(
                    "blocked",
                    ReminderSettings(),
                    ReminderPermission.Blocked,
                    RemindersSubtitle.Off,
                ),
                arrayOf(
                    "undetermined",
                    ReminderSettings(),
                    ReminderPermission.Undetermined,
                    RemindersSubtitle.Off,
                ),
                arrayOf(
                    "unsupported",
                    ReminderSettings(),
                    ReminderPermission.Unsupported,
                    RemindersSubtitle.Off,
                ),
            )
    }
}
