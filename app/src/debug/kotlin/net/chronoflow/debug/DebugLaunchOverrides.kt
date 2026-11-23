package net.chronoflow.debug

import android.content.Intent
import net.chronoflow.app.perf.LaunchDiagnosticsParser
import net.chronoflow.feature.launch.LaunchOverrides
import kotlin.time.Duration.Companion.milliseconds

object DebugLaunchOverrides {
    fun fromIntent(intent: Intent?): LaunchOverrides = fromSeed(TestSeed.fromIntent(intent))

    fun fromSeed(seed: TestSeed): LaunchOverrides =
        LaunchDiagnosticsParser.overrides(seed.raw).copy(
            reduceMotion = seed.reduceMotion,
            holdSplash = seed.holdSplash == true,
            freezeAt = seed.freezeAtMs?.milliseconds,
            skipOverlay = seed.profile == UiTestProfile.Screen,
        )
}
