package net.chronoflow.core.data.reminders

import net.chronoflow.core.reminders.ReminderRequest
import java.time.Instant

data class ScheduledReminderRow(
    val identifier: String,
    val requestCode: Int,
    val sendAt: Instant,
    val expiresAt: Instant,
    val title: String,
    val body: String,
    val tripId: String,
    val blockId: String,
    val inputHash: String,
    val memberKeys: List<String>,
    val accountId: String?,
) {
    fun toRequest(): ReminderRequest =
        ReminderRequest(identifier, title, body, sendAt, tripId, blockId, inputHash, memberKeys, accountId, expiresAt)

    fun sameContent(request: ReminderRequest): Boolean = toRequest() == request

    companion object {
        fun of(
            request: ReminderRequest,
            requestCode: Int,
        ): ScheduledReminderRow =
            ScheduledReminderRow(
                request.identifier,
                requestCode,
                request.sendAt,
                request.expiresAt,
                request.title,
                request.body,
                request.tripID,
                request.blockID,
                request.inputHash,
                request.memberKeys,
                request.accountID,
            )
    }
}
