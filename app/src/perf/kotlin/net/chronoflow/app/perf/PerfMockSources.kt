package net.chronoflow.app.perf

import kotlinx.coroutines.CoroutineDispatcher
import net.chronoflow.app.BuildConfig
import net.chronoflow.app.GraphOverrides
import net.chronoflow.app.MockSources
import net.chronoflow.core.datamock.FileMockStateStore
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.model.DataSource
import java.io.File
import java.time.Instant
import kotlin.random.Random
import kotlin.time.DurationUnit

class PerfMockSources(
    private val io: CoroutineDispatcher,
) : MockSources {
    override fun dataSource(
        root: File,
        overrides: GraphOverrides,
        bearer: suspend () -> String?,
    ): DataSource =
        MockDataSource(
            store = FileMockStateStore(root),
            seed =
                overrides.mockSeed?.let {
                    MockAppSeed.make(
                        Instant.now(),
                        it.unverified,
                        it.walkthroughSeen,
                        it.termsPending,
                    )
                },
            delay =
                overrides.mockDelay?.let { duration -> { duration.toDouble(DurationUnit.SECONDS) } }
                    ?: { Random.nextDouble(0.3, 0.8) },
            bearer = bearer,
            debugAppSeed = BuildConfig.DEBUG,
            io = io,
        )
}
