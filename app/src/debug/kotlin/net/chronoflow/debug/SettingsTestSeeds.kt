package net.chronoflow.debug

import kotlinx.coroutines.flow.first
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.datamock.MockScenarioValues
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.session.SessionMutationPermit

internal suspend fun seedSettingsPending(
    database: AccountDatabase,
    permit: SessionMutationPermit,
) {
    if (AccountRepository(database).values().first()?.email != MockScenarioValues.demoEmail) return
    val trips = TripRepository(database)
    for (trip in trips.trips()) {
        if (trip.deletedAt != null || trip.deletedLocally) continue
        val hash = trip.currentInputHash ?: continue
        val plan = trips.plan(trip.id, hash) ?: continue
        val detail = ApiJson.response.decodeFromString(TripDetail.serializer(), plan.detail.decodeToString())
        val key =
            detail.plan.blocks
                .firstOrNull { it.actionable && it.checkKey != null }
                ?.checkKey ?: continue
        val outbox = OutboxRepository(database)
        val row =
            outbox.enqueue(
                OutboxChange.CheckOff(
                    CheckOffChange(trip.id, hash, key.type, key.localDate, key.ordinal, CheckStatus.Done),
                ),
                permit,
            )
        check(outbox.pending().any { it.id == row.id }) { "settings pending seed failed: missing pending change" }
        return
    }
    error("settings pending seed failed: no cached actionable step")
}
