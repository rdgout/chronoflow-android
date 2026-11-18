package net.chronoflow.debug

import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import net.chronoflow.app.AppGraph
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.GraphOverrides
import net.chronoflow.app.resetLaunchForTest

object DebugAppHooks {
    suspend fun resetGraph(
        app: ChronoFlowApp,
        overrides: GraphOverrides,
    ) = withContext(NonCancellable) {
        val old = app.graph
        withContext(overrides.dispatchers?.main ?: old.dispatchers.main) {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(old.activation)
        }
        old.close()
        val graph = AppGraph(app, overrides = overrides)
        app.graph = graph
        withContext(graph.dispatchers.main) {
            graph.resetLaunchForTest()
            ProcessLifecycleOwner.get().lifecycle.addObserver(graph.activation)
        }
    }
}
