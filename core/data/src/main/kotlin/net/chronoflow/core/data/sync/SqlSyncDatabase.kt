package net.chronoflow.core.data.sync

import kotlinx.coroutines.flow.Flow
import net.chronoflow.core.data.repository.AccountRepository
import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.data.repository.TripRepository
import net.chronoflow.core.data.store.StoreApplication
import net.chronoflow.core.data.store.encoded
import net.chronoflow.core.data.store.json
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.store.deleteOutboxRow
import net.chronoflow.core.database.store.fetchCheckMark
import net.chronoflow.core.database.store.fetchOutboxRow
import net.chronoflow.core.database.store.fetchPlanVersion
import net.chronoflow.core.database.store.fetchTrip
import net.chronoflow.core.database.store.fetchTrips
import net.chronoflow.core.database.store.saveOutboxRow
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.MutationReceipt
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.api.AccountIdentity
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.CheckKey
import net.chronoflow.core.model.contracts.TripDetailResponse
import net.chronoflow.core.model.contracts.TripListItem
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.LocalMutationFailure
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.TripRecord
import net.chronoflow.core.sync.PlanRetention
import net.chronoflow.core.sync.SyncDatabase
import net.chronoflow.core.sync.SyncRefusal
import net.chronoflow.core.sync.SyncSnapshot

