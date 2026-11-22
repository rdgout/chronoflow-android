package net.chronoflow.debug

import kotlinx.coroutines.CoroutineDispatcher
import net.chronoflow.core.datamock.MemoryMockStateStore
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import java.time.Instant

internal val auditSeedNow: Instant = Instant.parse("2026-11-22T12:00:00Z")
internal const val AUDIT_SEED_ID = "019a562b-8000-7000-8000-000000000082"

internal fun auditSeedSource(io: CoroutineDispatcher): MockDataSource =
    MockDataSource(
        store = MemoryMockStateStore(),
        seed = MockAppSeed.make(auditSeedNow),
        now = { auditSeedNow },
        delay = { 0.0 },
        sleep = {},
        debugAppSeed = false,
        io = io,
    )
