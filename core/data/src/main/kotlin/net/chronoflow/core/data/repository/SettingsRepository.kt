package net.chronoflow.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import net.chronoflow.core.data.store.encoded
import net.chronoflow.core.data.store.objectValue
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.store.deleteSetting
import net.chronoflow.core.database.store.fetchSetting
import net.chronoflow.core.database.store.fetchSettings
import net.chronoflow.core.database.store.saveSetting
import net.chronoflow.core.model.contracts.ProfilePreferences
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.store.SettingsRecord

class SettingsRepository(
    val database: AccountDatabase,
) {
    suspend fun settings(): List<SettingsRecord> = database.read { it.fetchSettings() }

    fun values(): Flow<List<SettingsRecord>> = database.observe("settings") { it.fetchSettings() }

    suspend fun upsert(record: SettingsRecord) =
        database.write { sql ->
            val value =
                when (record.section) {
                    "bodyClock" -> {
                        record.copy(value = encoded(ProfilePreferences.bodyClock(objectValue(record.value))))
                    }

                    "readiness" -> {
                        val storedBody = sql.fetchSetting("bodyClock")
                        val body = storedBody?.let { objectValue(it.value) } ?: JsonObject(emptyMap())
                        record.copy(value = encoded(ProfilePreferences.readiness(objectValue(record.value), body)))
                    }

                    else -> {
                        record
                    }
                }
            sql.saveSetting(value)
        }

    suspend fun upsertSecurity(
        value: JsonObject,
        permit: SessionMutationPermit,
    ) = database.write(permit) {
        it.saveSetting(SettingsRecord("security", encoded(value)))
    }

    suspend fun remove(section: String) = database.write { it.deleteSetting(section) }
}
