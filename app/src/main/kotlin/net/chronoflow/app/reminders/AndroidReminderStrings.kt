package net.chronoflow.app.reminders

import android.content.Context
import android.content.res.Configuration
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.reminders.ReminderStrings
import net.chronoflow.core.ui.R
import net.chronoflow.core.ui.l10n.generated.CopyIndex
import net.chronoflow.core.ui.l10n.generated.CopyTable
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class AndroidReminderStrings(
    context: Context,
) : ReminderStrings {
    private val app = context.applicationContext
    private val localized = ConcurrentHashMap<Language, Context>()

    private fun localized(language: Language): Context {
        val key = if (language == Language.Nl) Language.Nl else Language.En
        return localized.getOrPut(key) {
            app.createConfigurationContext(
                Configuration(app.resources.configuration).apply {
                    setLocale(Locale.forLanguageTag(if (key == Language.Nl) "nl" else "en"))
                },
            )
        }
    }

    override fun text(
        key: String,
        language: Language,
        time: String,
    ): String {
        val entry = CopyIndex.find(CopyTable.Reminders, key) ?: return key.replace("{{time}}", time)
        return localized(language).getString(entry.resId, time)
    }

    fun channelName(language: Language): String =
        localized(language).getString(R.string.reminders_reminders_on_this_phone)
}
