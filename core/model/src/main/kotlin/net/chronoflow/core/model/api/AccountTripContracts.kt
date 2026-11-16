package net.chronoflow.core.model.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.contracts.ObjectSerializer
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.model.contracts.booleanAt
import net.chronoflow.core.model.contracts.objectAt
import net.chronoflow.core.model.contracts.require
import net.chronoflow.core.model.contracts.stringAt

@Serializable(with = AccountSettings.Serializer::class)
@ConsistentCopyVisibility
data class AccountSettings private constructor(
    val bodyClock: JsonObject,
    val sleepAnswered: Boolean,
    val preferences: JsonObject,
    val reminders: JsonObject,
    val planCredits: JsonObject,
    val policies: AccountPolicies,
) {
    companion object {
        operator fun invoke(
            bodyClock: JsonObject,
            sleepAnswered: Boolean,
            preferences: JsonObject,
            reminders: JsonObject,
            planCredits: JsonObject,
            policies: AccountPolicies,
        ) = AccountSettings(
            ProfilePreferences.bodyClock(bodyClock),
            sleepAnswered,
            preferences,
            reminders,
            planCredits,
            policies,
        )
    }

    object Serializer : ObjectSerializer<AccountSettings>("net.chronoflow.core.model.api.AccountSettings") {
        override fun decode(
            json: Json,
            obj: JsonObject,
        ) = AccountSettings(
            bodyClock = ProfilePreferences.bodyClock(obj.objectAt("bodyClock")),
            sleepAnswered = obj.booleanAt("sleepAnswered"),
            preferences = obj.objectAt("preferences"),
            reminders = obj.objectAt("reminders"),
            planCredits = obj.objectAt("planCredits"),
            policies = json.decodeFromJsonElement(AccountPolicies.serializer(), obj.require("policies")),
        )

        override fun encode(
            json: Json,
            value: AccountSettings,
        ) = JsonObject(
            mapOf(
                "bodyClock" to ProfilePreferences.bodyClock(value.bodyClock),
                "sleepAnswered" to JsonPrimitive(value.sleepAnswered),
                "preferences" to value.preferences,
                "reminders" to value.reminders,
                "planCredits" to value.planCredits,
                "policies" to json.encodeToJsonElement(AccountPolicies.serializer(), value.policies),
            ),
        )
    }
}

@Serializable(with = AccountSnapshot.Serializer::class)
@ConsistentCopyVisibility
data class AccountSnapshot private constructor(
    val id: String,
    val name: String,
    val email: String,
    val hasVerifiedEmail: Boolean,
    val settings: AccountSettings,
    val readiness: JsonObject,
    val billing: JsonObject,
    val security: JsonObject,
) {
    companion object {
        operator fun invoke(
            id: String,
            name: String,
            email: String,
            hasVerifiedEmail: Boolean,
            settings: AccountSettings,
            readiness: JsonObject,
            billing: JsonObject,
            security: JsonObject,
        ) = AccountSnapshot(
            id,
            name,
            email,
            hasVerifiedEmail,
            settings,
            ProfilePreferences.readiness(readiness, settings.bodyClock),
            billing,
            security,
        )
    }

    object Serializer : ObjectSerializer<AccountSnapshot>("net.chronoflow.core.model.api.AccountSnapshot") {
        override fun decode(
            json: Json,
            obj: JsonObject,
        ): AccountSnapshot {
            val settings = json.decodeFromJsonElement(AccountSettings.serializer(), obj.require("settings"))
            return AccountSnapshot(
                id = obj.stringAt("id"),
                name = obj.stringAt("name"),
                email = obj.stringAt("email"),
                hasVerifiedEmail = obj.booleanAt("hasVerifiedEmail"),
                settings = settings,
                readiness = ProfilePreferences.readiness(obj.objectAt("readiness"), settings.bodyClock),
                billing = obj.objectAt("billing"),
                security = obj.objectAt("security"),
            )
        }

        override fun encode(
            json: Json,
            value: AccountSnapshot,
        ) = JsonObject(
            mapOf(
                "id" to JsonPrimitive(value.id),
                "name" to JsonPrimitive(value.name),
                "email" to JsonPrimitive(value.email),
                "hasVerifiedEmail" to JsonPrimitive(value.hasVerifiedEmail),
                "settings" to json.encodeToJsonElement(AccountSettings.serializer(), value.settings),
                "readiness" to ProfilePreferences.readiness(value.readiness, value.settings.bodyClock),
                "billing" to value.billing,
                "security" to value.security,
            ),
        )
    }
}
