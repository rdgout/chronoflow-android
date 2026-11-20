package net.chronoflow.core.wizard.model

import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.model.contracts.ApiAirportSuggestion
import net.chronoflow.core.model.contracts.SegmentKindCode
import net.chronoflow.core.model.contracts.TripDirection
import java.time.Duration
import java.time.Instant

object WizardOutboundRules {
    fun errors(
        form: WizardForm,
        airports: Map<String, ApiAirportSuggestion>,
        now: Instant,
        checkPast: Boolean,
    ): Map<String, FieldMessage> {
        val errors =
            WizardValidation
                .validate(form, airports)
                .filterKeys { key ->
                    (0 until form.returnBoundary).any { key.startsWith("legs.$it.") }
                }.toMutableMap()
        val timing = WizardFlightTiming(form, airports)
        form.legs.take(form.returnBoundary).forEachIndexed { index, _ ->
            val departure = timing.instant(WizardFlightTiming.Field.departure(index))
            val arrival = timing.instant(WizardFlightTiming.Field.arrival(index))
            if (departure != null && arrival != null &&
                arrival <= departure
            ) {
                errors["legs.$index.arriveLocal"] =
                    FieldMessage("Check the departure and arrival times.")
            }
        }
        val arrival = timing.instant(WizardFlightTiming.Field.arrival(form.returnBoundary - 1))
        if (checkPast && form.end.kind != "return_legs" && errors.isEmpty() && arrival != null && arrival <= now) {
            errors["review"] =
                FieldMessage("A new plan needs an upcoming arrival. Check your flight dates or add your return flight.")
        }
        return errors
    }

    fun connection(
        form: WizardForm,
        index: Int,
        airports: Map<String, ApiAirportSuggestion> = emptyMap(),
        duration: (Int) -> String,
    ): FieldMessage? {
        if (index <= 0 || index !in form.legs.indices || index == form.returnBoundary) return null
        val leg = form.legs[index - 1]
        val airport = leg.toCity ?: leg.toIata
        if (airport.isEmpty()) return null
        val home = index >= form.returnBoundary
        val localIndex = index - 1 - if (home) form.returnBoundary else 0
        val fact =
            form.itinerary?.layovers?.firstOrNull {
                it.journey ==
                    (if (home) TripDirection.Return else TripDirection.Outbound) &&
                    it.afterLegIndex == localIndex
            }
        if (fact !=
            null
        ) {
            return connectionMessage(
                airport,
                fact.durationMin,
                if (fact.kind ==
                    SegmentKindCode.Connection
                ) {
                    0
                } else {
                    fact.nights
                },
                duration,
            )
        }
        val zone = airports[leg.toIata]?.timezone
        if (zone != null) {
            val arrival = WizardDateTime.instant(leg.arriveLocal, zone)
            val departure = WizardDateTime.instant(form.legs[index].departLocal, zone)
            if (arrival != null && departure != null &&
                departure > arrival
            ) {
                return connectionMessage(
                    airport,
                    Duration.between(arrival, departure).toMinutes().toInt(),
                    completeNights(leg.arriveLocal, form.legs[index].departLocal, zone),
                    duration,
                )
            }
        }
        return FieldMessage("Stop in {{airport}}", mapOf("airport" to JsonPrimitive(airport)))
    }

    fun flightMinutes(
        form: WizardForm,
        index: Int,
        airports: Map<String, ApiAirportSuggestion>,
    ): Int? {
        if (index !in form.legs.indices) return null
        val timing = WizardFlightTiming(form, airports)
        val departure = timing.instant(WizardFlightTiming.Field.departure(index)) ?: return null
        val arrival = timing.instant(WizardFlightTiming.Field.arrival(index)) ?: return null
        return if (arrival > departure) Duration.between(departure, arrival).toMinutes().toInt() else null
    }

    private fun completeNights(
        arrival: String,
        departure: String,
        zone: String,
    ): Int {
        val start = WizardDateTime.date(arrival) ?: return 0
        val end = WizardDateTime.date(departure) ?: return 0
        val landing = WizardDateTime.instant(arrival, zone) ?: return 0
        val takeoff = WizardDateTime.instant(departure, zone) ?: return 0
        var day = start.toLocalDate().atStartOfDay()
        var count = 0
        // Match the planner's complete local 23:00–07:00 windows, including DST nights.
        while (day < end) {
            val next = day.plusDays(1)
            val bed = WizardDateTime.instant(WizardDateTime.string(day).take(10) + "T23:00", zone)
            val wake = WizardDateTime.instant(WizardDateTime.string(next).take(10) + "T07:00", zone)
            if (bed != null && wake != null && bed >= landing && wake <= takeoff) count++
            day = next
        }
        return count
    }

    private fun connectionMessage(
        airport: String,
        minutes: Int,
        nights: Int,
        duration: (Int) -> String,
    ): FieldMessage {
        if (nights > 0) {
            return FieldMessage(
                if (nights ==
                    1
                ) {
                    "Stop in {{airport}} · {{count}} complete night"
                } else {
                    "Stop in {{airport}} · {{count}} complete nights"
                },
                mapOf("airport" to JsonPrimitive(airport), "count" to JsonPrimitive(nights)),
            )
        }
        return FieldMessage(
            "Connection in {{airport}} · {{duration}}",
            mapOf(
                "airport" to JsonPrimitive(airport),
                "duration" to JsonPrimitive(duration(minutes)),
            ),
        )
    }
}

object WizardFlightRules {
    fun errors(
        form: WizardForm,
        airports: Map<String, ApiAirportSuggestion>,
        now: Instant,
        checkPast: Boolean,
        mode: WizardMode = WizardMode.member,
    ): Map<String, FieldMessage> {
        val errors = WizardValidation.validate(form, airports).filterKeys { it.startsWith("legs.") }.toMutableMap()
        val timing = WizardFlightTiming(form, airports)
        form.legs.forEachIndexed { index, _ ->
            val departure = timing.instant(WizardFlightTiming.Field.departure(index))
            val arrival = timing.instant(WizardFlightTiming.Field.arrival(index))
            if (departure != null && arrival != null &&
                arrival <= departure
            ) {
                errors["legs.$index.arriveLocal"] =
                    FieldMessage("Check the departure and arrival times.")
            }
        }
        val arrival = timing.instant(WizardFlightTiming.Field.arrival(form.legs.lastIndex))
        if (checkPast && errors.isEmpty() && arrival != null && arrival <= now) {
            errors["review"] =
                FieldMessage(
                    if (mode == WizardMode.guest ||
                        form.end.kind == "return_legs"
                    ) {
                        "A new plan needs an upcoming arrival. Check your flight dates."
                    } else {
                        "A new plan needs an upcoming arrival. Check your flight dates or add your return flight."
                    },
                )
        }
        return errors
    }
}
