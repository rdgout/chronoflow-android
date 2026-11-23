package net.chronoflow.core.data.reminders

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.store.fetchSetting
import net.chronoflow.core.database.store.saveSetting
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.store.SettingsRecord
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings

class FirstTripRemindersOffer(
    private val database: AccountDatabase,
    private val permit: SessionMutationPermit? = null,
) {
    suspend fun queue(firstTrip: Boolean) {
        if (!firstTrip) return
        database.write(permit) { sql ->
            if (sql.fetchSetting("wizardReminders") == null) {
                sql.saveSetting(SettingsRecord("wizardReminders", "\"pending\""))
            }
        }
    }

    suspend fun consume(permission: ReminderPermission): Boolean? {
        currentCoroutineContext().ensureActive()
        return database.write(permit) { sql ->
            if (sql.fetchSetting("wizardReminders")?.value != "\"pending\"") return@write false
            val settings = ReminderSettings.decode(sql.fetchSetting("reminders")?.value) ?: return@write null
            // Consume before presentation so recovery and Not now cannot offer the sheet again.
            sql.saveSetting(SettingsRecord("wizardReminders", "\"seen\""))
            permission == ReminderPermission.Undetermined && settings.enabled
        }
    }
}
