package net.chronoflow.feature.auth.links

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import net.chronoflow.core.model.auth.ResetPasswordLink
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.model.session.SessionUpdate

class GuestResetLinkRouter(
    private val presence: suspend () -> SessionPresence?,
    updates: Flow<SessionUpdate?>,
    scope: CoroutineScope,
    private val open: (ResetPasswordLink) -> Unit,
) {
    var pending: ResetPasswordLink? = null
        private set

    init {
        scope.launch {
            updates.collect { if (it?.presence == SessionPresence.SignedOut) consume() }
        }
    }

    suspend fun receive(link: ResetPasswordLink) {
        pending = link
        consume()
    }

    private suspend fun consume() {
        if (presence() != SessionPresence.SignedOut) return
        val link = pending ?: return
        pending = null
        open(link)
    }
}
