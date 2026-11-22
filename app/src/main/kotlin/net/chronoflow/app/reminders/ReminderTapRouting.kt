package net.chronoflow.app.reminders

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.navigation.ReminderTapExtras
import net.chronoflow.core.navigation.ReminderTapRouter

class ReminderTapRouting(
    private val router: ReminderTapRouter,
    private val activeSession: suspend () -> SessionSnapshot?,
    private val open: (ReminderTapExtras) -> Unit,
    private val main: CoroutineDispatcher,
) {
    suspend fun run() =
        withContext(main) {
            router.pending.filterNotNull().collect { tap ->
                router.clear()
                val current = activeSession() ?: return@collect
                if (tap.accountId == null || tap.accountId == current.session.accountId) open(tap)
            }
        }
}
