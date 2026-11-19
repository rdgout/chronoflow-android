package net.chronoflow.core.navigation.shell

import net.chronoflow.core.navigation.Route

enum class ShellTab { Today, Trips, Settings }

data class ShellNavState(
    val selected: ShellTab,
    val stacks: Map<ShellTab, List<Route>>,
    val overlay: Route? = null,
    val scrollToTop: Map<ShellTab, Int> = emptyMap(),
)

sealed interface ShellStart {
    data class Tab(
        val tab: ShellTab,
    ) : ShellStart

    data class Trip(
        val tripId: String,
    ) : ShellStart
}
