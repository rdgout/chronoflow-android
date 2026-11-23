package net.chronoflow.feature.settings.preferences

import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings

sealed interface RemindersSubtitle {
    data class On(
        val minutes: Int,
    ) : RemindersSubtitle

    data object Off : RemindersSubtitle
}

fun remindersSubtitle(
    reminders: ReminderSettings?,
    permission: ReminderPermission?,
): RemindersSubtitle? =
    when {
        reminders == null || permission == null -> null
        permission.remindersActive(reminders) -> RemindersSubtitle.On(reminders.leadMin)
        else -> RemindersSubtitle.Off
    }
