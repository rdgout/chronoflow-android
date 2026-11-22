package net.chronoflow.app.perf

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import net.chronoflow.app.BuildConfig
import net.chronoflow.app.GraphOverrides
import net.chronoflow.app.MockSeedFlags
import net.chronoflow.app.MockSources
import net.chronoflow.core.datamock.FileMockStateStore
import net.chronoflow.core.datamock.MockAppSeed
import net.chronoflow.core.datamock.MockDataSource
import net.chronoflow.core.datamock.MockLatency
import net.chronoflow.core.datamock.MockMail
import net.chronoflow.core.model.DataSource
import net.chronoflow.core.model.auth.ResetPasswordLink
import java.io.File
import java.time.Instant
import java.util.Locale
import kotlin.math.roundToLong
import kotlin.random.Random
import kotlin.time.DurationUnit

class PerfMockSources(
    private val io: CoroutineDispatcher,
    private val debugAppSeed: Boolean = BuildConfig.DEBUG,
) : MockSources {
    override suspend fun latestVerificationLink(
        source: DataSource,
        context: net.chronoflow.feature.auth.model.VerificationContext,
    ): net.chronoflow.core.model.api.SignedVerificationInput? =
        (
            (source as? MockDataSource)?.inbox()?.lastOrNull {
                it is MockMail.Verification && it.to == context.email && it.input.id == context.accountId
            } as? MockMail.Verification
        )?.input

    override suspend fun latestResetLink(
        source: DataSource,
        email: String,
    ): ResetPasswordLink? {
        val normalized = email.trim().lowercase(Locale.ROOT)
        val event =
            (source as? MockDataSource)?.inbox()?.lastOrNull {
                it is MockMail.PasswordReset && it.to.lowercase(Locale.ROOT) == normalized
            } as? MockMail.PasswordReset
        return event?.input?.let { ResetPasswordLink(it.token, it.email) }
    }

    override fun dataSource(
        root: File,
        overrides: GraphOverrides,
        bearer: suspend () -> String?,
    ): DataSource =
        MockDataSource(
            store = FileMockStateStore(root),
            mutationsOffline = overrides.mutationsOffline,
            seed =
                (
                    overrides.mockSeed ?: if (PerfJourneyState.current.demoAccount) {
                        MockSeedFlags(unverified = false, walkthroughSeen = true, termsPending = false)
                    } else {
                        null
                    }
                )?.let {
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
            sleep =
                overrides.mockDelay?.let { MockLatency.exact(it.toDouble(DurationUnit.SECONDS)) }
                    ?: { seconds -> delay((seconds * 1000).roundToLong()) },
            bearer = bearer,
            debugAppSeed = debugAppSeed,
            io = io,
        )
}
