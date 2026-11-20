package net.chronoflow.app

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.rules.ExternalResource
import kotlin.time.Duration.Companion.seconds

internal class GraphTestRule : ExternalResource() {
    val dispatcher = StandardTestDispatcher()
    val overrides = GraphOverrides(dispatchers = AppDispatchers(main = dispatcher, default = dispatcher))
    private val graphs = mutableListOf<AppGraph>()

    fun own(graph: AppGraph): AppGraph = graph.also { graphs += it }

    override fun after() {
        runTest(dispatcher, timeout = 60.seconds) {
            graphs.asReversed().forEach { it.closeAndJoinForTest() }
        }
    }
}
