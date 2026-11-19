package net.chronoflow.app.perf

import net.chronoflow.app.LaunchDiagnostics
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.launch.SignedOutReadiness
import net.chronoflow.feature.launch.LaunchOverrides

object LaunchDiagnosticsParser {
    @Suppress("UNUSED_PARAMETER")
    fun parse(extras: Map<String, String>): LaunchDiagnostics = LaunchDiagnostics.None

    @Suppress("UNUSED_PARAMETER")
    fun overrides(
        extras: Map<String, String>,
        nativeReadiness: LaunchReadiness = SignedOutReadiness,
    ): LaunchOverrides = LaunchOverrides.None
}
