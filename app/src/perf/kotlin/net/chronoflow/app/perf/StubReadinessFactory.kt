package net.chronoflow.app.perf

import net.chronoflow.app.StubConfig
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.feature.launch.model.LaunchClock

internal class StubReadinessFactory(
    val config: StubConfig,
    private val nativeReadiness: LaunchReadiness,
) : (LaunchClock) -> LaunchReadiness {
    override fun invoke(clock: LaunchClock): LaunchReadiness = StubReadiness(config, clock, nativeReadiness)
}
