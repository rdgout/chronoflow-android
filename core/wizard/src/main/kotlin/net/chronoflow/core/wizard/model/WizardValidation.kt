package net.chronoflow.core.wizard.model

import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.contracts.ApiAirportSuggestion

object WizardValidation {
    fun validate(
        form: WizardForm,
        airports: Map<String, ApiAirportSuggestion> = emptyMap(),
    ): Map<String, FieldMessage> {
        val errors = linkedMapOf<String, FieldMessage>()
        val timing = WizardFlightTiming(form, airports)

        fun error(
            path: String,
            key: String = "The planner cannot use this value.",
        ) {
            errors[path] = FieldMessage(key)
        }

        fun dateTime(
            path: String,
            value: String?,
            required: Boolean = false,
        ) {
            if (value.isNullOrEmpty()) {
                if (required) error(path, "Please fill this in.")
            } else if (WizardDateTime.date(value) == null) {
                error(path, "Enter a valid date and time.")
            }
        }

        fun minutes(
            path: String,
            value: Int?,
        ) {
            if (value != null && value !in 0..65535) error(path)
        }

        fun busy(
            path: String,
            rows: List<WizardBusy>,
        ) {
            if (rows.size > 32) error(path)
            rows.forEachIndexed { index, row ->
                dateTime("$path.$index.startLocal", row.startLocal, true)
                dateTime("$path.$index.endLocal", row.endLocal, true)
                if (WizardDateTime.date(row.startLocal) != null && WizardDateTime.date(row.endLocal) != null &&
                    row.endLocal <= row.startLocal
                ) {
                    error("$path.$index.endLocal", "Busy time must end after it starts.")
                }
            }
        }
        if (form.legs.isEmpty() || form.legs.size > WizardForm.maxFlights ||
            form.stays.size != form.legs.size
        ) {
            error("legs")
        }
        form.legs.forEachIndexed { index, leg ->
            val path = "legs.$index"
            if (leg.fromIata.isEmpty()) error("$path.fromIata", "Choose the airport you leave from.")
            if (leg.toIata.isEmpty()) error("$path.toIata", "Choose the airport you land at.")
            if (leg.fromIata.isNotEmpty() &&
                leg.fromIata == leg.toIata
            ) {
                error("$path.toIata", "Choose different departure and arrival airports.")
            }
            dateTime("$path.departLocal", leg.departLocal, true)
            dateTime("$path.arriveLocal", leg.arriveLocal, true)
            dateTime("$path.leaveForAirportLocal", leg.leaveForAirportLocal)
            val leave = leg.leaveForAirportLocal
            if (leave != null && WizardDateTime.date(leave) != null && WizardDateTime.date(leg.departLocal) != null &&
                leave >= leg.departLocal
            ) {
                error("$path.leaveForAirportLocal", "Check the departure and arrival times.")
            }
            minutes("$path.airportTransferMin", leg.airportTransferMin)
            if ((leg.flightNumber?.length ?: 0) > 12) error("$path.flightNumber")
            if (index > 0) {
                val previous = form.legs[index - 1]
                if (previous.toIata.isNotEmpty() && leg.fromIata.isNotEmpty() && previous.toIata != leg.fromIata) {
                    error("$path.fromIata", "Start this flight where the previous flight lands.")
                }
                val arrival = timing.instant(WizardFlightTiming.Field.arrival(index - 1))
                val departure = timing.instant(WizardFlightTiming.Field.departure(index))
                if (arrival != null && departure != null &&
                    departure <= arrival
                ) {
                    error("$path.departLocal", "This flight leaves before the previous one lands.")
                }
            }
        }
        form.stays.forEachIndexed { index, stay ->
            val path = "stays.$index"
            dateTime("$path.bedAvailableFromLocal", stay.bedAvailableFromLocal, stay.bedAvailableMode == "declared")
            dateTime("$path.checkOutLocal", stay.checkOutLocal)
            minutes("$path.arrivalTransferMin", stay.arrivalTransferMin)
            busy("$path.busyIntervals", stay.busyIntervals)
        }
        if (form.end.kind == "nights" && (form.end.nights ?: 0) !in 1..60) error("end.nights")
        if (form.end.kind == "check_out" &&
            WizardDateTime.date((form.end.checkOutDate ?: "") + "T12:00") == null
        ) {
            error("end.checkOutDate", "Enter a valid date and time.")
        }
        if (form.end.kind == "return_legs" &&
            ((form.end.returnFromLeg ?: 0) < 1 || (form.end.returnFromLeg ?: 0) >= form.legs.size)
        ) {
            error("end.returnFromLeg")
        }
        val sleepError = "Check your bedtime and wake-up time."
        if (form.override.enabled) {
            if (!WizardDateTime.isTime(form.override.habitualBed)) error("override.habitualBed", sleepError)
            if (!WizardDateTime.isTime(form.override.habitualWake)) error("override.habitualWake", sleepError)
        }
        val prep = form.preparation
        if (prep.days !in 0..WizardForm.maxPreparationDays) error("preparation.days")
        for ((field, value) in listOf(
            "latestWake" to prep.latestWake,
            "feasibleNightBed" to prep.feasibleNightBed,
            "feasibleNightWake" to prep.feasibleNightWake,
        )) {
            if (value != null && !WizardDateTime.isTime(value)) error("preparation.$field", sleepError)
        }
        if ((prep.feasibleNightBed == null) !=
            (prep.feasibleNightWake == null)
        ) {
            error("preparation.feasibleNightBed", sleepError)
        }
        busy("preparation.busyIntervals", prep.busyIntervals)
        return errors
    }

    fun validationErrors(
        fields: Map<String, FieldMessage>,
        form: WizardForm,
    ): Map<String, FieldMessage> =
        WizardJourney.errors(fields, form).filterKeys { path ->
            listOf("legs", "stays", "end", "override", "preparation").any {
                path ==
                    it ||
                    path.startsWith("$it.")
            }
        }

    fun errorSection(
        path: String,
        form: WizardForm,
    ): FieldMessage {
        val parts = WizardJourney.errorPath(path, form).split('.')
        val section = parts.firstOrNull() ?: ""
        if (section == "preparation") return FieldMessage("Preparation")
        if (section == "override") return FieldMessage("Your sleep for this trip")
        val position = parts.getOrNull(1) ?: ""
        val field = parts.getOrNull(2) ?: ""
        if (section == "end") {
            if (position == "nights") return FieldMessage("Nights at your destination")
            if (position == "checkOutDate") return FieldMessage("End of stay")
        }
        val index = position.toIntOrNull()
        if (section in listOf("legs", "stays") && index != null && index in form.legs.indices) {
            if (section == "legs" && index == 0 &&
                (field.startsWith("leaveForAirport") || field == "airportTransferMin")
            ) {
                return FieldMessage("Leaving for the airport")
            }
            if (section == "stays") {
                if (field == "busyIntervals") return FieldMessage("Nights at your destination")
                if (index ==
                    form.returnBoundary - 1
                ) {
                    return FieldMessage(
                        if (field in
                            listOf("checkOutLocal", "lateCheckOutOrDayRoom")
                        ) {
                            "End of stay"
                        } else {
                            "Arrival"
                        },
                    )
                }
            }
            return FieldMessage(
                if (index <
                    form.returnBoundary
                ) {
                    "Outbound, flight {{number}}"
                } else {
                    "Return, flight {{number}}"
                },
                mapOf("number" to JsonPrimitive(index + 1)),
            )
        }
        return FieldMessage("Your flights")
    }
}
