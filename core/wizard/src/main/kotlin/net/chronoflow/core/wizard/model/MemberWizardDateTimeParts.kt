package net.chronoflow.core.wizard.model

import java.time.LocalDate

fun MemberWizardDraft.datePart(
    index: Int,
    arrival: Boolean,
): String? {
    val leg = form.legs.getOrNull(index) ?: return null
    val value = if (arrival) leg.arriveLocal else leg.departLocal
    return WizardDateTime.date(value)?.let { value.take(10) }
        ?: state.pendingDateTimes[partKey(leg, arrival)]?.date
}

fun MemberWizardDraft.timePart(
    index: Int,
    arrival: Boolean,
): String? {
    val leg = form.legs.getOrNull(index) ?: return null
    val value = if (arrival) leg.arriveLocal else leg.departLocal
    return WizardDateTime.date(value)?.let { value.takeLast(5) }
        ?: state.pendingDateTimes[partKey(leg, arrival)]?.time
}

fun MemberWizardModel.datePart(
    index: Int,
    arrival: Boolean,
): String? = draft.datePart(index, arrival)

fun MemberWizardModel.timePart(
    index: Int,
    arrival: Boolean,
): String? = draft.timePart(index, arrival)

fun MemberWizardModel.setDatePart(
    index: Int,
    arrival: Boolean,
    date: LocalDate,
) {
    if (closed || index !in form.legs.indices || date.year !in 1..9999) return
    writeParts(index, arrival, WizardPendingDateTime(date.toString(), timePart(index, arrival)))
}

fun MemberWizardModel.setTimePart(
    index: Int,
    arrival: Boolean,
    hour: Int,
    minute: Int,
) {
    if (closed || index !in form.legs.indices || hour !in 0..23 || minute !in 0..59) return
    val time = hour.toString().padStart(2, '0') + ":" + minute.toString().padStart(2, '0')
    writeParts(index, arrival, WizardPendingDateTime(datePart(index, arrival), time))
}

private fun MemberWizardModel.writeParts(
    index: Int,
    arrival: Boolean,
    parts: WizardPendingDateTime,
) {
    val key = partKey(form.legs[index], arrival)
    if (parts.date != null && parts.time != null) {
        state = state.copy(pendingDateTimes = state.pendingDateTimes - key)
        setDateTime("${parts.date}T${parts.time}", index, arrival)
    } else {
        state = state.copy(pendingDateTimes = state.pendingDateTimes + (key to parts))
        changed()
    }
}

private fun partKey(
    leg: WizardLeg,
    arrival: Boolean,
): String = leg.id + if (arrival) ".arriveLocal" else ".departLocal"