class SqlSyncDatabase(
    private val database: AccountDatabase,
) : SyncDatabase {
    override val accountID: AccountID? get() = database.accountID

    override suspend fun validate(epoch: SyncEpoch) = database.validate(epoch)

    override suspend fun account(): AccountRecord? = AccountRepository(database).account()

    override suspend fun pending(): List<OutboxRecord> = OutboxRepository(database).pending()

    override fun outboxValues(): Flow<List<OutboxRecord>> = OutboxRepository(database).values()

    override suspend fun outboxRow(id: String): OutboxRecord? = database.read { it.fetchOutboxRow(id) }

    override suspend fun trip(id: String): TripRecord? = database.read { it.fetchTrip(id) }

    override suspend fun snapshot(): SyncSnapshot =
        database.read { sql ->
            val versions =
                sql.queries.planVersionsQueries.selectCurrentPlanVersions().executeAsList().associate {
                    it.trip_id to
                        it.detail_version
                }
            SyncSnapshot(
                sql.fetchTrips().filter { !it.deletedLocally && it.deletedAt == null }.associate { trip ->
                    trip.id to
                        SyncSnapshot.Value(
                            trip.copy(
                                keepOffline = false,
                                card = encoded(json(trip.card)),
                                adaptation = trip.adaptation?.let { encoded(json(it)) },
                                form = trip.form?.let { encoded(json(it)) },
                                revision = trip.revision?.let { encoded(json(it)) },
                            ),
                            versions[trip.id],
                        )
                },
            )
        }

    override suspend fun applyIdentity(
        epoch: SyncEpoch,
        identity: AccountIdentity,
    ) = database.write(epoch) {
        StoreApplication.identity(identity, epoch.accountID, it)
    }

    override suspend fun markEmailUnverified(epoch: SyncEpoch) =
        database.write<Unit>(epoch) {
            it.queries.accountQueries.setEmailUnverified(epoch.accountID.rawValue)
        }

    override suspend fun applyPull(
        epoch: SyncEpoch,
        account: AccountSnapshot,
        cards: List<TripListItem>,
        at: String,
        retention: PlanRetention,
    ) = database.write(epoch) { sql ->
        StoreApplication.snapshot(account, epoch.accountID, sql)
        TripRepository.applyCards(cards, at, sql)
        sql.queries.accountQueries.setSyncedAt(at, epoch.accountID.rawValue)
        retention.withHeldPlans { TripRepository.evictPlans(it, sql) }
    }

    override suspend fun detailNeeded(
        item: TripListItem,
        retention: PlanRetention,
    ): Boolean =
        database.read { sql ->
            val trip = sql.fetchTrip(item.card.id)
            if (trip == null || trip.deletedLocally || trip.deletedAt != null) return@read false
            if (!trip.keepOffline && !retention.withHeldPlans { trip.id in it }) return@read false
            val stored = sql.fetchPlanVersion(trip.id, item.planInputHash)
            stored == null || stored.detailVersion != item.detailVersion
        }

    override suspend fun applyDetail(
        epoch: SyncEpoch,
        response: TripDetailResponse,
        tripId: String,
        at: String,
    ) = database.write(epoch) {
        TripRepository.applyDetail(response, tripId, at, it)
    }

    override suspend fun applyReceipt(
        epoch: SyncEpoch,
        receipt: MutationReceipt,
        row: OutboxRecord,
    ) = database.write(epoch) {
        StoreApplication.receipt(receipt, row, epoch.accountID, it)
    }

    override suspend fun refuse(
        epoch: SyncEpoch,
        row: OutboxRecord,
        refusal: SyncRefusal,
    ) = database.write(epoch) { sql ->
        StoreApplication.setting(
            "syncFailure",
            json(ApiJson.response.encodeToString(SyncRefusal.serializer(), refusal)),
            sql,
        )
        StoreApplication.refuse(row, epoch.accountID, sql)
        if (row.kind == "check-off" && refusal.notice != null) {
            OutboxPayload.referencedTripId(row)?.let {
                sql.queries.planVersionsQueries.clearDetailVersionsOfTrip(it)
            }
        }
    }

    override suspend fun recordTransientFailure(
        epoch: SyncEpoch,
        rowId: String,
        lastError: String,
    ): Int =
        database.write(epoch) { sql ->
            sql.queries.outboxQueries.incrementOutboxAttempts(lastError, rowId)
            sql.queries.outboxQueries
                .selectOutboxAttempts(rowId)
                .executeAsOneOrNull()
                ?.toInt() ?: 0
        }

    override suspend fun deleteOutboxRow(
        epoch: SyncEpoch,
        id: String,
    ) = database.write(epoch) { it.deleteOutboxRow(id) }

    override suspend fun rekeyCheckOff(
        epoch: SyncEpoch,
        row: OutboxRecord,
        change: CheckOffChange,
    ): OutboxRecord? =
        database.write(epoch) { sql ->
            val storedRow = sql.fetchOutboxRow(row.id) ?: throw DataFailure.Server(null)
            val trip = sql.fetchTrip(change.tripID) ?: throw DataFailure.Server(null)
            val hash = trip.currentInputHash ?: throw DataFailure.Server(null)
            val plan = sql.fetchPlanVersion(trip.id, hash) ?: throw DataFailure.Server(null)
            val detail = TripDetailResponse.decode(plan.detail).detail
            val key = CheckKey(change.type, change.localDate, change.ordinal)
            if (detail.plan.blocks.none { it.actionable && it.checkKey == key }) {
                sql.deleteOutboxRow(row.id)
                sql.queries.checkMarksQueries.deleteCheckMarkIfNotNewer(
                    change.tripID,
                    change.planInputHash,
                    change.type.raw,
                    change.localDate,
                    change.ordinal.toLong(),
                    row.createdAt,
                )
                return@write null
            }
            val next = change.copy(planInputHash = hash)
            val rewritten =
                storedRow.copy(
                    payload = OutboxPayload.record(OutboxChange.CheckOff(next), row.id, row.createdAt).payload,
                    attempts = if (hash != change.planInputHash) 0 else storedRow.attempts,
                    lastError = if (hash != change.planInputHash) null else storedRow.lastError,
                )
            sql.saveOutboxRow(rewritten)
            val newer =
                StoreApplication.pending(sql).any { other ->
                    if (other.kind != "check-off" || other.id == row.id ||
                        !(other.createdAt > row.createdAt || (other.createdAt == row.createdAt && other.id > row.id))
                    ) {
                        false
                    } else {
                        val check =
                            try {
                                (OutboxPayload.change(other) as? OutboxChange.CheckOff)?.change
                            } catch (
                                _: LocalMutationFailure,
                            ) {
                                null
                            }
                        check != null && check.tripID == change.tripID && check.type == change.type &&
                            check.localDate == change.localDate && check.ordinal == change.ordinal
                    }
                }
            val mark = sql.fetchCheckMark(change.tripID, hash, change.type.raw, change.localDate, change.ordinal)
            if (!newer &&
                (mark == null || mark.markedAt <= row.createdAt)
            ) {
                StoreApplication.setMark(next, row.createdAt, sql)
            }
            rewritten
        }
}
