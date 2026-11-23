package net.chronoflow.feature.wizard.answers

import net.chronoflow.core.model.contracts.PromptKind
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.WizardDateTime
import net.chronoflow.feature.wizard.finetune.promptLabel
import net.chronoflow.feature.wizard.finetune.promptLegIndex
import net.chronoflow.feature.wizard.sleep.WizardTimeSheetUi
import net.chronoflow.feature.wizard.sleep.clockString
import net.chronoflow.feature.wizard.ui.WizardCopy
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

internal fun answerSheetUi(
    state: WizardAnswerSheetState,
    draft: MemberWizardDraft,
    format: TimeFormat,
    now: Instant,
    openTime: String?,
): WizardAnswerSheetUi {
    val form = draft.form
    val leg = form.legs.getOrNull(promptLegIndex(state.prompt, form))
    val (label, suffix) = promptLabel(state.prompt, form)
    val roomZone = leg?.toCity?.let { WizardCopy("Time in {{city}}", mapOf("city" to it)) } ?: WizardCopy("Local time")
    val first = form.legs.firstOrNull()
    val originZone = draft.airports[first?.fromIata]?.timezone ?: "UTC"
    val localNow = WizardDateTime.localDate(now, originZone) ?: LocalDateTime.ofInstant(now, ZoneOffset.UTC)
    val initial = first?.departLocal?.takeIf { WizardDateTime.date(it) != null } ?: WizardDateTime.string(localNow)
    val room = state.prompt.kind == PromptKind.Room && openTime == "stay.bed_available_from_local"
    val free = state.prompt.kind == PromptKind.FreeDayTimes && openTime in setOf("free_day_bed", "free_day_wake")
    return WizardAnswerSheetUi(
        state,
        format,
        openTime =
            when {
                room -> {
                    WizardTimeSheetUi(
                        openTime,
                        state.values[openTime].clockString()?.takeLast(5),
                        minutePrecision = true,
                        title = WizardCopy(label),
                        subtitle = roomZone,
                        titleSuffix = suffix,
                    )
                }

                free -> {
                    WizardTimeSheetUi(openTime!!, state.values[openTime].clockString())
                }

                else -> {
                    null
                }
            },
        labelKey = label,
        suffix = suffix,
        route = leg?.let { "${it.fromIata} → ${it.toIata}" },
        roomArrival = leg?.arriveLocal?.takeIf { WizardDateTime.date(it) != null },
        roomZone = roomZone,
        busyInitial = initial,
        busyZone =
            first?.fromCity?.let { WizardCopy("Time in {{city}}", mapOf("city" to it)) } ?: WizardCopy("Local time"),
    )
}
