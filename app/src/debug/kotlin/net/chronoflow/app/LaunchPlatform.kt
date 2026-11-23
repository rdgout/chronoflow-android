package net.chronoflow.app

import android.content.Intent
import androidx.compose.runtime.Composable
import net.chronoflow.debug.DebugLaunchOverrides
import net.chronoflow.debug.DebugRouteLabel
import net.chronoflow.feature.launch.LaunchController

internal object LaunchPlatform {
    var welcomeDestinations: WelcomeDestinations = net.chronoflow.debug.DebugWelcomeDestinations()
        private set
    val ioViolationSink: (Boolean, List<String>) -> Unit = net.chronoflow.debug.StrictModeViolationCounter::record
    val routeLabel: (@Composable (String) -> Unit)? = { DebugRouteLabel(it) }
    private var current: LaunchController? = null

    fun controller(graph: AppGraph): LaunchController = current ?: newLaunchController(graph).also { current = it }

    fun overrides(intent: Intent?) = DebugLaunchOverrides.fromIntent(intent)

    fun reset(graph: AppGraph) {
        check(BuildConfig.DEBUG)
        current?.onActivityDestroyed()
        current = newLaunchController(graph)
        welcomeDestinations = net.chronoflow.debug.DebugWelcomeDestinations()
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
