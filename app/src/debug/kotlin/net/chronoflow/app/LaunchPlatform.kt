package net.chronoflow.app

import android.content.Intent
import androidx.compose.runtime.Composable
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.debug.DebugLaunchOverrides
import net.chronoflow.debug.DebugRouteLabel
import net.chronoflow.feature.launch.LaunchController

internal object LaunchPlatform {
    fun mockSources(io: kotlinx.coroutines.CoroutineDispatcher): MockSources? = platformMockSources(io)

    fun welcomeDestinations(navigator: RootNavigator): WelcomeDestinations =
        net.chronoflow.debug.DebugWelcomeDestinations(navigator)

    val ioViolationSink: (Boolean, List<String>) -> Unit = net.chronoflow.debug.StrictModeViolationCounter::record
    val routeLabel: (@Composable (String) -> Unit)? = { DebugRouteLabel(it) }
    private var current: LaunchController? = null

    fun controller(graph: AppGraph): LaunchController = current ?: newLaunchController(graph).also { current = it }

    fun overrides(
        intent: Intent?,
        graph: AppGraph? = null,
    ) = DebugLaunchOverrides.fromIntent(
        intent,
        graph?.launchReadiness ?: net.chronoflow.core.model.launch.SignedOutReadiness,
    )

    fun observeRoutes(graph: AppGraph) =
        net.chronoflow.debug.RouteProbe
            .observe(graph)

    suspend fun routeScreen(
        graph: AppGraph,
        intent: Intent?,
    ) {
        val seed =
            net.chronoflow.debug.TestSeed
                .fromIntent(intent)
        if (seed.profile == net.chronoflow.debug.UiTestProfile.Screen) {
            net.chronoflow.debug.ScreenSessionFixture
                .route(graph, seed.raw["CF_AUTH_TEST_AUDIT_SCREEN"])
        }
    }

    fun reset(graph: AppGraph) {
        check(BuildConfig.DEBUG)
        current?.onActivityDestroyed()
        current = newLaunchController(graph)
        graph.welcomeDestinations.closeSeam()
    }
}

internal fun AppGraph.resetLaunchForTest() {
    check(BuildConfig.DEBUG)
    LaunchPlatform.reset(this)
}

internal fun AppGraph.resetIoViolationsForTest() =
    net.chronoflow.debug.StrictModeViolationCounter
        .reset()

internal val AppGraph.ioViolationsForTest: Int get() = net.chronoflow.debug.StrictModeViolationCounter.count
