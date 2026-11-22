package net.chronoflow.app.reminders

import net.chronoflow.core.navigation.LaunchIntentHandler
import net.chronoflow.core.navigation.PendingLaunchIntent
import net.chronoflow.core.navigation.ReminderTapRouter

class ReminderTapDispatcher(
    private val router: ReminderTapRouter,
) : LaunchIntentHandler {
    override suspend fun handle(intent: PendingLaunchIntent) {
        intent.reminder?.let(router::receive)
    }
}
