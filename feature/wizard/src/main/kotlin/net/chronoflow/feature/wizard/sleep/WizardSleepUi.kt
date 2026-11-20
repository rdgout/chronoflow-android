package net.chronoflow.feature.wizard.sleep

import androidx.compose.runtime.Immutable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.WizardDateTime
import net.chronoflow.core.wizard.model.WizardMode
import net.chronoflow.feature.wizard.ui.WizardCopy

internal fun JsonElement?.clockString(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

@Immutable
data class WizardTimeSheetUi(
    val field: String,
    val stored: String?,
    val minutePrecision: Boolean = false,
    val title: WizardCopy? = null,
    val subtitle: WizardCopy? = null,
    val titleSuffix: String? = null,
) {
    val initialHour: Int get() = if (WizardDateTime.isTime(stored)) stored!!.take(2).toInt() else 12
    val initialMinute: Int get() = if (WizardDateTime.isTime(stored)) stored!!.takeLast(2).toInt() else 0
    val usual: Boolean get() = this.field == "habitual_bed" || this.field == "habitual_wake"
    val label: WizardCopy get() =
        title ?: WizardCopy(
            when (this.field) {
                "habitual_bed" -> "Usual bedtime"
                "habitual_wake" -> "Usual wake-up"
                "free_day_bed" -> "Bedtime"
                "override.habitual_bed" -> "Bedtime on this trip"
                "override.habitual_wake" -> "Wake-up on this trip"
                else -> "Wake-up"
            },
        )
}

@Immutable
data class WizardSleepUi(
    val guest: Boolean,
    val bed: String?,
    val wake: String?,
    val chronotype: JsonElement?,
    val errors: Map<String, FieldMessage>,
    val continueFailures: Int,
    val timeFormat: TimeFormat,
    val openTime: WizardTimeSheetUi?,
) {
    val title: WizardCopy get() = WizardCopy("How do you usually sleep?")
    val subtitle: WizardCopy get() =
        WizardCopy(
            if (guest) "On a normal work day." else "On a work day. We save it for next time.",
        )
    val selectedChronotype: String? get() =
        when (chronotype) {
            JsonNull -> "unsure"
            is JsonPrimitive -> chronotype.content
            else -> null
        }

    companion object {
        fun from(
            draft: MemberWizardDraft,
            failures: Int,
            format: TimeFormat,
            openTime: String?,
        ): WizardSleepUi {
            val profile = draft.state.profile
            return WizardSleepUi(
                draft.state.mode == WizardMode.guest,
                profile["habitual_bed"].clockString(),
                profile["habitual_wake"].clockString(),
                profile["chronotype"],
                draft.fieldErrors.filterKeys { it.startsWith("profile.") }.toSortedMap(),
                failures,
                format,
                openTime
                    ?.takeIf { it in setOf("habitual_bed", "habitual_wake") }
                    ?.let { WizardTimeSheetUi(it, profile[it].clockString()) },
            )
        }
    }
}

internal fun clockString(
    hour: Int,
    minute: Int,
): String = hour.toString().padStart(2, '0') + ":" + minute.toString().padStart(2, '0')
