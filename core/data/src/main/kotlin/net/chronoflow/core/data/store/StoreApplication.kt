package net.chronoflow.core.data.store

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.database.AccountSql
import net.chronoflow.core.database.store.deleteCheckMark
import net.chronoflow.core.database.store.deleteOutboxRow
import net.chronoflow.core.database.store.deleteSetting
import net.chronoflow.core.database.store.fetchAccount
import net.chronoflow.core.database.store.fetchPendingOutbox
import net.chronoflow.core.database.store.fetchSetting
import net.chronoflow.core.database.store.saveAccount
import net.chronoflow.core.database.store.saveCheckMark
import net.chronoflow.core.database.store.saveOutboxRow
import net.chronoflow.core.database.store.saveSetting
import net.chronoflow.core.model.AccountID
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.MutationReceipt
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.api.AccountIdentity
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.CheckState
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.model.store.AccountRecord
import net.chronoflow.core.model.store.CheckMarkRecord
import net.chronoflow.core.model.store.LocalMutationFailure
import net.chronoflow.core.model.store.OutboxPayload
import net.chronoflow.core.model.store.OutboxRecord
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.model.store.StoreFailure

internal object StoreApplication {
    fun pending(sql: AccountSql): List<OutboxRecord> = sql.fetchPendingOutbox()

    fun identity(
        value: AccountIdentity,
        accountID: AccountID,
        sql: AccountSql,
    ) {
        if (value.id != accountID.rawValue) throw StoreFailure.InvalidAccountID
        val rows = pending(sql)
        var record = sql.fetchAccount(value.id) ?: AccountRecord(value.id, value.name, value.email)
        if (rows.none { it.kind == "profile" }) record = record.copy(name = value.name, email = value.email)
        sql.saveAccount(
            record.copy(
                hasVerifiedEmail = value.hasVerifiedEmail,
                walkthroughSeen = value.walkthroughSeen || rows.any { it.kind == "walkthrough" },
            ),
        )
        setting("policies", json(ApiJson.response.encodeToString(AccountPolicies.serializer(), value.policies)), sql)
    }

    fun snapshot(
        value: AccountSnapshot,
        accountID: AccountID,
        sql: AccountSql,
    ) {
        if (value.id != accountID.rawValue) throw StoreFailure.InvalidAccountID
        val rows = pending(sql)
        var record = sql.fetchAccount(value.id) ?: throw LocalMutationFailure.MissingAccount
        if (rows.none { it.kind == "profile" }) record = record.copy(name = value.name, email = value.email)
        sql.saveAccount(record.copy(hasVerifiedEmail = value.hasVerifiedEmail))
        val settings = value.settings
        listOf(
            "bodyClock" to settings.bodyClock,
            "preferences" to settings.preferences,
            "reminders" to settings.reminders,
            "planCredits" to settings.planCredits,
            "policies" to json(ApiJson.response.encodeToString(AccountPolicies.serializer(), settings.policies)),
            "sleepAnswered" to JsonPrimitive(settings.sleepAnswered),
            "billing" to value.billing,
            "security" to value.security,
        ).forEach { (section, document) -> setting(section, document, sql) }
        if (rows.any { OutboxPayload.dependency(it) == "settings:bodyClock" }) {
            sql.deleteSetting("readiness")
        } else {
            setting("readiness", value.readiness, sql)
        }
    }

    fun setting(
        section: String,
        value: JsonElement,
        sql: AccountSql,
    ) {
        val rows = pending(sql).filter { OutboxPayload.dependency(it) == "settings:$section" }
        val latest = rows.lastOrNull()
        if (section == "policies" && latest != null) {
            val change = OutboxPayload.change(latest)
            if (change is OutboxChange.Policies) {
                val policies = (value as JsonObject).toMutableMap()
                policies["acceptedTermsVersion"] = JsonPrimitive(change.version)
                policies["acceptedTermsAt"] = JsonPrimitive(latest.createdAt)
                sql.saveSetting(SettingsRecord(section, encoded(JsonObject(policies))))
                return
            }
        }
        if (rows.isNotEmpty()) return
        val sanitized =
            when (section) {
                "bodyClock" -> {
                    ProfilePreferences.bodyClock(value as JsonObject)
                }

                "readiness" -> {
                    ProfilePreferences.readiness(
                        value as JsonObject,
                        sql.fetchSetting("bodyClock")?.let { objectValue(it.value) } ?: JsonObject(emptyMap()),
                    )
                }

                else -> {
                    value
                }
            }
        sql.saveSetting(SettingsRecord(section, encoded(sanitized)))
    }

    fun checks(
        value: CheckState,
        tripId: String,
        sql: AccountSql,
    ) {
        if (pending(sql).any { it.kind == "check-off" && OutboxPayload.referencedTripId(it) == tripId }) return
        sql.queries.checkMarksQueries.deleteCheckMarksOfPlan(tripId, value.planInputHash)
        value.marks.forEach { mark ->
            sql.saveCheckMark(
                CheckMarkRecord(
                    tripId,
                    value.planInputHash,
                    mark.key.type.raw,
                    mark.key.localDate,
                    mark.key.ordinal,
                    mark.status.raw,
                    mark.markedAt,
                ),
            )
        }
    }

    fun setMark(
        value: CheckOffChange,
        at: String,
        sql: AccountSql,
    ) {
        val status = value.status
        if (status != null) {
            sql.saveCheckMark(
                CheckMarkRecord(
                    value.tripID,
                    value.planInputHash,
                    value.type.raw,
                    value.localDate,
                    value.ordinal,
                    status.raw,
                    at,
                ),
            )
        } else {
            sql.deleteCheckMark(value.tripID, value.planInputHash, value.type.raw, value.localDate, value.ordinal)
        }
    }

    fun refuse(
        row: OutboxRecord,
        accountID: AccountID,
        sql: AccountSql,
    ) {
        sql.deleteOutboxRow(row.id)
        if (row.kind != "profile") return
        val previous =
            (objectValue(row.payload)["previousName"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return
        val next = pending(sql).firstOrNull { it.kind == "profile" }
        if (next !=
            null
        ) {
            sql.saveOutboxRow(
                next.copy(
                    payload =
                        encoded(
                            JsonObject(objectValue(next.payload) + ("previousName" to JsonPrimitive(previous))),
                        ),
                ),
            )
        } else {
            sql.queries.accountQueries.setAccountName(previous, accountID.rawValue)
        }
    }

    fun receipt(
        value: MutationReceipt,
        row: OutboxRecord,
        accountID: AccountID,
        sql: AccountSql,
    ) {
        sql.deleteOutboxRow(row.id)
        when (value) {
            MutationReceipt.Acknowledged -> { }

            is MutationReceipt.Account -> {
                snapshot(value.snapshot, accountID, sql)
            }

            is MutationReceipt.Identity -> {
                identity(value.identity, accountID, sql)
            }

            is MutationReceipt.Checks -> {
                val tripId = OutboxPayload.referencedTripId(row) ?: throw LocalMutationFailure.InvalidPayload
                checks(value.result.checkState, tripId, sql)
                sql.queries.planVersionsQueries.clearDetailVersionIfDifferent(
                    tripId,
                    value.result.checkState.planInputHash,
                    value.result.detailVersion,
                )
            }
        }
    }
}
