package net.chronoflow.app.perf

import net.chronoflow.app.LaunchDiagnostics
import net.chronoflow.app.StubConfig
import net.chronoflow.app.StubSession
import net.chronoflow.app.StubStored
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.launch.SignedOutReadiness
import net.chronoflow.feature.launch.LaunchMode
import net.chronoflow.feature.launch.LaunchOverrides
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object LaunchDiagnosticsParser {
    fun parse(extras: Map<String, String>): LaunchDiagnostics {
        fun ms(key: String): Duration? =
            extras[key]?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..60_000.0 }?.milliseconds
        val mode =
            when (extras["cf.mode"]) {
                "ready" -> LaunchMode.Ready
                "slow" -> LaunchMode.Slow
                "welcome" -> LaunchMode.Welcome
                else -> null
            }
        val stub =
            if (listOf("cf.session", "cf.gate", "cf.stored", "cf.readyDelay").any { it in extras }) {
                StubConfig(
                    session =
                        when (extras["cf.session"]) {
                            "in", "signedIn" -> StubSession.SignedIn
                            "out", "signedOut" -> StubSession.SignedOut
                            "never" -> StubSession.Never
                            else -> StubSession.Native
                        },
                    gate = Gate.entries.firstOrNull { it.rawValue == extras["cf.gate"] } ?: Gate.App,
                    stored =
                        when (extras["cf.stored"]) {
                            "no" -> StubStored.No
                            "never" -> StubStored.Never
                            else -> StubStored.Yes
                        },
                    readyDelay =
                        (
                            extras["cf.readyDelay"]?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..60.0 }
                                ?: 0.0
                        ).seconds,
                )
            } else {
                null
            }
        return LaunchDiagnostics(
            mode,
            ms("cf.readyAt") ?: 4600.milliseconds,
            ms("cf.mainThreadStall"),
            when (extras["cf.reduceMotion"]) {
                "1", "true" -> true
                "0", "false" -> false
                else -> null
            },
            stub,
        )
    }

    fun overrides(
        extras: Map<String, String>,
        nativeReadiness: LaunchReadiness = SignedOutReadiness,
    ): LaunchOverrides {
        val diagnostics = parse(extras)
        return LaunchOverrides(
            mode = diagnostics.mode,
            readyAt = diagnostics.readyAt,
            mainThreadStall = diagnostics.mainThreadStall,
            reduceMotion = diagnostics.reduceMotion,
            readiness = diagnostics.stub?.let { config -> { clock -> StubReadiness(config, clock, nativeReadiness) } },
        )
    }
}
