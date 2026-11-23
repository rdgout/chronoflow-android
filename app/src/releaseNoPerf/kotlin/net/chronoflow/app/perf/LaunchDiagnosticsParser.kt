package net.chronoflow.app.perf

import net.chronoflow.app.LaunchDiagnostics
import net.chronoflow.feature.launch.LaunchOverrides

object LaunchDiagnosticsParser {
    @Suppress("UNUSED_PARAMETER")
    fun parse(extras: Map<String, String>): LaunchDiagnostics = LaunchDiagnostics.None

    @Suppress("UNUSED_PARAMETER")
    fun overrides(extras: Map<String, String>): LaunchOverrides = LaunchOverrides.None
}
