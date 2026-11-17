package net.chronoflow.app

import android.content.Intent
import net.chronoflow.debug.DebugLaunchOverrides
import net.chronoflow.feature.launch.LaunchController

internal object LaunchPlatform {
    private var current: LaunchController? = null
    val controller: LaunchController get() = current ?: newLaunchController().also { current = it }

    fun overrides(intent: Intent?) = DebugLaunchOverrides.fromIntent(intent)

    fun reset() {
        check(BuildConfig.DEBUG)
        current = newLaunchController()
    }
}

internal fun AppGraph.resetLaunchForTest() {
    check(BuildConfig.DEBUG)
    LaunchPlatform.reset()
}
