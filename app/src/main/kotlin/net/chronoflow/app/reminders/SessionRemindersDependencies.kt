package net.chronoflow.app.reminders

import android.os.Build
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import net.chronoflow.app.AppGraph
import net.chronoflow.app.SessionGraph
import net.chronoflow.core.data.reminders.FirstTripRemindersOffer
import net.chronoflow.core.model.OutboxChange
import net.chronoflow.core.model.SettingsSection
import net.chronoflow.core.model.session.StaleSessionException
import net.chronoflow.core.reminders.ReminderPermission
import net.chronoflow.core.reminders.ReminderSettings
import net.chronoflow.feature.reminders.RemindersDependencies

internal class SessionRemindersDependencies(
    private val app: AppGraph,
    private val session: SessionGraph,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
) : RemindersDependencies {
    override fun reminderSettings() =
        session.settings
            .values()
            .map { rows ->
                ReminderSettings.decode(rows.firstOrNull { it.section == "reminders" }?.value)
            }.distinctUntilChanged()

    override fun settingsChanges() = session.settings.values().map { }

    override suspend fun permission() = app.reminderPermission()

    override suspend fun beginPermissionRequest() = app.beginReminderPermissionRequest(sdkInt)

    override suspend fun enqueueReminders(patch: JsonObject) {
        val permit = app.controller.mutationPermit(session.snapshot) ?: throw StaleSessionException()
        withContext(app.dispatchers.io) {
            session.outbox.enqueue(OutboxChange.Settings(SettingsSection.Reminders, patch), permit)
        }
    }

    override suspend fun reschedule() = app.reminderService.reschedule()

    override suspend fun consumeFirstTripOffer(permission: ReminderPermission): Boolean? {
        val permit = app.controller.mutationPermit(session.snapshot) ?: throw StaleSessionException()
        return withContext(app.dispatchers.io) { FirstTripRemindersOffer(session.database, permit).consume(permission) }
    }

    override suspend fun isCurrent() = app.controller.activeSession() == session.snapshot
}
