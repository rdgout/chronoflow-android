package net.chronoflow.core.testing.reminders

import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.reminders.ReminderStrings

class FakeReminderStrings : ReminderStrings {
    override fun text(
        key: String,
        language: Language,
        time: String,
    ): String {
        val value =
            if (language == Language.Nl) {
                when (key) {
                    "Seek bright light from {{time}}." -> "Zoek vanaf {{time}} fel licht."
                    "Avoid bright light from {{time}}." -> "Vermijd vanaf {{time}} fel licht."
                    "Last caffeine before {{time}}." -> "Neem vóór {{time}} je laatste cafeïne."
                    "Wind down for bed: sleep from {{time}}." -> "Kom tot rust: slapen vanaf {{time}}."
                    else -> key
                }
            } else {
                key
            }
        return value.replace("{{time}}", time)
    }
}
