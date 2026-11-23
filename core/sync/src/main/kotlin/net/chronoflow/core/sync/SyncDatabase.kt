package net.chronoflow.core.sync

import kotlinx.coroutines.flow.Flow
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.MutationReceipt
import net.chronoflow.core.model.api.AccountIdentity
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.contracts.TripDetailResponse
import net.chronoflow.core.model.contracts.TripListItem
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.TripRecord

interface SyncDatabase {
    val accountID: AccountID?

    suspend fun validate(epoch: SyncEpoch)

    suspend fun account(): AccountRecord?

    suspend fun pending(): List<OutboxRecord>

    fun outboxValues(): Flow<List<OutboxRecord>>

    suspend fun outboxRow(id: String): OutboxRecord?

    suspend fun trip(id: String): TripRecord?

    suspend fun snapshot(): SyncSnapshot

    suspend fun applyIdentity(
        epoch: SyncEpoch,
        identity: AccountIdentity,
    )

    suspend fun markEmailUnverified(epoch: SyncEpoch)

    suspend fun applyPull(
        epoch: SyncEpoch,
        account: AccountSnapshot,
        cards: List<TripListItem>,
        at: String,
        retention: PlanRetention,
    )

    suspend fun detailNeeded(
        item: TripListItem,
        retention: PlanRetention,
    ): Boolean

    suspend fun applyDetail(
        epoch: SyncEpoch,
        response: TripDetailResponse,
        tripId: String,
        at: String,
    )

    suspend fun applyReceipt(
        epoch: SyncEpoch,
        receipt: MutationReceipt,
        row: OutboxRecord,
    )

    suspend fun refuse(
        epoch: SyncEpoch,
        row: OutboxRecord,
        refusal: SyncRefusal,
    )

    suspend fun recordTransientFailure(
        epoch: SyncEpoch,
        rowId: String,
        lastError: String,
    ): Int

    suspend fun deleteOutboxRow(
        epoch: SyncEpoch,
        id: String,
    )

    suspend fun rekeyCheckOff(
        epoch: SyncEpoch,
        row: OutboxRecord,
        change: CheckOffChange,
    ): OutboxRecord?
}

fun interface ActiveSyncDatabase {
    suspend fun activeDatabase(): SyncDatabase?
}
