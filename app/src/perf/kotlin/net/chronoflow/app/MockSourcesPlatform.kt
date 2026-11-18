package net.chronoflow.app

import kotlinx.coroutines.CoroutineDispatcher
import net.chronoflow.app.perf.PerfMockSources

internal fun platformMockSources(io: CoroutineDispatcher): MockSources? = PerfMockSources(io)
