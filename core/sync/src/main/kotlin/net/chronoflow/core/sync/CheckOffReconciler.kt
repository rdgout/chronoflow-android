package net.chronoflow.core.sync

import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.session.SyncEpoch
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.OutboxRecord

class CheckOffReconciler(
    val pull: FullPull,
) {
    suspend fun prepare(
        row: OutboxRecord,
        epoch: SyncEpoch,
        refresh: Boolean = false,
    ): OutboxRecord? {
        epoch.check()
        val change = (OutboxPayload.change(row) as? OutboxChange.CheckOff)?.change ?: return row
        val trip = pull.database.trip(change.tripID)
        epoch.check()
        if (trip == null || trip.deletedAt != null || trip.deletedLocally) {
            pull.database.deleteOutboxRow(epoch, row.id)
            epoch.check()
            return null
        }
        if (!refresh && trip.currentInputHash == change.planInputHash) return row
        pull.fetchDetail(change.tripID, epoch)
        epoch.check()
        val rekeyed = pull.database.rekeyCheckOff(epoch, row, change)
        epoch.check()
        return rekeyed
    }
}
