package net.chronoflow.core.data.repository

import kotlinx.coroutines.flow.Flow
import net.chronoflow.core.data.store.StoreApplication
import net.chronoflow.core.data.store.encoded
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.AccountSql
import net.chronoflow.core.database.store.deleteTrip
import net.chronoflow.core.database.store.fetchCheckMarks
import net.chronoflow.core.database.store.fetchPlanVersion
import net.chronoflow.core.database.store.fetchTrip
import net.chronoflow.core.database.store.fetchTrips
import net.chronoflow.core.database.store.saveCheckMark
import net.chronoflow.core.database.store.savePlanVersion
import net.chronoflow.core.database.store.saveTrip
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.AdaptationView
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetailResponse
import net.chronoflow.core.model.contracts.TripListItem
import net.chronoflow.core.model.contracts.TripStatus
import net.chronoflow.core.model.store.CheckMarkRecord
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.PlanVersionRecord
import net.chronoflow.core.model.store.TripRecord

class TripRepository(
    val database: AccountDatabase,
) {
    suspend fun trips(): List<TripRecord> = database.read { it.fetchTrips() }

    fun values(): Flow<List<TripRecord>> = database.observe("trips") { it.fetchTrips() }

    suspend fun upsert(trip: TripRecord) = database.write { it.saveTrip(trip) }

    suspend fun remove(id: String) = database.write { it.deleteTrip(id) }

    suspend fun plan(
        tripId: String,
        inputHash: String,
    ): PlanVersionRecord? =
        database.read {
            it.fetchPlanVersion(tripId, inputHash)
        }

    suspend fun checks(
        tripId: String,
        inputHash: String,
    ): List<CheckMarkRecord> =
        database.read {
            it.fetchCheckMarks(tripId, inputHash)
        }

    fun planValues(
        tripId: String,
        inputHash: String,
    ): Flow<PlanVersionRecord?> =
        database.observe("plan_versions") {
            it.fetchPlanVersion(tripId, inputHash)
        }

    fun checkValues(
        tripId: String,
        inputHash: String,
    ): Flow<List<CheckMarkRecord>> =
        database.observe("check_marks") {
            it.fetchCheckMarks(tripId, inputHash)
        }

    suspend fun store(
        plan: PlanVersionRecord,
        checks: List<CheckMarkRecord>,
    ) = database.write { sql ->
        sql.savePlanVersion(plan)
        checks.forEach { sql.saveCheckMark(it) }
    }

    companion object {
        private fun cardJSON(value: TripCardView): String {
            val json = ApiJson.response.encodeToJsonElement(TripCardView.serializer(), value)
            return encoded(json)
        }

        fun applyCards(
            items: List<TripListItem>,
            at: String,
            sql: AccountSql,
        ) {
            val ids = items.map { it.card.id }.toSet()
            if (ids.size != items.size ||
                items.any { it.card.id.isEmpty() || it.card.departureUtc.isEmpty() }
            ) {
                throw DataFailure.Server(null)
            }
            sql.fetchTrips().filter { it.id !in ids && it.deletedAt == null }.forEach {
                sql.saveTrip(
                    it.copy(deletedAt = at),
                )
            }
            val pending = StoreApplication.pending(sql)
            items.forEach { item ->
                val id = item.card.id
                val trip =
                    sql.fetchTrip(id)
                        ?: TripRecord(
                            id,
                            cardJSON(item.card),
                            departureUtc = item.card.departureUtc,
                        )
                if (trip.deletedLocally) return@forEach
                var card = item.card
                if (pending.any { it.kind == "complete-trip" && OutboxPayload.referencedTripId(it) == id }) {
                    card = card.copy(progress = card.progress.copy(status = TripStatus.Completed), canComplete = false)
                }
                sql.saveTrip(
                    trip.copy(
                        card = cardJSON(card),
                        adaptation =
                            item.adaptation?.let {
                                val json = ApiJson.response.encodeToJsonElement(AdaptationView.serializer(), it)
                                encoded(json)
                            },
                        currentInputHash = item.planInputHash,
                        keepOffline = item.keepOffline,
                        departureUtc = card.departureUtc,
                        planStartUtc = item.planStartUtc,
                        planStartZone = item.planStartZone,
                        planEndUtc = item.planEndUtc,
                        planEndDate = item.planEndDate,
                        preparationDays = item.preparationDays,
                        deletedAt = null,
                    ),
                )
                item.checkState?.let { StoreApplication.checks(it, id, sql) }
            }
        }

        fun applyDetail(
            response: TripDetailResponse,
            tripId: String,
            at: String,
            sql: AccountSql,
        ) {
            val value = response.detail
            val hash = value.plan.meta.inputHash
            if (value.trip.id != tripId || hash.isEmpty() ||
                hash != value.checkState.planInputHash
            ) {
                throw DataFailure.Server(null)
            }
            val trip = sql.fetchTrip(tripId) ?: return
            if (trip.deletedLocally || trip.deletedAt != null) return
            sql.savePlanVersion(PlanVersionRecord(tripId, hash, response.data, at, value.detailVersion))
            sql.saveTrip(trip.copy(currentInputHash = hash))
            StoreApplication.checks(value.checkState, tripId, sql)
        }

        fun evictPlans(
            held: Set<String>,
            sql: AccountSql,
        ) {
            val pending = StoreApplication.pending(sql).mapNotNull { OutboxPayload.referencedTripId(it) }.toSet()
            sql.fetchTrips().forEach { trip ->
                if (trip.id in pending || trip.id in held ||
                    (trip.deletedAt == null && !trip.deletedLocally && trip.keepOffline)
                ) {
                    return@forEach
                }
                sql.queries.planVersionsQueries.deletePlanVersionsOfTrip(trip.id)
                sql.queries.checkMarksQueries.deleteCheckMarksOfTrip(trip.id)
                if (trip.deletedAt != null ||
                    trip.deletedLocally
                ) {
                    sql.saveTrip(trip.copy(currentInputHash = null, keepOffline = false))
                }
            }
        }
    }
}
