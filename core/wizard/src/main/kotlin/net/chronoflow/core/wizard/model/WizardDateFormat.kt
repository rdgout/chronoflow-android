package net.chronoflow.core.wizard.model

import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import java.time.LocalDate

object WizardDateFormat {
    private val enWeekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    private val nlWeekdays = listOf("maandag", "dinsdag", "woensdag", "donderdag", "vrijdag", "zaterdag", "zondag")
    private val enMonths =
        listOf(
            "January",
            "February",
            "March",
            "April",
            "May",
            "June",
            "July",
            "August",
            "September",
            "October",
            "November",
            "December",
        )
    private val nlMonths =
        listOf(
            "januari",
            "februari",
            "maart",
            "april",
            "mei",
            "juni",
            "juli",
            "augustus",
            "september",
            "oktober",
            "november",
            "december",
        )

    fun datePill(
        date: String,
        language: Language,
    ): String {
        val value = LocalDate.parse(date)
        return "${value.dayOfMonth} ${WizardCalendarText.shortMonth(value.monthValue, language)} ${value.year}"
    }

    fun dateSpoken(
        date: String,
        language: Language,
    ): String {
        val value = LocalDate.parse(date)
        val nl = language == Language.Nl
        val weekday = (if (nl) nlWeekdays else enWeekdays)[value.dayOfWeek.value - 1]
        val month = (if (nl) nlMonths else enMonths)[value.monthValue - 1]
        return "$weekday ${value.dayOfMonth} $month ${value.year}"
    }

    fun time(
        time: String,
        timeFormat: TimeFormat,
        language: Language,
    ): String {
        if (timeFormat != TimeFormat.Value12) return time
        val hour24 = time.take(2).toInt()
        val hour = (hour24 % 12).let { if (it == 0) 12 else it }
        val nl = language == Language.Nl
        val period =
            when {
                nl && hour24 < 12 -> "a.m."
                nl -> "p.m."
                hour24 < 12 -> "AM"
                else -> "PM"
            }
        return "${if (nl) hour.toString().padStart(2, '0') else hour.toString()}:${time.takeLast(2)} $period"
    }
}
