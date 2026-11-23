package net.chronoflow.core.data.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch

class ReminderUpkeepReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in actions) return
        val runtime = (context.applicationContext as? ReminderHost)?.reminderRuntime ?: return
        val pending = goAsync()
        runtime.scope.launch(runtime.io) {
            try {
                ReminderWork.enqueueReschedule(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        val actions: Set<String> =
            setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_LOCALE_CHANGED,
            )
    }
}
