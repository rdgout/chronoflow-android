package net.chronoflow.debug

import kotlinx.coroutines.launch
import net.chronoflow.app.AppGraph
import net.chronoflow.core.navigation.RootState

object RouteProbe {
    @Volatile var state = RootState()
        private set

    internal fun observe(graph: AppGraph) {
        state = graph.rootNavigator.state.value
        graph.appScope.launch { graph.rootNavigator.state.collect { state = it } }
    }
}
