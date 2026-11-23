package net.chronoflow.app

import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.feature.launch.model.LaunchRoute
import net.chronoflow.feature.launch.model.LaunchState

class LaunchSessionBridge(
    private val navigator: RootNavigator,
    private val rootMounted: () -> Unit = {},
) {
    private var attached = false
    private var launchRevision = navigator.state.value.revision

    fun update(state: LaunchState) {
        val route = state.route ?: return
        if (navigator.state.value.revision != launchRevision) return
        navigator.navigate(route.toRoute())
        launchRevision = navigator.state.value.revision
        if (!attached) {
            attached = true
            rootMounted()
        }
    }
}

fun LaunchRoute.toRoute(): Route =
    when (this) {
        LaunchRoute.Welcome -> Route.Welcome
        LaunchRoute.Today -> Route.Today
        LaunchRoute.Verify -> Route.Verify
        LaunchRoute.Terms -> Route.Terms
        LaunchRoute.Walkthrough -> Route.Walkthrough()
    }
