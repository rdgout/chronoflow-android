package net.chronoflow.app.shell

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelStoreOwner
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.shell.ShellNav
import net.chronoflow.core.navigation.shell.ShellNavState
import net.chronoflow.core.navigation.shell.ShellStart
import net.chronoflow.core.navigation.shell.ShellTab

@Stable
internal class ShellHolder(
    initial: ShellNavState,
    parent: ViewModelStoreOwner,
) {
    var state by mutableStateOf(initial)
        private set
    private val owners = ShellTab.entries.associateWith { TabViewModelStoreOwner(parent) }
    private var cleared = false

    fun owner(tab: ShellTab): TabViewModelStoreOwner = owners.getValue(tab)

    fun select(tab: ShellTab) {
        state = ShellNav.select(state, tab)
    }

    fun push(route: Route) {
        state = ShellNav.push(state, route)
    }

    fun openTrip(tripId: String) {
        state = ShellNav.openTrip(state, tripId)
    }

    fun seed(start: ShellStart) {
        state = ShellNav.seed(state, start)
    }

    fun wizardSaved(tripId: String) {
        state = ShellNav.wizardSaved(state, tripId)
    }

    fun back(): Boolean {
        val (next, handled) = ShellNav.back(state)
        state = next
        return handled
    }

    fun clear() {
        if (cleared) return
        cleared = true
        owners.values.forEach { it.viewModelStore.clear() }
    }
}
