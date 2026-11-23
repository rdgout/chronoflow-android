package net.chronoflow.app.wizard

import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.WizardMode
import java.util.concurrent.atomic.AtomicBoolean

internal class WizardLauncher(
    private val overlay: () -> Route?,
    private val push: (Route) -> Unit,
) {
    private val pending = AtomicBoolean(false)

    fun open() {
        if (overlay() != null || !pending.compareAndSet(false, true)) return
        push(Route.Wizard(WizardMode.Create))
    }

    fun overlayChanged() {
        pending.set(false)
    }
}
