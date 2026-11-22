package net.chronoflow.feature.reminders

import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.tiers.Capture
import net.chronoflow.feature.reminders.support.RemindersScreenTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Capture
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RemindersCaptureTest(
    private val board: String,
    private val permission: ReminderPermission,
    private val language: AppLanguage,
    dark: Boolean,
) : RemindersScreenTest(DeviceProfiles.pixel8, language, dark) {
    @Test fun capture() {
        show {
            val state = RemindersUiState(ReminderSettings(), permission)
            if (board.startsWith("Wizard")) RemindersSheetFixture(state) else RemindersScreen(state, {})
        }
        BoardCapture.capture(compose, "${board}_pixel8_${language.tag}_${if (dark) "dark" else "light"}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "board={0}-language={2}-dark={3}")
        fun cases(): List<Array<Any>> =
            listOf(
                arrayOf("SetReminders_on", ReminderPermission.Granted, AppLanguage.En, false),
                arrayOf("SetReminders_unsupported", ReminderPermission.Unsupported, AppLanguage.En, false),
                arrayOf("SetRemindersAsk", ReminderPermission.Undetermined, AppLanguage.En, false),
                arrayOf("SetRemindersBlocked", ReminderPermission.Blocked, AppLanguage.En, false),
                arrayOf("DarkSetReminders", ReminderPermission.Granted, AppLanguage.En, true),
                arrayOf("WizardReminders", ReminderPermission.Undetermined, AppLanguage.En, false),
                arrayOf("WizardRemindersDarkNL", ReminderPermission.Undetermined, AppLanguage.Nl, true),
            )
    }
}
