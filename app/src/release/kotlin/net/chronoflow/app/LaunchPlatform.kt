package net.chronoflow.app

import android.content.Intent
import net.chronoflow.feature.launch.LaunchOverrides

internal object LaunchPlatform {
    val controller by lazy { newLaunchController() }

    fun overrides(intent: Intent?): LaunchOverrides =
        if (BuildConfig.PERF_ENTRY &&
            intent?.hasExtra("cf.trip-page") == true
        ) {
            LaunchOverrides(skipOverlay = true)
        } else {
            LaunchOverrides.None
        }
}
