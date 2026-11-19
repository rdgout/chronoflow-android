package net.chronoflow.core.data.plan

import net.chronoflow.core.data.repository.OutboxRepository
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.model.session.SessionMutationPermit

class CheckOffWriter(
    private val enqueue: suspend (CheckOffChange) -> Unit,
) {
    constructor(outbox: OutboxRepository, permit: SessionMutationPermit?) : this({ change ->
        outbox.enqueue(OutboxChange.CheckOff(change), permit)
    })

    suspend fun setCheck(
        tripId: String,
        detail: TripDetail,
        blockId: String,
        status: CheckStatus?,
    ) {
        val block = detail.plan.blocks.firstOrNull { it.id == blockId } ?: return
        if (!block.actionable) return
        val key = block.checkKey ?: return
        enqueue(CheckOffChange(tripId, detail.plan.meta.inputHash, key.type, key.localDate, key.ordinal, status))
    }
}
