package net.chronoflow.core.wizard.model

import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

object WizardDateTime {
    private val timePattern = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
    private val datePattern = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$")
    private val instantPattern = Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?(Z|[+-]\\d{2}:\\d{2})$")

    fun isTime(value: String?): Boolean = value != null && timePattern.matches(value)

    fun date(value: String): LocalDateTime? {
        if (!datePattern.matches(value)) return null
        return try {
            val year = value.substring(0, 4).toInt()
            if (year <= 0) return null
            LocalDateTime
                .of(
                    year,
                    value.substring(5, 7).toInt(),
                    value.substring(8, 10).toInt(),
                    value.substring(11, 13).toInt(),
                    value.substring(14, 16).toInt(),
                ).takeIf { string(it) == value }
        } catch (_: DateTimeException) {
            null
        }
    }

    fun string(date: LocalDateTime): String =
        date.year.toString().padStart(4, '0') + "-" + two(date.monthValue) + "-" + two(date.dayOfMonth) +
            "T" + two(date.hour) + ":" + two(date.minute)

    fun shiftDate(
        value: String,
        days: Int,
    ): String? =
        date(value.take(10) + "T12:00")?.plusDays(days.toLong())?.let {
            string(it).take(10)
        }

    fun pickerValue(
        value: String?,
        fallbackDate: String,
        dateTime: Boolean = true,
    ): String =
        if (dateTime) {
            value?.takeIf { date(it) != null } ?: (fallbackDate + "T12:00")
        } else {
            value?.takeIf(::isTime)
                ?: "12:00"
        }

    fun pickerDate(
        value: String,
        dateTime: Boolean = true,
    ): LocalDateTime? = date(if (dateTime) value else "2001-01-15T$value")

    fun pickedValue(
        date: LocalDateTime,
        dateTime: Boolean = true,
    ): String =
        string(date).let {
            if (dateTime) it else it.takeLast(5)
        }

    fun pickerMillis(value: LocalDateTime): Long = value.toEpochSecond(ZoneOffset.UTC) * 1000

    fun fromPickerMillis(millis: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES)

    fun instant(
        value: String,
        zone: String,
    ): Instant? {
        val wall = date(value) ?: return null
        return try {
            val zoneId = ZoneId.of(zone)
            if (zoneId.rules
                    .getValidOffsets(
                        wall,
                    ).isEmpty()
            ) {
                null
            } else {
                ZonedDateTime.ofLocal(wall, zoneId, null).toInstant()
            }
        } catch (_: DateTimeException) {
            null
        }
    }

    fun localDate(
        instant: Instant,
        zone: String,
    ): LocalDateTime? =
        try {
            LocalDateTime.ofInstant(instant, ZoneId.of(zone)).truncatedTo(ChronoUnit.MINUTES)
        } catch (_: DateTimeException) {
            null
        }

    fun parseInstant(value: String): Instant? {
        val expanded = if (value.length == 17 && value.endsWith('Z')) value.dropLast(1) + ":00Z" else value
        if (!instantPattern.matches(expanded)) return null
        return try {
            OffsetDateTime.parse(expanded).toInstant()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    fun formatWeekdayTime(
        value: String?,
        language: Language,
        twelveHour: Boolean = false,
    ): String? {
        value ?: return null
        val date = date(value) ?: return value
        val weekday = format(date, WizardDateTemplate.EEE, language)
        return "$weekday ${WizardDateFormat.time(
            "${two(date.hour)}:${two(date.minute)}",
            if (twelveHour) TimeFormat.Value12 else TimeFormat.Value24,
            language,
        )}"
    }

    fun format(
        date: LocalDateTime,
        template: WizardDateTemplate,
        language: Language,
    ): String = WizardCalendarText.format(date, template, language)

    fun format(
        instant: Instant,
        template: WizardDateTemplate,
        language: Language,
        zone: String,
    ): String? = localDate(instant, zone)?.let { format(it, template, language) }

    private fun two(value: Int): String = value.toString().padStart(2, '0')
}
