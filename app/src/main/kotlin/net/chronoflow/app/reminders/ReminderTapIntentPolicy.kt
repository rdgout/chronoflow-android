package net.chronoflow.app.reminders

import net.chronoflow.core.data.reminders.ACTION_OPEN
import net.chronoflow.core.data.reminders.EXTRA_ACCOUNT_ID
import net.chronoflow.core.data.reminders.EXTRA_BLOCK_ID
import net.chronoflow.core.data.reminders.EXTRA_INPUT_HASH
import net.chronoflow.core.data.reminders.EXTRA_TRIP_ID
import net.chronoflow.core.navigation.ReminderTapExtras

object ReminderTapIntentPolicy {
    fun tapToCapture(
        action: String?,
        flags: Int,
        restored: Boolean,
        extra: (String) -> String?,
    ): ReminderTapExtras? {
        if (restored || action != ACTION_OPEN || flags and 0x00100000 != 0) return null
        return ReminderTapExtras.parse(
            extra(EXTRA_TRIP_ID),
            extra(EXTRA_BLOCK_ID),
            extra(EXTRA_ACCOUNT_ID),
            extra(EXTRA_INPUT_HASH),
        )
    }
}
