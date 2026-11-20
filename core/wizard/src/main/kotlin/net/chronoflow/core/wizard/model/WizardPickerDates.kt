package net.chronoflow.core.wizard.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object WizardPickerDates {
    fun utcMillis(date: LocalDate): Long = WizardDateTime.pickerMillis(date.atStartOfDay())

    fun localDate(utcMillis: Long): LocalDate = WizardDateTime.fromPickerMillis(utcMillis).toLocalDate()

    fun isSelectable(
        utcMillis: Long,
        minimum: LocalDateTime?,
    ): Boolean = minimum == null || localDate(utcMillis) >= minimum.toLocalDate()

    fun isSelectableYear(
        year: Int,
        minimum: LocalDateTime?,
    ): Boolean = minimum == null || year >= minimum.year

    fun initialDate(
        draft: MemberWizardDraft,
        field: WizardFlightTiming.Field,
        now: Instant,
    ): LocalDate =
        draft.datePart(field.index, field.isArrival)?.let(LocalDate::parse)
            ?: WizardFlightTiming(draft.form, draft.airports).initialLocal(field, now).toLocalDate()

    fun initialTime(
        draft: MemberWizardDraft,
        field: WizardFlightTiming.Field,
        now: Instant,
    ): LocalTime =
        draft.timePart(field.index, field.isArrival)?.let(LocalTime::parse)
            ?: WizardFlightTiming(draft.form, draft.airports).initialLocal(field, now).toLocalTime()
}
