package net.chronoflow.core.data.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import net.chronoflow.core.model.store.StoreFailure
import java.io.IOException

class ReminderRescheduleWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        try {
            (applicationContext as? ReminderHost)?.reminderRuntime?.backgroundPass()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: StoreFailure) {
        } catch (_: IOException) {
        }
        return Result.success()
    }
}
