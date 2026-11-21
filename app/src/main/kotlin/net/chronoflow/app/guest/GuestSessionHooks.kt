package net.chronoflow.app.guest

import net.chronoflow.core.data.session.SessionHooks
import net.chronoflow.core.wizard.model.GuestWizardStore

internal class GuestSessionHooks(
    private val store: GuestWizardStore,
    private val next: SessionHooks,
) : SessionHooks {
    override suspend fun onForeground() {
        quietly { store.recover() }
        next.onForeground()
    }

    override suspend fun onSessionEnding() = next.onSessionEnding()
}
