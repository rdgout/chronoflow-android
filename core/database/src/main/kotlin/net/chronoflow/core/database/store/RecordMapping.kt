package net.chronoflow.core.database.store

import net.chronoflow.core.database.Account
import net.chronoflow.core.database.AccountSql
import net.chronoflow.core.database.Outbox
import net.chronoflow.core.database.Settings
import net.chronoflow.core.database.Trips
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.JSONDocument
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.model.store.TripRecord

private fun Boolean.sql(): Long = if (this) 1L else 0L

private fun Account.record(): AccountRecord =
    AccountRecord(
        id = id,
        name = name,
        email = email,
        hasVerifiedEmail = has_verified_email != 0L,
        syncedAt = synced_at,
        walkthroughSeen = walkthrough_seen != 0L,
    )

fun AccountSql.fetchAccount(id: String): AccountRecord? =
    queries.accountQueries
        .selectAccount(id)
        .executeAsOneOrNull()
        ?.record()

fun AccountSql.saveAccount(record: AccountRecord) {
    queries.accountQueries.updateAccount(
        record.name,
        record.email,
        record.hasVerifiedEmail.sql(),
        record.syncedAt,
        record.walkthroughSeen.sql(),
        record.id,
    )
    if (queries.accountQueries.changes().executeAsOne() == 0L) {
        queries.accountQueries.insertAccount(
            record.id,
            record.name,
            record.email,
            record.hasVerifiedEmail.sql(),
            record.syncedAt,
            record.walkthroughSeen.sql(),
        )
    }
}

private fun Trips.record(): TripRecord =
    TripRecord(
        id = id,
        card = card,
        adaptation = adaptation,
        form = form,
        revision = revision,
        currentInputHash = current_input_hash,
        keepOffline = keep_offline != 0L,
        departureUtc = departure_utc,
        planStartUtc = plan_start_utc,
        planStartZone = plan_start_zone,
        preparationDays = preparation_days?.toInt(),
        planEndUtc = plan_end_utc,
        planEndDate = plan_end_date,
        deletedAt = deleted_at,
        deletedLocally = deleted_locally != 0L,
    )

fun AccountSql.fetchTrip(id: String): TripRecord? =
    queries.tripsQueries
        .selectTrip(id)
        .executeAsOneOrNull()
        ?.record()

fun AccountSql.fetchTrips(): List<TripRecord> =
    queries.tripsQueries
        .selectTrips()
        .executeAsList()
        .map { it.record() }

fun AccountSql.saveTrip(record: TripRecord) {
    JSONDocument.requireValid(record.card)
    record.adaptation?.let(JSONDocument::requireValid)
    record.form?.let(JSONDocument::requireValid)
    record.revision?.let(JSONDocument::requireValid)
    queries.tripsQueries.updateTrip(
        record.card,
        record.adaptation,
        record.form,
        record.revision,
        record.currentInputHash,
        record.keepOffline.sql(),
        record.departureUtc,
        record.planStartUtc,
        record.planStartZone,
        record.preparationDays?.toLong(),
        record.planEndUtc,
        record.planEndDate,
        record.deletedAt,
        record.deletedLocally.sql(),
        record.id,
    )
    if (queries.accountQueries.changes().executeAsOne() == 0L) {
        queries.tripsQueries.insertTrip(
            record.id,
            record.card,
            record.adaptation,
            record.form,
            record.revision,
            record.currentInputHash,
            record.keepOffline.sql(),
            record.departureUtc,
            record.planStartUtc,
            record.planStartZone,
            record.preparationDays?.toLong(),
            record.planEndUtc,
            record.planEndDate,
            record.deletedAt,
            record.deletedLocally.sql(),
        )
    }
}

fun AccountSql.deleteTrip(id: String) {
    queries.tripsQueries.deleteTrip(id)
}

private fun Settings.record(): SettingsRecord =
    SettingsRecord(
        section = section,
        value = value_,
    )

fun AccountSql.fetchSetting(section: String): SettingsRecord? =
    queries.settingsQueries
        .selectSetting(section)
        .executeAsOneOrNull()
        ?.record()

fun AccountSql.fetchSettings(): List<SettingsRecord> =
    queries.settingsQueries.selectSettings().executeAsList().map {
        it.record()
    }

fun AccountSql.saveSetting(record: SettingsRecord) {
    JSONDocument.requireValid(record.value)
    queries.settingsQueries.updateSetting(
        record.value,
        record.section,
    )
    if (queries.accountQueries.changes().executeAsOne() == 0L) {
        queries.settingsQueries.insertSetting(
            record.section,
            record.value,
        )
    }
}

fun AccountSql.deleteSetting(section: String) {
    queries.settingsQueries.deleteSetting(section)
}

private fun Outbox.record(): OutboxRecord =
    OutboxRecord(
        id = id,
        kind = kind,
        payload = payload,
        createdAt = created_at,
        attempts = attempts.toInt(),
        lastError = last_error,
    )

fun AccountSql.fetchOutboxRow(id: String): OutboxRecord? =
    queries.outboxQueries
        .selectOutboxRow(id)
        .executeAsOneOrNull()
        ?.record()

fun AccountSql.fetchPendingOutbox(): List<OutboxRecord> =
    queries.outboxQueries.selectPendingOutbox().executeAsList().map {
        it.record()
    }

fun AccountSql.saveOutboxRow(record: OutboxRecord) {
    JSONDocument.requireValid(record.payload)
    queries.outboxQueries.updateOutboxRow(
        record.kind,
        record.payload,
        record.createdAt,
        record.attempts.toLong(),
        record.lastError,
        record.id,
    )
    if (queries.accountQueries.changes().executeAsOne() == 0L) {
        queries.outboxQueries.insertOutboxRow(
            record.id,
            record.kind,
            record.payload,
            record.createdAt,
            record.attempts.toLong(),
            record.lastError,
        )
    }
}

fun AccountSql.deleteOutboxRow(id: String) {
    queries.outboxQueries.deleteOutboxRow(id)
}
