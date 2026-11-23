package net.chronoflow.core.reminders

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import net.chronoflow.core.model.api.ApiJson
import java.time.Instant

data class ReminderLedgerEntry(
    val tripId: String,
    val inputHash: String,
    val sendAtMs: Double,
    val memberKeys: List<String>,
) {
    companion object
}

object ReminderLedger {
    fun decode(data: String): List<ReminderLedgerEntry> {
        val rows =
            try {
                ApiJson.response.parseToJsonElement(data) as? JsonArray
            } catch (_: SerializationException) {
                null
            } ?: return emptyList()
        return rows.mapNotNull { element ->
            val row = element as? JsonObject ?: return@mapNotNull null
            val trip = (row["tripId"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return@mapNotNull null
            val hash = (row["inputHash"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return@mapNotNull null
            val number =
                (row["sendAtMs"] as? JsonPrimitive)
                    ?.takeUnless { it.isString }
                    ?.doubleOrNull
                    ?.takeIf { it.isFinite() } ?: return@mapNotNull null
            val keys = row["memberKeys"] as? JsonArray ?: return@mapNotNull null
            val strings =
                keys.map { key ->
                    (key as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return@mapNotNull null
                }
            ReminderLedgerEntry(trip, hash, number, strings)
        }
    }

    fun elapsed(
        rows: List<ReminderLedgerEntry>,
        now: Instant,
    ): List<ReminderLedgerEntry> {
        val milliseconds = now.epochSecond * 1000.0 + now.nano / 1_000_000.0
        return rows.filter {
            it.sendAtMs >= milliseconds - REMINDER_LOOKBACK_SECONDS * 1000 &&
                it.sendAtMs <= milliseconds
        }
    }
}

fun ReminderLedgerEntry.Companion.from(request: ReminderRequest): ReminderLedgerEntry =
    ReminderLedgerEntry(
        request.tripID,
        request.inputHash,
        request.sendAt.epochSecond * 1000.0 + request.sendAt.nano / 1_000_000.0,
        request.memberKeys,
    )
