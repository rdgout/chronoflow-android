package net.chronoflow.core.data.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION_ALARM) return
        val runtime = (context.applicationContext as? ReminderHost)?.reminderRuntime ?: return
        val pending = goAsync()
        runtime.scope.launch(runtime.io) {
            try {
                withTimeoutOrNull(8.seconds) {
                    attemptReminderDelivery {
                        ReminderDelivery.deliver(context.applicationContext, intent, runtime, runtime.clock)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
