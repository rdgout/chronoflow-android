package net.chronoflow.app

import net.chronoflow.app.perf.StubReadinessFactory
import net.chronoflow.app.perf.today.FixtureTodaySource
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.feature.launch.model.LaunchClock
import net.chronoflow.feature.today.data.TodaySource

internal fun todayStubSelected(
    perfEntry: Boolean,
    readiness: ((LaunchClock) -> LaunchReadiness)?,
): Boolean {
    val config = (readiness as? StubReadinessFactory)?.config
    return perfEntry && config?.session == StubSession.SignedIn && config.gate == Gate.App
}

internal fun platformTodaySource(graph: AppGraph): TodaySource? =
    if (todayStubSelected(BuildConfig.PERF_ENTRY, graph.launch.overrides.readiness)) {
        FixtureTodaySource(graph.dispatchers.default)
    } else {
        null
    }
