package net.chronoflow.core.plan

import net.chronoflow.core.model.contracts.BlockView
import net.chronoflow.core.model.contracts.DayView
import net.chronoflow.core.model.contracts.Language

fun CopyLineFormatter.dayDateLabel(
    day: DayView,
    language: Language,
): String {
    val range = day.labelRange
    if (range == null || range == day.labelDate) return formatWeekdayDate(day.labelDate, language)
    val first = formatWeekdayDate(day.labelDate, language, month = day.labelDate.take(7) != range.take(7))
    return first + " – " + formatWeekdayDate(range, language)
}

fun CopyLineFormatter.blockInterval(
    block: BlockView,
    options: CopyFormat,
    separator: String = " – ",
): String {
    val zone = block.displayTz
    val dates = localDate(block.startUtc, zone) != localDate(block.endUtc, zone)
    val offsets = utcOffset(block.startUtc, zone) != utcOffset(block.endUtc, zone)

    fun endpoint(at: String): String =
        (if (dates) formatDate(at, options.language, zone) + " " else "") +
            formatInstant(at, options, zone) +
            (if (offsets) " (" + utcOffset(at, zone) + ")" else "")
    return endpoint(block.startUtc) + separator + endpoint(block.endUtc)
}

fun localDate(
    at: String,
    zone: String,
): String {
    val instant = planDate(at) ?: return ""
    val fields = localFields(instant, planZoneRules.rules(zone))
    return StringBuilder(10)
        .appendYear(fields.year)
        .append('-')
        .append2(fields.month)
        .append('-')
        .append2(fields.day)
        .toString()
}

fun utcOffset(
    at: String,
    zone: String,
): String {
    val instant = planDate(at) ?: return "GMT"
    val offset = planZoneRules.rules(zone).getOffset(instant).totalSeconds
    if (offset == 0) return "GMT"
    val magnitude = Math.abs(offset)
    return StringBuilder(9)
        .append("GMT")
        .append(if (offset < 0) '-' else '+')
        .append2(magnitude / 3600)
        .append(':')
        .append2(magnitude / 60 % 60)
        .toString()
}
