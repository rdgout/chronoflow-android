package net.chronoflow.core.data.reminders

import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.database.store.fetchSetting
import net.chronoflow.core.database.store.saveSetting
import net.chronoflow.core.model.session.SessionMutationPermit
import net.chronoflow.core.model.store.SettingsRecord

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
}
