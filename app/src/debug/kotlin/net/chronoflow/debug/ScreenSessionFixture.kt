package net.chronoflow.debug

import net.chronoflow.app.AppGraph
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.toRoute
import net.chronoflow.feature.launch.LaunchTrace

object ScreenSessionFixture {
    suspend fun route(
        graph: AppGraph,
        audit: String? = null,
    ) {
        val presence = graph.readiness.session()
        LaunchTrace.endSession()
        val route =
            if (presence is SessionPresence.SignedIn) {
                graph.readiness.allowFirstLoad()
                graph.readiness.gate().toRoute()
            } else {
                Route.Welcome
            }
        graph.rootNavigator.navigate(route)
        when (audit) {
            "sign-in" -> graph.rootNavigator.navigate(Route.SignIn)
            "forgot" -> graph.rootNavigator.navigate(Route.Forgot)
        }
        graph.finishLaunch()
    }
}
