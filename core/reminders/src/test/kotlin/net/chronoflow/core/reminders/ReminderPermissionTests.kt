package net.chronoflow.core.reminders

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import org.junit.Test

class ReminderPermissionTests {
    @Test fun permissionMatrix() {
        for (permission in ReminderPermission.entries) {
            assertThat(
                permission.remindersActive(ReminderSettings()),
            ).isEqualTo(permission == ReminderPermission.Granted)
            assertThat(permission.remindersActive(null)).isFalse()
            assertThat(permission.remindersActive(ReminderSettings().settingEnabled(false))).isFalse()
        }
    }
}
