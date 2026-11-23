package net.chronoflow.core.data.reminders

import net.chronoflow.core.model.contracts.Language

class FakeReminderNotices : ReminderNotices {
    var channels = 0
    var removed = 0

    override suspend fun ensureChannel(language: Language) {
        channels++
    }

    override suspend fun removeDelivered() {
        removed++
    }
}
