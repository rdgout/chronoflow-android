package net.chronoflow.core.plan

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.zone.ZoneRules
import java.util.concurrent.ConcurrentHashMap

// Apple's ISO8601 parser is lenient about field overflow and offsets; ISO_OFFSET_DATE_TIME is not, so this parses by hand.
fun planDate(value: String): Instant? {
    val text =
        when {
            value.length == 10 -> value + "T12:00:00Z"
            value.length == 17 && value.endsWith('Z') -> value.substring(0, 16) + ":00Z"
            else -> value
        }
    return parseIso(text)
}

private const val SECONDS_PER_DAY = 86_400L

private class Cursor(
    val text: String,
) {
    var index = 0

    fun digits(count: Int): Int {
        if (index + count > text.length) return -1
        var result = 0
        for (i in index until index + count) {
            val digit = text[i] - '0'
            if (digit !in 0..9) return -1
            result = result * 10 + digit
        }
        index += count
        return result
    }

    fun expect(char: Char): Boolean {
        if (index < text.length && text[index] == char) {
            index++
            return true
        }
        return false
    }

    fun peek(): Char? = text.getOrNull(index)

    val atEnd: Boolean get() = index == text.length
}

private fun parseIso(text: String): Instant? {
    val cursor = Cursor(text)
    val year = cursor.digits(4)
    if (year < 0 || !cursor.expect('-')) return null
    val month = cursor.digits(2)
    if (month !in 1..12 || !cursor.expect('-')) return null
    val day = cursor.digits(2)
    if (day !in 1..31 || !cursor.expect('T')) return null
    val hour = cursor.digits(2)
    if (hour !in 0..24 || !cursor.expect(':')) return null
    val minute = cursor.digits(2)
    if (minute !in 0..59 || !cursor.expect(':')) return null
    val second = cursor.digits(2)
    if (second !in 0..60) return null
    var nanos = 0
    if (cursor.expect('.')) {
        var count = 0
        while (cursor.peek()?.let { it in '0'..'9' } == true) {
            if (count == 9) return null
            nanos = nanos * 10 + (cursor.peek()!! - '0')
            cursor.index++
            count++
        }
        if (count == 0) return null
        repeat(9 - count) { nanos *= 10 }
    }
    val offsetSeconds =
        when (cursor.peek()) {
            'Z', 'z' -> {
                cursor.index++
                0
            }

            '+', '-' -> {
                val sign = if (cursor.peek() == '-') -1 else 1
                cursor.index++
                val offsetHour = cursor.digits(2)
                if (offsetHour !in 0..23) return null
                var offsetMinute = 0
                if (!cursor.atEnd) {
                    cursor.expect(':')
                    offsetMinute = cursor.digits(2)
                    if (offsetMinute !in 0..59) return null
                }
                sign * (offsetHour * 3600 + offsetMinute * 60)
            }

            else -> {
                return null
            }
        }
    if (!cursor.atEnd) return null
    val epochDay = LocalDate.of(year, month, 1).toEpochDay() + day - 1
    val epochSecond = epochDay * SECONDS_PER_DAY + hour * 3600L + minute * 60L + second - offsetSeconds
    return Instant.ofEpochSecond(epochSecond, nanos.toLong())
}

internal class ZoneRulesCache {
    private val rules = ConcurrentHashMap<String, ZoneRules>()

    fun rules(tz: String?): ZoneRules {
        if (tz == null) return ZoneOffset.UTC.rules
        return rules.computeIfAbsent(tz) {
            try {
                ZoneId.of(it).rules
            } catch (_: DateTimeException) {
                ZoneOffset.UTC.rules
            }
        }
    }
}

internal val planZoneRules = ZoneRulesCache()

internal class LocalFields(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val dayOfWeek: Int,
)

internal fun localFields(
    instant: Instant,
    rules: ZoneRules,
): LocalFields {
    val local = instant.epochSecond + rules.getOffset(instant).totalSeconds
    val epochDay = Math.floorDiv(local, SECONDS_PER_DAY)
    val secondOfDay = Math.floorMod(local, SECONDS_PER_DAY).toInt()
    val date = LocalDate.ofEpochDay(epochDay)
    return LocalFields(
        year = date.year,
        month = date.monthValue,
        day = date.dayOfMonth,
        hour = secondOfDay / 3600,
        minute = secondOfDay / 60 % 60,
        dayOfWeek = date.dayOfWeek.value,
    )
}

internal fun StringBuilder.append2(n: Int): StringBuilder {
    if (n < 10) append('0')
    return append(n)
}
