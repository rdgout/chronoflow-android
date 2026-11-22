package net.chronoflow.debug

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.chronoflow.core.datamock.FileMockStateStore
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockScenarioValues
import net.chronoflow.core.datamock.MockServerState
import java.io.File
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

object AuditSeedStatus {
    val prepared = AtomicBoolean(false)
}

internal object AuditMockSeed {
    fun transform(
        original: MockServerState,
        seed: TestSeed,
        now: Instant,
    ): MockServerState {
        if (seed.testId == null) return original
        val countdown = seed.auditScreen == "verify" && seed.auditState in setOf("throttle", "failedThrottle")
        return original.copy(
            accounts =
                original.accounts.mapValues { (_, account) ->
                    if (account.snapshot.email != MockScenarioValues.demoEmail) {
                        account
                    } else {
                        account.copy(
                            trips = if (seed.emptyTrips) emptyList() else account.trips,
                            details = if (seed.emptyTrips) emptyMap() else account.details,
                            resendAvailableAt =
                                if (countdown) {
                                    now.epochSecond + now.nano / 1_000_000_000.0 + 60
                                } else {
                                    account.resendAvailableAt
                                },
                        )
                    }
                },
        )
    }

    suspend fun prepare(
        root: File,
        seed: TestSeed,
        now: Instant,
        io: CoroutineDispatcher,
    ) = withContext(io) {
        if (seed.testId == null || (!seed.emptyTrips && seed.auditScreen != "verify")) return@withContext
        val store = FileMockStateStore(root)
        val original = store.load() ?: MockAppSeed.make(now, seed.unverified, seed.walkthroughSeen, seed.termsPending)
        store.save(transform(original, seed, now))
    }
}
