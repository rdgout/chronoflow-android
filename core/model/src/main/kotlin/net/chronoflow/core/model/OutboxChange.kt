package net.chronoflow.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.model.api.AccountIdentity
import net.chronoflow.core.model.api.AccountSnapshot
import net.chronoflow.core.model.contracts.TripCheckResult

@Serializable
data class ProfileChange(
    val name: String,
    val email: String,
)

enum class SettingsSection(
    val raw: String,
) {
    BodyClock("bodyClock"),
    Preferences("preferences"),
    Reminders("reminders"),
}

sealed interface OutboxChange {
    data class CheckOff(
        val change: CheckOffChange,
    ) : OutboxChange

    data class Settings(
        val section: SettingsSection,
        val patch: JsonObject,
    ) : OutboxChange

    data class Policies(
        val version: String,
    ) : OutboxChange

    data class Profile(
        val change: ProfileChange,
    ) : OutboxChange

    data object Walkthrough : OutboxChange

    data class Unsupported(
        val kind: OutboxKind,
        val payload: JsonObject,
    ) : OutboxChange
}

sealed interface MutationReceipt {
    data object Acknowledged : MutationReceipt

    data class Account(
        val snapshot: AccountSnapshot,
    ) : MutationReceipt

    data class Identity(
        val identity: AccountIdentity,
    ) : MutationReceipt

    data class Checks(
        val result: TripCheckResult,
    ) : MutationReceipt
}
