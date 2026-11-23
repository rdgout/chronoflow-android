package net.chronoflow.core.data.reminders

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.reminders.ReminderRequest
import net.chronoflow.core.testing.reminders.reminderAccount
import net.chronoflow.core.testing.reminders.reminderNow
import java.util.Locale

fun adapterRequest(): ReminderRequest =
    ReminderRequest(
        "chronoflow.reminder.test",
        "Title",
        "Body",
        reminderNow.plusSeconds(1800),
        "trip",
        "light",
        "hash",
        listOf("seek_light|2026-10-14|1"),
        reminderAccount,
        reminderNow.plusSeconds(4800),
    )

fun channelName(
    context: Context,
    language: Language,
): String {
    val config = android.content.res.Configuration(context.resources.configuration)
    config.setLocale(Locale.forLanguageTag(if (language == Language.Nl) "nl" else "en"))
    return context
        .createConfigurationContext(
            config,
        ).getString(net.chronoflow.core.ui.R.string.reminders_reminders_on_this_phone)
}

class MemoryReminderPreferences : DataStore<Preferences> {
    override val data = MutableStateFlow(emptyPreferences())

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        transform(data.value).also { data.value = it }
}
