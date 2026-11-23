package net.chronoflow.app.wizard

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import net.chronoflow.app.SessionGraph
import net.chronoflow.core.data.wizard.MemberWizardStore
import net.chronoflow.core.data.wizard.storing
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.wizard.generation.TripGenerationEffects
import net.chronoflow.core.wizard.generation.TripGenerationService
import net.chronoflow.core.wizard.model.AirportSearchCache
import net.chronoflow.core.wizard.model.AirportSearchService
import net.chronoflow.core.wizard.model.TripWizardAction
import net.chronoflow.feature.wizard.TripWizardDependencies
import java.time.Instant
import kotlin.math.roundToLong

class SessionTripWizardDependencies(
    private val session: SessionGraph,
    private val permit: SessionMutationPermit,
    private val io: CoroutineDispatcher,
) : TripWizardDependencies {
    override val now: () -> Instant = { Instant.now() }
    override val store = MemberWizardStore(session.database, permit, now, io)
    override val source get() = session.dataSource
    override val airportSearch =
        AirportSearchService(
            source,
            now,
            sleep = { delay((it * 1000).roundToLong()) },
            cache = AirportSearchCache(),
        )
    override val writerScope = CoroutineScope(session.scope.coroutineContext + io)

    override suspend fun start() = store.start()

    override suspend fun startTrip(
        id: String,
        action: TripWizardAction,
    ) = store.startTrip(id, action, source)

    override fun newGeneration() =
        TripGenerationService(
            source,
            TripGenerationEffects.storing(
                session.database,
                source,
                SyncEpoch(AccountID.of(session.snapshot.session.accountId), session.snapshot.epoch),
                permit,
                requestSync = { session.sync.requestSync() },
                now = now,
            ),
            parentContext = session.scope.coroutineContext + io,
        )
}
