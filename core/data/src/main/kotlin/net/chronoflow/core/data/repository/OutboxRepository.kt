package net.chronoflow.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.store.StoreApplication
import net.chronoflow.core.data.store.encoded
import net.chronoflow.core.data.store.objectValue
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.store.deleteOutboxRow
import net.chronoflow.core.database.store.deleteSetting
import net.chronoflow.core.database.store.fetchAccount
import net.chronoflow.core.database.store.fetchPendingOutbox
import net.chronoflow.core.database.store.fetchSetting
import net.chronoflow.core.database.store.fetchTrip
import net.chronoflow.core.database.store.saveAccount
import net.chronoflow.core.database.store.saveOutboxRow
import net.chronoflow.core.database.store.saveSetting
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.model.device.UUIDv7
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.store.LocalMutationFailure
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.model.store.StoreFailure
import net.chronoflow.core.model.store.StoreTime
import java.security.SecureRandom
import java.time.Instant
import java.util.Locale

class OutboxRepository(
    val database: AccountDatabase,
) {
    suspend fun pending(): List<OutboxRecord> = database.read { it.fetchPendingOutbox() }

    fun values(): Flow<List<OutboxRecord>> = database.observe("outbox") { it.fetchPendingOutbox() }

    suspend fun upsert(record: OutboxRecord) = database.write { it.saveOutboxRow(record) }

    suspend fun remove(id: String) = database.write { it.deleteOutboxRow(id) }

    suspend fun enqueue(
        change: OutboxChange,
        permit: SessionMutationPermit? = null,
        now: Instant = Instant.now(),
        randomBytes: ByteArray = ByteArray(10).also { SecureRandom().nextBytes(it) },
    ): OutboxRecord {
        val ms = now.toEpochMilli()
        if (ms < 0 || ms >= (1L shl 48)) throw LocalMutationFailure.InvalidPayload
        val id = UUIDv7.generate(ms, randomBytes).toString().lowercase(Locale.ROOT)
        val createdAt = StoreTime.millis(now)
        val initial = OutboxPayload.record(change, id, createdAt)
        val accountID = database.accountID ?: throw StoreFailure.Closed
        return database.write(permit) { sql ->
            var row = initial
            val account = sql.fetchAccount(accountID.rawValue) ?: throw LocalMutationFailure.MissingAccount
            when (change) {
                is OutboxChange.CheckOff -> {
                    val trip = sql.fetchTrip(change.change.tripID)
                    if (trip == null || trip.deletedLocally || trip.deletedAt != null) {
                        throw LocalMutationFailure.MissingTrip
                    }
                    StoreApplication.setMark(change.change, createdAt, sql)
                }

                is OutboxChange.Settings -> {
                    val stored = sql.fetchSetting(change.section.raw)?.let { objectValue(it.value) }
                    val value = (stored ?: JsonObject(emptyMap())).toMutableMap()
                    val invalid = (value["invalidDrafts"] as? JsonObject ?: JsonObject(emptyMap())).toMutableMap()
                    change.patch.forEach { (key, entry) ->
                        invalid.remove(key)
                        value[ProfilePreferences.camel(key)] = entry
                    }
                    var merged = JsonObject(value)
                    if (change.section == SettingsSection.BodyClock) {
                        value["invalidDrafts"] = JsonObject(invalid)
                        merged = ProfilePreferences.bodyClock(JsonObject(value))
                        if ("habitual_bed" in change.patch || "habitual_wake" in change.patch) {
                            sql.saveSetting(SettingsRecord("sleepAnswered", "true"))
                        }
                    }
                    sql.saveSetting(SettingsRecord(change.section.raw, encoded(merged)))
                    if (change.section == SettingsSection.BodyClock) sql.deleteSetting("readiness")
                }

                is OutboxChange.Policies -> {
                    val stored = sql.fetchSetting("policies") ?: throw LocalMutationFailure.InvalidPayload
                    val value = objectValue(stored.value)
                    if (value["termsVersion"] != JsonPrimitive(change.version) || change.version.isEmpty()) {
                        throw LocalMutationFailure.InvalidPayload
                    }
                    val pending =
                        sql.fetchPendingOutbox().firstOrNull {
                            if (OutboxPayload.dependency(it) != "settings:policies") return@firstOrNull false
                            val pendingChange =
                                try {
                                    OutboxPayload.change(it)
                                } catch (_: LocalMutationFailure) {
                                    null
                                }
                            pendingChange == change
                        }
                    val next =
                        value + ("acceptedTermsVersion" to JsonPrimitive(change.version)) +
                            ("acceptedTermsAt" to JsonPrimitive(pending?.createdAt ?: createdAt))
                    sql.saveSetting(SettingsRecord("policies", encoded(JsonObject(next))))
                    if (pending != null) return@write pending
                }

                is OutboxChange.Profile -> {
                    if (change.change.email.lowercase(Locale.ROOT) != account.email.lowercase(Locale.ROOT)) {
                        throw LocalMutationFailure.OnlineOnly
                    }
                    val payload = objectValue(row.payload) + ("previousName" to JsonPrimitive(account.name))
                    row = row.copy(payload = encoded(JsonObject(payload)))
                    sql.saveAccount(account.copy(name = change.change.name))
                }

                OutboxChange.Walkthrough -> {
                    sql.saveAccount(account.copy(walkthroughSeen = true))
                    sql.fetchPendingOutbox().firstOrNull { it.kind == "walkthrough" }?.let { return@write it }
                }

                is OutboxChange.Unsupported -> {
                    throw LocalMutationFailure.Unsupported
                }
            }
            sql.queries.outboxQueries.insertOutboxRow(
                row.id,
                row.kind,
                row.payload,
                row.createdAt,
                row.attempts.toLong(),
                row.lastError,
            )
            row
        }
    }
}
