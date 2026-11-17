package net.chronoflow.app

import android.content.Intent
import net.chronoflow.app.perf.LaunchDiagnosticsParser
import net.chronoflow.feature.launch.LaunchOverrides

internal object LaunchPlatform {
    val controller by lazy { newLaunchController() }

    fun overrides(intent: Intent?): LaunchOverrides =
        if (BuildConfig.PERF_ENTRY &&
            intent?.hasExtra("cf.trip-page") == true
        ) {
            LaunchDiagnosticsParser.overrides(launchExtras(intent)).copy(skipOverlay = true)
        } else {
            LaunchDiagnosticsParser.overrides(launchExtras(intent))
        }
}
