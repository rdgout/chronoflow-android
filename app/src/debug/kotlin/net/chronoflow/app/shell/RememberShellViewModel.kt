package net.chronoflow.app.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.shell.ShellNav

internal object ShellRouteProbe {
    @Volatile var route: Route? = null
        private set

    fun record(route: Route?) {
        this.route = route
    }
}

@Composable
internal fun rememberShellViewModel(root: Route): ShellViewModel {
    val holder = viewModel { ShellViewModel(ShellNav.fromRoot(root)) }
    ObserveShellRoute(holder)
    return holder
}

@Composable
private fun ObserveShellRoute(holder: ShellViewModel) {
    val state = holder.state
    val route = state.overlay ?: state.stacks.getValue(state.selected).last()
    SideEffect { ShellRouteProbe.record(route) }
    DisposableEffect(holder) {
        onDispose { ShellRouteProbe.record(null) }
    }
}
