package net.chronoflow.core.plan

import net.chronoflow.core.model.contracts.Language

fun tripDatesText(
    start: String,
    end: String?,
    language: Language,
    long: Boolean = false,
    pageCopy: PageCopy,
): String {
    val first = start.take(10)
    val last = (end ?: start).take(10)
    val sameYear = first.take(4) == last.take(4)
    val sameMonth = first.take(7) == last.take(7)

    fun date(
        value: String,
        leading: Boolean = false,
    ): String {
        val month = value.substring(5, 7).toIntOrNull()?.takeIf { it in 1..12 } ?: return value
        val day = value.substring(8, 10).toIntOrNull() ?: return value
        val out = StringBuilder(14).append(day)
        if (!(leading && sameMonth)) out.append(' ').append(monthName(month, language, long))
        if (!sameYear) out.append(' ').append(value, 0, 4)
        return out.toString()
    }
    if (first == last) return date(first)
    return pageCopy(if (long) "%@ to %@" else "%@–%@", date(first, leading = true), date(last))
}

fun tripMetaText(
    start: String,
    end: String?,
    flights: List<String>,
    language: Language,
    pageCopy: PageCopy,
): String =
    (
        listOf(tripDatesText(start, end, language, pageCopy = pageCopy)) +
            flights.map { it.trim() }.filter { it.isNotEmpty() }
    ).joinToString(" · ")
