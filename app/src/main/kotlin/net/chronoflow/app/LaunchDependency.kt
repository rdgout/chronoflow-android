package net.chronoflow.app

import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import net.chronoflow.app.perf.MainThreadStall
import net.chronoflow.feature.launch.LaunchController

internal fun newLaunchController(graph: AppGraph): LaunchController {
    val bridge = graph.launchBridge
    return LaunchController(
        scope = graph.launchScope,
        processToken = graph.processToken,
        readinessFactory = { graph.launchReadiness },
        onFirstCommit = graph::onFirstCommit,
        onStateChange = { state ->
            if (state.session != null) {
                net.chronoflow.feature.launch.LaunchTrace
                    .endSession()
            }
            bridge.update(state)
        },
        frameLog =
            if (BuildConfig.DEBUG) {
                net.chronoflow.feature.launch
                    .LaunchFrameLog()
            } else {
                null
            },
        onReadinessStarted = {
            t0,
            overrides,
            ->
            MainThreadStall.schedule(overrides.mainThreadStall, t0)
        },
        moon = { resources ->
            ImageBitmap.imageResource(resources, R.drawable.launch_moon)
        },
    )
}

internal fun launchExtras(intent: Intent?): Map<String, String> =
    listOf(
        "cf.session",
        "cf.gate",
        "cf.stored",
        "cf.readyDelay",
        "cf.mode",
        "cf.readyAt",
        "cf.mainThreadStall",
        "cf.reduceMotion",
    ).mapNotNull { key -> intent?.getStringExtra(key)?.let { key to it } }
        .toMap()
