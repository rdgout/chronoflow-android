package net.chronoflow.app

import android.content.Intent
import net.chronoflow.debug.DebugLaunchOverrides
import net.chronoflow.feature.launch.LaunchController

internal object LaunchPlatform {
    private var current: LaunchController? = null

    fun controller(graph: AppGraph): LaunchController = current ?: newLaunchController(graph).also { current = it }

    fun overrides(intent: Intent?) = DebugLaunchOverrides.fromIntent(intent)

    fun reset(graph: AppGraph) {
        check(BuildConfig.DEBUG)
        current?.onActivityDestroyed()
        current = newLaunchController(graph)
    }
}

internal fun AppGraph.resetLaunchForTest() {
    check(BuildConfig.DEBUG)
    LaunchPlatform.reset(this)
}
