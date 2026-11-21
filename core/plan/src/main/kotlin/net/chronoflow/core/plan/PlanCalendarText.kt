package net.chronoflow.core.plan

import net.chronoflow.core.model.contracts.Language
import java.time.Instant
import java.time.zone.ZoneRules

internal enum class DateTemplate { D_MMM_YYYY, D_MMM, EEE_D_MMM, EEE_D, EEE, MMM_YYYY }

fun formatPlanDayMonth(
    value: String,
    language: Language,
    zone: String = "UTC",
): String = dateText(planDate(value), language, planZoneRules.rules(zone), DateTemplate.D_MMM)

private val enMonths = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val nlMonths = arrayOf("jan", "feb", "mrt", "apr", "mei", "jun", "jul", "aug", "sep", "okt", "nov", "dec")
private val enLongMonths =
    arrayOf(
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
private val nlLongMonths =
    arrayOf(
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
private val enWeekdays = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val nlWeekdays = arrayOf("ma", "di", "wo", "do", "vr", "za", "zo")

// Fixed tables: CLDR prints "Sept" and "sep." where Apple prints "Sep" and "sep".
internal fun dateText(
    instant: Instant?,
    language: Language,
    rules: ZoneRules,
    template: DateTemplate,
): String {
    if (instant == null) return ""
    val fields = localFields(instant, rules)
    val nl = language == Language.Nl
    val month = (if (nl) nlMonths else enMonths)[fields.month - 1]
    val weekday = (if (nl) nlWeekdays else enWeekdays)[fields.dayOfWeek - 1]
    val out = StringBuilder(12)
    when (template) {
        DateTemplate.D_MMM_YYYY -> {
            out
                .append(fields.day)
                .append(' ')
                .append(month)
                .append(' ')
                .appendYear(fields.year)
        }

        DateTemplate.D_MMM -> {
            out.append(fields.day).append(' ').append(month)
        }

        DateTemplate.EEE_D_MMM -> {
            out
                .append(weekday)
                .append(' ')
                .append(fields.day)
                .append(' ')
                .append(month)
        }

        DateTemplate.EEE_D -> {
            out.append(weekday).append(' ').append(fields.day)
        }

        DateTemplate.EEE -> {
            out.append(weekday)
        }

        DateTemplate.MMM_YYYY -> {
            out.append(month).append(' ').appendYear(fields.year)
        }
    }
    return out.toString()
}

/** Month names for trip header ranges; `month` is 1-based. Fixed tables, never locale data (A17). */
internal fun monthName(
    month: Int,
    language: Language,
    long: Boolean,
): String {
    val nl = language == Language.Nl
    return (if (long) (if (nl) nlLongMonths else enLongMonths) else (if (nl) nlMonths else enMonths))[month - 1]
}

internal fun StringBuilder.appendYear(year: Int): StringBuilder {
    val digits = year.toString()
    repeat(4 - digits.length) { append('0') }
    return append(digits)
}
