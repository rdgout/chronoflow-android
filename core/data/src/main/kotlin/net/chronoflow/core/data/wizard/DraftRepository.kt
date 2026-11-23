package net.chronoflow.core.data.wizard

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.store.deleteDraft
import net.chronoflow.core.database.store.fetchDraft
import net.chronoflow.core.database.store.fetchListedDrafts
import net.chronoflow.core.database.store.insertDraft
import net.chronoflow.core.database.store.saveDraft
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.store.DraftRecord
import net.chronoflow.core.wizard.model.MemberWizardRecord
import net.chronoflow.core.wizard.model.RecoveryRejected
import java.time.Instant
import java.time.format.DateTimeParseException

class DraftRepository(
    val database: AccountDatabase,
) {
    suspend fun drafts(): List<DraftRecord> = database.read { it.fetchListedDrafts() }

    fun values(): Flow<List<DraftRecord>> = database.observe("drafts") { it.fetchListedDrafts() }

    suspend fun upsert(record: DraftRecord) = database.write { it.saveDraft(record) }

    suspend fun remove(
        id: String,
        permit: SessionMutationPermit? = null,
    ) = database.write(permit) { it.deleteDraft(id) }

    suspend fun recovery(
        now: Instant,
        permit: SessionMutationPermit? = null,
    ): DraftRecord? =
        database.write(permit) { sql ->
            val record = sql.fetchDraft(MemberWizardRecord.ID) ?: return@write null
            val updated =
                try {
                    Instant.parse(record.updatedAt).toEpochMilli()
                } catch (_: DateTimeParseException) {
                    null
                } catch (_: ArithmeticException) {
                    null
                }
            if (updated == null || MemberWizardRecord.isExpired(updated, now.toEpochMilli()) ||
                record.wizardState == null
            ) {
                sql.deleteDraft(record.id)
                null
            } else {
                record
            }
        }

    suspend fun createRecovery(
        record: DraftRecord,
        permit: SessionMutationPermit? = null,
    ) = database.write(permit) { sql ->
        if (record.id != MemberWizardRecord.ID || sql.fetchDraft(record.id) != null) throw RecoveryRejected()
        sql.insertDraft(record)
    }

    suspend fun replaceRecovery(
        record: DraftRecord,
        permit: SessionMutationPermit? = null,
    ) = database.write(permit) { sql ->
        if (record.id != MemberWizardRecord.ID) throw RecoveryRejected()
        sql.saveDraft(record)
    }

    suspend fun updateRecovery(
        record: DraftRecord,
        instance: String,
        revision: Int,
        permit: SessionMutationPermit? = null,
    ) = database.write(permit) { sql ->
        if (record.id != MemberWizardRecord.ID) throw RecoveryRejected()
        val current = sql.fetchDraft(record.id) ?: throw RecoveryRejected()
        val state = state(current) ?: throw RecoveryRejected()
        if ((state["instance"] as? JsonPrimitive)?.content != instance) throw RecoveryRejected()
        val previous = (state["revision"] as? JsonPrimitive)?.content?.toBigDecimalOrNull() ?: 0.toBigDecimal()
        if (previous > revision.toBigDecimal()) return@write
        sql.saveDraft(record)
    }

    suspend fun deleteRecovery(
        instance: String,
        permit: SessionMutationPermit? = null,
    ) = database.write(permit) { sql ->
        val current = sql.fetchDraft(MemberWizardRecord.ID) ?: return@write
        if ((state(current)?.get("instance") as? JsonPrimitive)?.content == instance) sql.deleteDraft(current.id)
    }

    private fun state(record: DraftRecord): JsonObject? =
        try {
            record.wizardState?.let { ApiJson.response.parseToJsonElement(it) as? JsonObject }
        } catch (_: SerializationException) {
            null
        }
}
