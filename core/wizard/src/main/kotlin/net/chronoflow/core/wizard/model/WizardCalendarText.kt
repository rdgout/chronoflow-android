package net.chronoflow.core.wizard.model

import net.chronoflow.core.model.contracts.Language
import java.time.LocalDateTime

enum class WizardDateTemplate { EEE_D_MMM, EEE, EEE_D_MMM_Y }

internal object WizardCalendarText {
    private val enWeekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val nlWeekdays = listOf("ma", "di", "wo", "do", "vr", "za", "zo")
    private val enMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val nlMonths = listOf("jan", "feb", "mrt", "apr", "mei", "jun", "jul", "aug", "sep", "okt", "nov", "dec")

    fun format(
        date: LocalDateTime,
        template: WizardDateTemplate,
        language: Language,
    ): String {
        val nl = language == Language.Nl
        val weekday = (if (nl) nlWeekdays else enWeekdays)[date.dayOfWeek.value - 1]
        if (template == WizardDateTemplate.EEE) return weekday
        val month = (if (nl) nlMonths else enMonths)[date.monthValue - 1]
        val comma = if (template == WizardDateTemplate.EEE_D_MMM_Y && !nl) "," else ""
        val year = if (template == WizardDateTemplate.EEE_D_MMM_Y) " ${date.year}" else ""
        return "$weekday$comma ${date.dayOfMonth} $month$year"
    }
}
