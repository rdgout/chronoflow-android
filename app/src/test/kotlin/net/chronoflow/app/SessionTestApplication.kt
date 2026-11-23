package net.chronoflow.app

import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds

class SessionTestApplication : ChronoFlowApp() {
    private val initialDispatcher = StandardTestDispatcher()
    private lateinit var initialGraph: AppGraph

    override fun createGraph(): AppGraph =
        AppGraph(
            this,
            overrides =
                GraphOverrides(
                    sessionCipher = SoftwareSessionCipher(),
                    dispatchers = AppDispatchers(main = initialDispatcher, default = initialDispatcher),
                ),
        ).also { initialGraph = it }

    override fun onTerminate() {
        // Seeding can replace app.graph; the original application's lifecycle scope still needs a join.
        initialGraph.appScope.cancel()
        runTest(initialDispatcher, timeout = 60.seconds) { initialGraph.closeAndJoinForTest() }
        super.onTerminate()
    }
}
