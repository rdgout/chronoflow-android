package net.chronoflow.debug

import android.content.Intent
import net.chronoflow.app.perf.LaunchDiagnosticsParser
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.launch.SignedOutReadiness
import net.chronoflow.feature.launch.LaunchOverrides
import kotlin.time.Duration.Companion.milliseconds

object DebugLaunchOverrides {
    fun fromIntent(
        intent: Intent?,
        nativeReadiness: LaunchReadiness = SignedOutReadiness,
    ): LaunchOverrides = fromSeed(TestSeed.fromIntent(intent), nativeReadiness)

    fun fromSeed(
        seed: TestSeed,
        nativeReadiness: LaunchReadiness = SignedOutReadiness,
    ): LaunchOverrides =
        LaunchDiagnosticsParser.overrides(seed.raw, nativeReadiness).copy(
            reduceMotion = seed.reduceMotion,
            holdSplash = seed.holdSplash == true,
            freezeAt = seed.freezeAtMs?.milliseconds,
            skipOverlay = seed.profile == UiTestProfile.Screen,
        )
}
