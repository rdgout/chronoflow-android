package net.chronoflow.app.links

import net.chronoflow.app.PendingLaunchIntents
import net.chronoflow.core.model.auth.AuthLink

class AuthLinkDispatcher(
    private val pending: PendingLaunchIntents,
    private val handle: (AuthLink) -> Unit,
) {
    private var attached = false

    fun attach() {
        attached = true
        deliver()
    }

    fun deliver() {
        if (!attached) return
        val links = pending.takeLinks { AuthLink.parse(it) != null }
        links.lastOrNull()?.let { handle(checkNotNull(AuthLink.parse(it))) }
    }
}
