package net.chronoflow.feature.wizard.times

import androidx.compose.runtime.Immutable
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.WizardConnection
import net.chronoflow.core.wizard.model.WizardDateTime
import net.chronoflow.core.wizard.model.WizardFlightTiming
import net.chronoflow.core.wizard.model.WizardOutboundRules
import net.chronoflow.core.wizard.model.WizardPickerDates
import net.chronoflow.core.wizard.model.datePart
import net.chronoflow.core.wizard.model.timePart
import net.chronoflow.feature.wizard.ui.WizardCopy
import java.time.Instant
import java.time.LocalDate

@Immutable
data class WizardDateTimeFieldUi(
    val index: Int,
    val arrival: Boolean,
    val datePart: String?,
    val timePart: String?,
    val error: FieldMessage?,
    val minimumDate: LocalDate?,
    val city: String?,
) {
    val tag: String get() = "wizard.$index." + if (arrival) "arrival" else "departure"
    val zone: WizardCopy get() =
        city?.let { WizardCopy("Time in {{city}}", mapOf("city" to it)) }
            ?: WizardCopy("Local time")
}

@Immutable
data class WizardFlightCardUi(
    val id: String,
    val index: Int,
    val fromIata: String,
    val toIata: String,
    val departure: WizardDateTimeFieldUi,
    val arrival: WizardDateTimeFieldUi,
    val connection: WizardConnection?,
) {
    val hasErrors: Boolean get() = departure.error != null || arrival.error != null
}

@Immutable
data class WizardFlightPickerUi(
    val picker: WizardPicker,
    val initialDate: LocalDate,
    val initialHour: Int,
    val initialMinute: Int,
)

@Immutable
data class WizardTimesUi(
    val home: Boolean,
    val cards: List<WizardFlightCardUi>,
    val complete: Boolean,
    val banner: FieldMessage?,
    val flightMinutes: Int?,
    val timeFormat: TimeFormat,
    val continueFailures: Int,
    val picker: WizardFlightPickerUi? = null,
) {
    val id: String get() = if (home) "return-flight" else "outbound"
    val title: WizardCopy get() =
        WizardCopy(
            when {
                home -> "When do you fly home?"
                cards.size > 1 -> "When are your flights?"
                else -> "When is your flight?"
            },
        )
    val subtitle: WizardCopy get() = WizardCopy("Times are local at each airport.")

    companion object {
        fun from(
            draft: MemberWizardDraft,
            home: Boolean,
            timeFormat: TimeFormat = TimeFormat.Value24,
            now: Instant,
            continueFailures: Int = 0,
            picker: WizardPicker? = null,
        ): WizardTimesUi {
            val start = if (home) draft.form.returnBoundary else 0
            val end = if (home) draft.form.legs.size else draft.form.returnBoundary
            val timing = WizardFlightTiming(draft.form, draft.airports)

            fun field(
                index: Int,
                arrival: Boolean,
            ): WizardDateTimeFieldUi {
                val field = WizardFlightTiming.Field(index, arrival)
                val leg = draft.form.legs[index]
                return WizardDateTimeFieldUi(
                    index,
                    arrival,
                    draft.datePart(index, arrival),
                    draft.timePart(index, arrival),
                    draft.fieldErrors["legs.$index." + if (arrival) "arriveLocal" else "departLocal"],
                    timing.minimumLocal(field)?.toLocalDate(),
                    if (arrival) leg.toCity else leg.fromCity,
                )
            }
            val cards =
                (start until end).map { index ->
                    val leg = draft.form.legs[index]
                    WizardFlightCardUi(
                        leg.id,
                        index,
                        leg.fromIata,
                        leg.toIata,
                        field(index, false),
                        field(index, true),
                        if (index ==
                            start
                        ) {
                            null
                        } else {
                            WizardOutboundRules.connectionFact(draft.form, index, draft.airports)
                        },
                    )
                }
            return WizardTimesUi(
                home,
                cards,
                flightTimesComplete(draft, home),
                draft.fieldErrors["review"],
                if (cards.size == 1) WizardOutboundRules.flightMinutes(draft.form, start, draft.airports) else null,
                timeFormat,
                continueFailures,
                picker?.takeIf { it.index in start until end }?.let {
                    val field = WizardFlightTiming.Field(it.index, it.arrival)
                    val time = WizardPickerDates.initialTime(draft, field, now)
                    WizardFlightPickerUi(it, WizardPickerDates.initialDate(draft, field, now), time.hour, time.minute)
                },
            )
        }
    }
}

internal fun flightTimesComplete(
    draft: MemberWizardDraft,
    home: Boolean,
): Boolean {
    val form = draft.form
    if (home && form.end.kind != "return_legs") return false
    val legs = if (home) form.legs.drop(form.returnBoundary) else form.legs.take(form.returnBoundary)
    return legs.all {
        (!home || it.toIata.isNotEmpty()) && WizardDateTime.date(it.departLocal) != null &&
            WizardDateTime.date(it.arriveLocal) != null
    }
}
