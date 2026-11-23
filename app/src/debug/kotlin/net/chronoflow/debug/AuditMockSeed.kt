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
        val offset =
            if (seed.auditScreen == "today") {
                when (seed.auditState) {
                    "future" -> 365L
                    "completed" -> -365L
                    else -> 0L
                }
            } else {
                0L
            }
        val shifted =
            if (offset == 0L) {
                null
            } else {
                MockAppSeed
                    .make(
                        now.plusSeconds(offset * 86400),
                        seed.unverified,
                        seed.walkthroughSeen,
                        seed.termsPending,
                    ).accounts
                    .getValue(MockScenarioValues.demoID)
            }
        val countdown = seed.auditScreen == "verify" && seed.auditState in setOf("throttle", "failedThrottle")
        return original.copy(
            accounts =
                original.accounts.mapValues { (_, account) ->
                    if (account.snapshot.email != MockScenarioValues.demoEmail) {
                        account
                    } else {
                        account.copy(
                            trips = if (seed.emptyTrips) emptyList() else shifted?.trips ?: account.trips,
                            details = if (seed.emptyTrips) emptyMap() else shifted?.details ?: account.details,
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
        if (seed.testId == null ||
            (!seed.emptyTrips && seed.auditScreen !in setOf("verify", "today"))
        ) {
            return@withContext
        }
        val store = FileMockStateStore(root)
        val original = store.load() ?: MockAppSeed.make(now, seed.unverified, seed.walkthroughSeen, seed.termsPending)
        store.save(transform(original, seed, now))
    }
}
