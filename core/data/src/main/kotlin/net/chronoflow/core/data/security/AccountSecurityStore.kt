package net.chronoflow.core.data.security

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.data.repository.SettingsRepository
import net.chronoflow.core.data.store.StoreApplication
import net.chronoflow.core.data.store.objectValue
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.store.fetchSetting
import net.chronoflow.core.model.api.AccountSecurityChange
import net.chronoflow.core.model.api.AccountSecurityResult
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.store.StoreFailure

class AccountSecurityStore(
    private val database: AccountDatabase,
    private val settings: SettingsRepository,
) {
    suspend fun apply(
        change: AccountSecurityChange,
        result: AccountSecurityResult,
        permit: SessionMutationPermit,
    ) {
        when {
            result is AccountSecurityResult.Snapshot -> {
                database.write(permit) { sql ->
                    StoreApplication.snapshot(result.snapshot, database.accountID ?: throw StoreFailure.Closed, sql)
                }
            }

            result is AccountSecurityResult.Passkeys -> {
                val value = security().toMutableMap()
                value["passkeyCount"] = JsonPrimitive(result.passkeys.size)
                settings.upsertSecurity(JsonObject(value), permit)
            }

            change is AccountSecurityChange.ConfirmTwoFactor && result is AccountSecurityResult.Codes -> {
                val value = security().toMutableMap()
                value["twoFactorEnabled"] = JsonPrimitive(true)
                value["twoFactorConfirmedAt"] = JsonNull
                settings.upsertSecurity(JsonObject(value), permit)
            }
        }
    }

    private suspend fun security(): JsonObject =
        database.read { sql ->
            sql.fetchSetting("security")?.let { objectValue(it.value) } ?: JsonObject(emptyMap())
        }
}
