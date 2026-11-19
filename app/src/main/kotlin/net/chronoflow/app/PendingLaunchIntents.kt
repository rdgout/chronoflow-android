package net.chronoflow.app

import android.content.Intent
import net.chronoflow.core.navigation.PendingLaunchIntent
import net.chronoflow.core.navigation.ReminderTapExtras

class PendingLaunchIntents {
    private val buffer = PendingLaunchIntent.Buffer()

    fun capture(intent: Intent?) {
        val links =
            if (intent?.action == Intent.ACTION_VIEW &&
                intent.data?.scheme == "https"
            ) {
                setOfNotNull(intent.dataString)
            } else {
                emptySet()
            }
        val trip = intent?.getStringExtra("cf.reminder.tripId")
        val reminder =
            trip?.let {
                ReminderTapExtras(
                    it,
                    intent.getStringExtra("cf.reminder.blockId"),
                    intent.getStringExtra("cf.reminder.accountId"),
                    intent.getStringExtra("cf.reminder.inputHash"),
                )
            }
        buffer.capture(PendingLaunchIntent(links, reminder))
    }

    fun consume(): PendingLaunchIntent? = buffer.consume()
}
