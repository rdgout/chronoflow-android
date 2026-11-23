package net.chronoflow.debug.reminders

import kotlinx.coroutines.test.runTest
import net.chronoflow.core.reminders.ReminderPermission
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.time.Duration.Companion.seconds

@RunWith(Parameterized::class)
class ReminderPermissionTests(
    private val permission: ReminderPermission,
) {
    @Test fun mockAuthorizationChangesOnlyAfterAnExplicitRequest() =
        runTest(timeout = 60.seconds) {
            val center = MockReminderCenter(permission)
            assertEquals(permission, center.permission())
            assertEquals(0, center.permissionRequestCount)
            assertEquals(
                if (permission ==
                    ReminderPermission.Undetermined
                ) {
                    ReminderPermission.Granted
                } else {
                    permission
                },
                center.requestPermission(),
            )
            assertEquals(1, center.permissionRequestCount)
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "permission={0}")
        fun cases(): List<Array<Any>> = ReminderPermission.entries.map { arrayOf(it) }
    }
}
