package net.chronoflow.core.data.reminders

import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderPermission.Blocked
import net.chronoflow.core.reminders.ReminderPermission.Granted
import net.chronoflow.core.reminders.ReminderPermission.Undetermined
import net.chronoflow.core.reminders.ReminderPermission.Unsupported
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ReminderPermissionMappingTest(
    private val sdk: Int,
    private val available: Boolean,
    private val runtime: Boolean,
    private val requested: Boolean,
    private val enabled: Boolean,
    private val blocked: Boolean,
    private val expected: ReminderPermission,
) {
    @Test fun nativeAuthorizationMatrix() {
        assertEquals(expected, reminderPermission(sdk, available, runtime, requested, enabled, blocked))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: sdk={0}, requested={3}, result={6}")
        fun rows(): List<Array<Any>> =
            listOf(
                arrayOf(36, false, true, false, true, false, Unsupported),
                arrayOf(36, true, false, false, true, false, Undetermined),
                arrayOf(36, true, false, true, true, false, Blocked),
                arrayOf(36, true, true, true, true, false, Granted),
                arrayOf(36, true, true, false, false, false, Blocked),
                arrayOf(36, true, true, false, true, true, Blocked),
                arrayOf(33, true, false, false, false, false, Undetermined),
                arrayOf(32, true, false, true, true, false, Granted),
                arrayOf(29, true, false, true, false, false, Blocked),
                arrayOf(29, true, true, true, true, true, Blocked),
                arrayOf(32, true, false, false, true, false, Undetermined),
                arrayOf(29, true, false, false, false, false, Undetermined),
            )
    }
}
