package net.chronoflow.debug.reminders

import net.chronoflow.core.data.reminders.FirstTripRemindersOffer
import net.chronoflow.core.database.AccountDatabase
import net.chronoflow.core.datastore.DevicePreferences
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.debug.TestSeed

internal class RemindersSeed private constructor(
    val permission: MockReminderCenter?,
    private val requested: Boolean?,
    private val firstTrip: Boolean,
) {
    suspend fun apply(preferences: DevicePreferences) {
        requested?.let { preferences.setNotificationPermissionRequested(it) }
    }

    suspend fun queue(database: AccountDatabase) {
        if (firstTrip) FirstTripRemindersOffer(database).queue(true)
    }

    companion object {
        fun from(seed: TestSeed): RemindersSeed {
            val raw = seed.raw.takeIf { seed.testId != null }.orEmpty()
            val permission =
                raw["cf.notificationAuthorization"]?.let { value ->
                    MockReminderCenter(
                        when (value) {
                            "granted" -> ReminderPermission.Granted
                            "blocked" -> ReminderPermission.Blocked
                            "unsupported" -> ReminderPermission.Unsupported
                            else -> ReminderPermission.Undetermined
                        },
                    )
                }
            val requested =
                when (raw["cf.notificationPermissionRequested"]) {
                    "1" -> true
                    "0" -> false
                    else -> null
                }
            return RemindersSeed(permission, requested, raw["cf.firstTripReminders"] == "1")
        }
    }
}
