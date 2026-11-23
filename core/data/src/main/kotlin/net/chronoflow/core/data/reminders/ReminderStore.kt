package net.chronoflow.core.data.reminders

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.reminders.ReminderLedgerEntry
import java.time.Instant
import kotlin.math.floor

object ReminderStore {
    suspend fun rows(db: AccountDatabase): List<ScheduledReminderRow> =
        db.read { sql ->
            sql.queries.scheduledReminderQueries.selectScheduledReminders().executeAsList().map { row ->
                ScheduledReminderRow(
                    row.identifier,
                    row.request_code.toInt(),
                    Instant.ofEpochMilli(row.send_at),
                    Instant.ofEpochMilli(row.expires_at),
                    row.title,
                    row.body,
                    row.trip_id,
                    row.block_id,
                    row.input_hash,
                    memberKeys(row.member_keys),
                    row.account_id,
                )
            }
        }

    suspend fun save(
        db: AccountDatabase,
        row: ScheduledReminderRow,
    ) {
        db.write { sql ->
            val queries = sql.queries.scheduledReminderQueries
            val keys = ApiJson.request.encodeToString(row.memberKeys)
            queries.updateScheduledReminder(
                row.requestCode.toLong(),
                row.sendAt.toEpochMilli(),
                row.expiresAt.toEpochMilli(),
                row.title,
                row.body,
                row.tripId,
                row.blockId,
                row.inputHash,
                keys,
                row.accountId,
                row.identifier,
            )
            if (sql.queries.accountQueries
                    .changes()
                    .executeAsOne() == 0L
            ) {
                queries.insertScheduledReminder(
                    row.identifier,
                    row.requestCode.toLong(),
                    row.sendAt.toEpochMilli(),
                    row.expiresAt.toEpochMilli(),
                    row.title,
                    row.body,
                    row.tripId,
                    row.blockId,
                    row.inputHash,
                    keys,
                    row.accountId,
                )
            }
        }
    }

    suspend fun delete(
        db: AccountDatabase,
        identifier: String,
    ) {
        db.write { it.queries.scheduledReminderQueries.deleteScheduledReminder(identifier) }
    }

    suspend fun deleteAll(db: AccountDatabase) {
        db.write { it.queries.scheduledReminderQueries.deleteScheduledReminders() }
    }

    suspend fun ledger(db: AccountDatabase): List<ReminderLedgerEntry> =
        db.read { sql ->
            sql.queries.reminderLedgerQueries.selectReminderLedger().executeAsList().mapNotNull { row ->
                try {
                    ReminderLedgerEntry(
                        row.trip_id,
                        row.input_hash,
                        row.send_at_ms.toDouble(),
                        memberKeys(row.member_keys),
                    )
                } catch (_: SerializationException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        }

    suspend fun replaceLedger(
        db: AccountDatabase,
        entries: List<ReminderLedgerEntry>,
    ) {
        db.write { sql ->
            val queries = sql.queries.reminderLedgerQueries
            queries.deleteReminderLedger()
            entries.forEach { entry ->
                queries.insertReminderLedger(
                    entry.tripId,
                    entry.inputHash,
                    floor(entry.sendAtMs).toLong(),
                    ApiJson.request.encodeToString(entry.memberKeys),
                )
            }
        }
    }

    suspend fun clearLedger(db: AccountDatabase) {
        db.write { it.queries.reminderLedgerQueries.deleteReminderLedger() }
    }

    private fun memberKeys(json: String): List<String> {
        val array = ApiJson.response.parseToJsonElement(json) as? JsonArray ?: throw IllegalArgumentException()
        return array.map {
            val value = it as? JsonPrimitive ?: throw IllegalArgumentException()
            require(value.isString)
            value.content
        }
    }
}
