package net.chronoflow.core.data.reminders

import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ReminderWork {
    fun rescheduleRequest(sdkInt: Int = Build.VERSION.SDK_INT): OneTimeWorkRequest {
        val builder = OneTimeWorkRequestBuilder<ReminderRescheduleWorker>()
        if (sdkInt >= 31) builder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        return builder.build()
    }

    fun enqueueReschedule(context: Context) {
        WorkManager
            .getInstance(
                context,
            ).enqueueUniqueWork(WORK_RESCHEDULE, ExistingWorkPolicy.REPLACE, rescheduleRequest())
    }

    fun ensureRefill(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_REFILL,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ReminderRescheduleWorker>(REFILL_HOURS, TimeUnit.HOURS).build(),
        )
    }

    fun cancelAll(context: Context) {
        val manager = WorkManager.getInstance(context)
        manager.cancelUniqueWork(WORK_RESCHEDULE)
        manager.cancelUniqueWork(WORK_REFILL)
    }
}
