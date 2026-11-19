package net.chronoflow.app.today

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import net.chronoflow.app.SessionGraph
import net.chronoflow.core.data.plan.CheckOffWriter
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.plan.PlanDecoder
import net.chronoflow.feature.today.data.TodaySource

class DbTodaySource(
    private val session: SessionGraph,
    private val io: CoroutineDispatcher,
    private val decoder: PlanDecoder,
    private val mutationPermit: () -> SessionMutationPermit,
) : TodaySource {
    override fun synced() =
        session.accounts
            .values()
            .map { it?.syncedAt != null }
            .flowOn(io)
            .conflate()

    override fun preferences() =
        session.settings
            .planPreferences()
            .flowOn(io)
            .conflate()

    override fun trips() =
        session.trips
            .values()
            .flowOn(io)
            .conflate()

    override fun detail(tripId: String) =
        session.trips
            .detailValues(tripId)
            .map { snapshot ->
                try {
                    decoder.detail(snapshot)
                } catch (
                    _: SerializationException,
                ) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
            }.flowOn(io)
            .conflate()

    override suspend fun setCheck(
        tripId: String,
        detail: TripDetail,
        blockId: String,
        status: CheckStatus?,
    ) {
        withContext(io) { CheckOffWriter(session.outbox, mutationPermit()).setCheck(tripId, detail, blockId, status) }
    }
}
