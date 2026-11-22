package net.chronoflow.core.reminders

import net.chronoflow.core.model.contracts.Language

fun interface ReminderStrings {
    /** The catalog value with `{{time}}` replaced; the key itself when missing. */
    fun text(
        key: String,
        language: Language,
        time: String,
    ): String
}
