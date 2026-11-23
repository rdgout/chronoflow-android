package net.chronoflow.app.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CoroutineDispatcher
import net.chronoflow.app.wizard.WizardLauncher
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.shell.ShellNav
import net.chronoflow.core.navigation.shell.ShellNavState
import net.chronoflow.core.navigation.shell.ShellStart
import net.chronoflow.core.navigation.shell.ShellTab
import net.chronoflow.feature.wizard.TripWizardDependencies

@Stable
internal class ShellViewModel(
    initial: ShellNavState,
) : ViewModel() {
    private var launcher: WizardLauncher? = null
    private var launcherDependencies: TripWizardDependencies? = null

    fun wizardLauncher(
        dependencies: TripWizardDependencies?,
        io: CoroutineDispatcher,
    ): WizardLauncher {
        launcher?.takeIf { launcherDependencies === dependencies }?.let { return it }
        return WizardLauncher({ state.overlay }, ::push, dependencies, io).also {
            launcherDependencies = dependencies
            launcher = it
        }
    }

    private var overlayStoreOwner = TabViewModelStoreOwner()
    val overlayOwner: TabViewModelStoreOwner get() = overlayStoreOwner
    private var mutableState by mutableStateOf(initial)
    var state: ShellNavState
        get() = mutableState
        private set(value) {
            val closed = mutableState.overlay != null && value.overlay == null
            if (mutableState.overlay == null && value.overlay != null) overlayStoreOwner = TabViewModelStoreOwner()
            val overlayChanged = mutableState.overlay != value.overlay
            mutableState = value
            if (overlayChanged) launcher?.overlayChanged()
            if (closed) overlayOwner.viewModelStore.clear()
        }
    private val owners = ShellTab.entries.associateWith { TabViewModelStoreOwner() }

    fun owner(tab: ShellTab): TabViewModelStoreOwner = owners.getValue(tab)

    fun select(tab: ShellTab) {
        state = ShellNav.select(state, tab)
    }

    fun leaveTripPage(tab: ShellTab) {
        state = ShellNav.leaveTripPage(state, tab)
    }

    fun push(route: Route) {
        state = ShellNav.push(state, route)
    }

    fun replaceRoute(
        old: Route.TripPage,
        next: Route.TripPage,
    ) {
        val stacks =
            state.stacks.mapValues { (_, stack) ->
                val index = stack.lastIndexOf(old)
                if (index < 0) stack else stack.toMutableList().apply { set(index, next) }.toList()
            }
        state = state.copy(stacks = stacks)
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

    fun closeWizard(exit: net.chronoflow.feature.wizard.shell.WizardExit) {
        when (exit) {
            net.chronoflow.feature.wizard.shell.WizardExit.Welcome -> {}

            is net.chronoflow.feature.wizard.shell.WizardExit.Saved -> {
                if (state.overlay is Route.Wizard) {
                    wizardSaved(
                        exit.tripId,
                    )
                }
            }

            net.chronoflow.feature.wizard.shell.WizardExit.Trips -> {
                if (state.overlay is Route.Wizard) back()
            }
        }
    }

    override fun onCleared() {
        overlayOwner.viewModelStore.clear()
        owners.values.forEach { it.viewModelStore.clear() }
    }
}

@Composable
internal fun rememberShellViewModel(root: Route): ShellViewModel = viewModel { ShellViewModel(ShellNav.fromRoot(root)) }
