package net.chronoflow.app.reminders

import net.chronoflow.core.data.session.SessionHooks

class ReminderSessionHooks(
    private val reschedule: suspend () -> Unit,
    private val detach: suspend () -> Unit,
) : SessionHooks {
    override suspend fun onForeground() = reschedule()

    override suspend fun onSessionEnding() = detach()
}
