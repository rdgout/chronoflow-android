package net.chronoflow.core.data.reminders

import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.reminders.ReminderRequest

class ReminderAlarmRefused(
    cause: Throwable?,
) : Exception(null, cause)

interface ReminderAlarms {
    suspend fun set(
        request: ReminderRequest,
        requestCode: Int,
    )

    suspend fun cancel(
        identifier: String,
        requestCode: Int,
    )
}

interface ReminderNotices {
    suspend fun ensureChannel(language: Language)

    suspend fun removeDelivered()
}
