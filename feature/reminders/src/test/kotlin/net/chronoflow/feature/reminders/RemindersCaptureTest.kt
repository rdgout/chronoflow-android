package net.chronoflow.feature.reminders

import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings
import net.chronoflow.core.testing.compose.BoardCapture
import net.chronoflow.core.testing.compose.BoardCaptureCell
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
    private val cell: BoardCaptureCell,
) : RemindersScreenTest(cell.profile, cell.language, cell.dark) {
    @Test fun capture() {
        show {
            val state = RemindersUiState(ReminderSettings(), permission)
            if (board.startsWith("Wizard")) RemindersSheetFixture(state) else RemindersScreen(state, {})
        }
        BoardCapture.capture(compose, "${board}_${cell.suffix}")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-{1}-{2}")
        fun cases(): List<Array<Any>> =
            listOf(
                "SetReminders_on" to ReminderPermission.Granted,
                "SetReminders_unsupported" to ReminderPermission.Unsupported,
                "SetRemindersAsk" to ReminderPermission.Undetermined,
                "SetRemindersBlocked" to ReminderPermission.Blocked,
                "WizardReminders" to ReminderPermission.Undetermined,
            ).flatMap { (board, permission) ->
                BoardCaptureCell.standard.map { cell -> arrayOf<Any>(board, permission, cell) }
            }
    }
}
