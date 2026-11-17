package net.chronoflow.app

import android.content.Intent
import net.chronoflow.app.perf.LaunchDiagnosticsParser
import net.chronoflow.feature.launch.LaunchOverrides

internal object LaunchPlatform {
    private var current: net.chronoflow.feature.launch.LaunchController? = null

    fun controller(graph: AppGraph): net.chronoflow.feature.launch.LaunchController =
        current ?: newLaunchController(graph).also { current = it }

    fun overrides(intent: Intent?): LaunchOverrides =
        if (BuildConfig.PERF_ENTRY &&
            intent?.hasExtra("cf.trip-page") == true
        ) {
            LaunchDiagnosticsParser.overrides(launchExtras(intent)).copy(skipOverlay = true)
        } else {
            LaunchDiagnosticsParser.overrides(launchExtras(intent))
        }
}
