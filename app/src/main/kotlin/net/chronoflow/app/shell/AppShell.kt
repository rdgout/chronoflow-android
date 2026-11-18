package net.chronoflow.app.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.shell.ShellNav

@Composable
internal fun AppShell(
    root: Route,
    placeholder: @Composable (Route) -> Unit,
) {
    val render = rememberUpdatedState(placeholder)
    val parent = checkNotNull(LocalViewModelStoreOwner.current)
    val holder = remember(parent, root) { ShellHolder(ShellNav.fromRoot(root), parent) }
    AppShellScaffold(
        holder,
        today = { render.value(Route.Today) },
        trips = { render.value(Route.Trips) },
        settings = { render.value(Route.Settings) },
        page = { render.value(it) },
        overlay = { render.value(it) },
    )
}
