package net.chronoflow.core.model.store

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import net.chronoflow.core.model.CheckOffChange
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.OutboxKind
import net.chronoflow.core.model.ProfileChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.contracts.ProfilePreferences

sealed class LocalMutationFailure : Exception(null, null, false, false) {
    data object Unsupported : LocalMutationFailure()

    data object InvalidPayload : LocalMutationFailure()

    data object MissingAccount : LocalMutationFailure()

    data object MissingTrip : LocalMutationFailure()

    data object OnlineOnly : LocalMutationFailure()
}

object OutboxPayload {
    fun record(
        change: OutboxChange,
        id: String,
        createdAt: String,
    ): OutboxRecord {
        val kind: String
        val payload: JsonObject
        when (change) {
            is OutboxChange.CheckOff -> {
                kind = "check-off"
                val value = change.change
                payload =
                    buildJsonObject {
                        put("tripID", value.tripID)
                        put("plan_input_hash", value.planInputHash)
                        put("type", value.type.raw)
                        put("local_date", value.localDate)
                        put("ordinal", value.ordinal)
                        value.status?.let { put("status", it.raw) }
                    }
            }

            is OutboxChange.Settings -> {
                if (change.patch.isEmpty() ||
                    !safeSettings(change.patch, change.section)
                ) {
                    throw LocalMutationFailure.InvalidPayload
                }
                kind = "settings"
                payload =
                    buildJsonObject {
                        put("section", change.section.raw)
                        put("patch", change.patch)
                    }
            }

            is OutboxChange.Policies -> {
                kind = "settings"
                payload =
                    buildJsonObject {
                        put("section", "policies")
                        put("version", change.version)
                    }
            }

            is OutboxChange.Profile -> {
                kind = "profile"
                payload =
                    buildJsonObject {
                        put("name", change.change.name)
                        put("email", change.change.email)
                    }
            }

            OutboxChange.Walkthrough -> {
                kind = "walkthrough"
                payload = JsonObject(emptyMap())
            }

            is OutboxChange.Unsupported -> {
                throw LocalMutationFailure.Unsupported
            }
        }
        return OutboxRecord(id, kind, CanonicalJson.encode(payload), createdAt)
    }

    fun change(record: OutboxRecord): OutboxChange {
        val payload = objectOrNull(record) ?: throw LocalMutationFailure.InvalidPayload

        fun string(key: String): String = string(payload[key]) ?: throw LocalMutationFailure.InvalidPayload
        return when (record.kind) {
            "check-off" -> {
                val ordinal =
                    (payload["ordinal"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
                        ?: throw LocalMutationFailure.InvalidPayload
                val status =
                    when (val value = payload["status"]) {
                        null, JsonNull -> {
                            null
                        }

                        else -> {
                            when (string(value)) {
                                "done" -> CheckStatus.Done
                                "skipped" -> CheckStatus.Skipped
                                else -> throw LocalMutationFailure.InvalidPayload
                            }
                        }
                    }
                OutboxChange.CheckOff(
                    CheckOffChange(
                        string("tripID"),
                        string("plan_input_hash"),
                        BlockType.of(string("type")),
                        string("local_date"),
                        ordinal,
                        status,
                    ),
                )
            }

            "settings" -> {
                val section = string("section")
                if (section == "policies" && string(payload["version"]) != null) {
                    OutboxChange.Policies(string("version"))
                } else {
                    OutboxChange.Settings(
                        SettingsSection.entries.firstOrNull { it.raw == section }
                            ?: throw LocalMutationFailure.InvalidPayload,
                        payload["patch"] as? JsonObject ?: throw LocalMutationFailure.InvalidPayload,
                    )
                }
            }

            "profile" -> {
                OutboxChange.Profile(ProfileChange(string("name"), string("email")))
            }

            "walkthrough" -> {
                OutboxChange.Walkthrough
            }

            "complete-trip" -> {
                OutboxChange.Unsupported(OutboxKind.CompleteTrip, payload)
            }

            "data-export" -> {
                OutboxChange.Unsupported(OutboxKind.DataExport, payload)
            }

            "security" -> {
                OutboxChange.Unsupported(OutboxKind.Security, payload)
            }

            else -> {
                throw LocalMutationFailure.InvalidPayload
            }
        }
    }

    fun dependency(record: OutboxRecord): String {
        val payload = objectOrNull(record)
        return when (record.kind) {
            "settings" -> "settings:" + (string(payload?.get("section")) ?: "unknown")
            "check-off", "complete-trip" -> "trip:" + (referencedTripId(record) ?: "unknown")
            else -> record.kind
        }
    }

    fun referencedTripId(record: OutboxRecord): String? =
        objectOrNull(record)?.let {
            string(it["tripID"])
                ?: string(it["tripId"])
        }

    fun safeSettings(
        patch: JsonObject,
        section: SettingsSection,
    ): Boolean {
        val allowed =
            when (section) {
                SettingsSection.Preferences -> setOf("time_format", "hidden_categories", "show_optional_items")
                SettingsSection.Reminders -> setOf("light", "avoid", "caffeine", "winddown", "lead_min")
                SettingsSection.BodyClock -> ProfilePreferences.requestFields
            }
        return patch.all { (key, value) ->
            key in allowed &&
                when {
                    key in setOf("exclude_caffeine", "flight_movement_breaks") -> {
                        value is JsonPrimitive && !value.isString && value.booleanOrNull != null
                    }

                    value is JsonObject -> {
                        false
                    }

                    value is JsonArray -> {
                        value.all { it is JsonPrimitive && it.isString }
                    }

                    else -> {
                        true
                    }
                }
        }
    }

    private fun string(value: JsonElement?): String? = (value as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun objectOrNull(record: OutboxRecord): JsonObject? =
        try {
            ApiJson.response.parseToJsonElement(record.payload) as? JsonObject
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
}
