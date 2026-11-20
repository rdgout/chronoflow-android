package net.chronoflow.core.wizard.model

import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.wizard.generationInput
import net.chronoflow.core.wizard.wizardAirport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class WizardValidationTests {
    class Basics {
        @Test fun blankErrorsMatchRN() {
            assertEquals(
                mapOf(
                    "legs.0.fromIata" to FieldMessage("Choose the airport you leave from."),
                    "legs.0.toIata" to FieldMessage("Choose the airport you land at."),
                    "legs.0.departLocal" to FieldMessage("Please fill this in."),
                    "legs.0.arriveLocal" to FieldMessage("Please fill this in."),
                ),
                WizardValidation.validate(WizardForm()),
            )
        }

        @Test fun airportCountsSleepBusyAndLimits() {
            var form =
                WizardForm.from(generationInput()).updateLeg(0) {
                    it.copy(toIata = it.fromIata, airportTransferMin = -1, flightNumber = "A".repeat(13))
                }
            form =
                form.copy(
                    end = WizardEnd(kind = "nights", nights = 0),
                    override = WizardSleepOverride(enabled = true, habitualBed = "25:00"),
                    preparation =
                        WizardPreparation(
                            days = 5,
                            feasibleNightBed = "21:00",
                            busyIntervals =
                                listOf(
                                    WizardBusy(startLocal = "2026-11-01T13:00", endLocal = "2026-11-01T12:00"),
                                ),
                        ),
                )
            assertEquals(
                setOf(
                    "legs.0.toIata",
                    "legs.0.airportTransferMin",
                    "legs.0.flightNumber",
                    "end.nights",
                    "override.habitualBed",
                    "override.habitualWake",
                    "preparation.days",
                    "preparation.feasibleNightBed",
                    "preparation.busyIntervals.0.endLocal",
                ),
                WizardValidation.validate(form).keys,
            )
            assertNotNull(WizardValidation.validate(form.copy(stays = emptyList()))["legs"])
            assertNotNull(
                WizardValidation.validate(
                    form.copy(end = WizardEnd(kind = "check_out", checkOutDate = "2026-02-30")),
                )["end.checkOutDate"],
            )
            assertNotNull(
                WizardValidation.validate(
                    form.copy(end = WizardEnd(kind = "return_legs", returnFromLeg = 0)),
                )["end.returnFromLeg"],
            )
        }

        @Test fun differentAirportWallTimesDoNotEstablishChronology() {
            val form =
                WizardForm.from(generationInput()).updateLeg(0) {
                    it.copy(
                        fromIata = "SYD",
                        toIata = "LAX",
                        departLocal = "2026-11-01T22:00",
                        arriveLocal = "2026-11-01T18:00",
                    )
                }
            assertTrue(WizardValidation.validate(form).isEmpty())
        }

        @Test fun leaveAndConnectionOrderAndContinuity() {
            var form =
                WizardForm
                    .from(
                        generationInput(),
                    ).updateLeg(0) { it.copy(leaveForAirportLocal = it.departLocal) }
            assertNotNull(WizardValidation.validate(form)["legs.0.leaveForAirportLocal"])
            form =
                form.addFlight().updateLeg(1) {
                    it.copy(toIata = "SIN", departLocal = form.legs[0].arriveLocal, arriveLocal = "2026-11-15T12:00")
                }
            val airport = wizardAirport(form.legs[1].fromIata, "Tokyo", "Asia/Tokyo")
            assertNotNull(WizardValidation.validate(form, mapOf(airport.iata to airport))["legs.1.departLocal"])
            form = form.updateLeg(1) { it.copy(fromIata = "DXB") }
            assertEquals(
                "Start this flight where the previous flight lands.",
                WizardValidation.validate(form)["legs.1.fromIata"]?.key,
            )
        }

        @Test fun errorMappingAndSections() {
            val form = WizardForm().addFlight().addReturn().addFlight(home = true)
            val message = FieldMessage("error", mapOf("number" to JsonPrimitive(2)))
            assertEquals(
                mapOf("legs.1.arriveLocal" to message),
                WizardValidation.validationErrors(
                    mapOf(
                        "review_expired" to FieldMessage("expired"),
                        "review" to FieldMessage("review"),
                        "credits" to FieldMessage("credits"),
                        "legs.1.arrive_local" to message,
                    ),
                    form,
                ),
            )
        }
    }

    @RunWith(Parameterized::class)
    class InvalidDeparture(
        private val departure: String,
    ) {
        @Test fun leaveTimeDoesNotCompareAgainstInvalidDeparture() {
            val form =
                WizardForm.from(generationInput()).updateLeg(0) {
                    it.copy(departLocal = departure, leaveForAirportLocal = "2026-11-01T12:00")
                }
            val errors = WizardValidation.validate(form)
            assertEquals(
                if (departure.isEmpty()) "Please fill this in." else "Enter a valid date and time.",
                errors["legs.0.departLocal"]?.key,
            )
            assertNull(errors["legs.0.leaveForAirportLocal"])
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "departure={0}")
            fun cases() = listOf("", "2026-02-30T12:00", "invalid")
        }
    }

    @RunWith(Parameterized::class)
    class Mapping(
        private val path: String,
        private val mapped: String,
    ) {
        @Test fun errorMappingAndSections() {
            val form = WizardForm().addFlight().addReturn().addFlight(home = true)
            assertEquals(mapped, WizardJourney.errorPath(path, form))
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "path={0}")
            fun cases() =
                listOf(
                    arrayOf("outbound.legs.0.arrive_local", "legs.0.arriveLocal"),
                    arrayOf("return.legs.0.flight_number", "legs.2.flightNumber"),
                    arrayOf("return.legs.0.stay.busy_intervals.2.start_local", "stays.2.busyIntervals.2.startLocal"),
                    arrayOf("outbound.legs.0.stay", "stays.0"),
                    arrayOf("trip_end_nights", "end.nights"),
                    arrayOf("pre_departure_busy_intervals.0.end_local", "preparation.busyIntervals.0.endLocal"),
                    arrayOf("override.use", "override.enabled"),
                    arrayOf("profile.cups_filter_coffee", "profile.cupsFilterCoffee"),
                )
        }
    }

    @RunWith(Parameterized::class)
    class Sections(
        private val path: String,
        private val section: String,
    ) {
        @Test fun errorMappingAndSections() {
            val form = WizardForm().addFlight().addReturn().addFlight(home = true)
            assertEquals(section, WizardValidation.errorSection(path, form).key)
            assertEquals(section, WizardValidation.errorSection(WizardJourney.errorPath(path, form), form).key)
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "path={0}")
            fun cases(): List<Array<String>> =
                listOf(
                    "legs.0.leave_for_airport_local" to "Leaving for the airport",
                    "legs.0.airport_transfer_min" to "Leaving for the airport",
                    "stays.1.bed_available_from_local" to "Arrival",
                    "stays.1.check_out_local" to "End of stay",
                    "stays.2.busy_intervals.0.end_local" to "Nights at your destination",
                    "trip_end_nights" to "Nights at your destination",
                    "trip_end_check_out_date" to "End of stay",
                    "preparation_days" to "Preparation",
                    "fast_delay_opt_in" to "Preparation",
                    "latest_wake" to "Preparation",
                    "feasible_night_bed" to "Preparation",
                    "feasible_night_wake" to "Preparation",
                    "pre_departure_busy_answered" to "Preparation",
                    "pre_departure_busy_intervals.0.end_local" to "Preparation",
                    "use_profile_override" to "Your sleep for this trip",
                    "override_habitual_bed" to "Your sleep for this trip",
                    "override_habitual_wake" to "Your sleep for this trip",
                    "override_chronotype" to "Your sleep for this trip",
                ).map { arrayOf(it.first, it.second) } +
                    listOf(
                        "legs",
                        "end.kind",
                        "end.returnFromLeg",
                        "trip_end_kind",
                        "return_from_leg",
                        "stays",
                        "legs.99.departLocal",
                        "stays.99.checkOutLocal",
                        "legs.invalid.departLocal",
                        "unknown",
                    ).map {
                        arrayOf(it, "Your flights")
                    }
        }
    }

    @RunWith(Parameterized::class)
    class FlightSections(
        private val index: Int,
        private val field: String,
    ) {
        @Test fun errorMappingAndSections() {
            val form = WizardForm().addFlight().addReturn().addFlight(home = true)
            assertEquals(
                FieldMessage(
                    if (index <
                        2
                    ) {
                        "Outbound, flight {{number}}"
                    } else {
                        "Return, flight {{number}}"
                    },
                    mapOf("number" to JsonPrimitive(index + 1)),
                ),
                WizardValidation.errorSection("legs.$index.$field", form),
            )
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}-{1}")
            fun cases() =
                (0..3).flatMap { index ->
                    listOf(
                        "fromIata",
                        "toIata",
                        "departLocal",
                        "arriveLocal",
                        "flightNumber",
                        "seatLieFlat",
                        "seatWindow",
                    ).map {
                        arrayOf<Any>(index, it)
                    }
                }
        }
    }
}
