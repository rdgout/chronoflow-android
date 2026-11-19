package net.chronoflow.debug

import net.chronoflow.core.navigation.shell.ShellStart
import net.chronoflow.core.navigation.shell.ShellTab
import java.util.concurrent.atomic.AtomicBoolean

object DebugShellStart {
    private val consumed = AtomicBoolean(false)

    fun take(): ShellStart? {
        if (!consumed.compareAndSet(false, true)) return null
        return when (val route = DebugSeedRouting.current?.planRoute) {
            PlanRouteSeed.Trips -> ShellStart.Tab(ShellTab.Trips)
            is PlanRouteSeed.Trip -> ShellStart.Trip(route.tripId)
            null -> null
        }
    }

    fun reset() {
        consumed.set(false)
    }
}
