package net.chronoflow.core.wizard.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.ApiReviewIdentity
import net.chronoflow.core.model.contracts.ApiTripInputSnapshot
import net.chronoflow.core.wizard.generationInput
import net.chronoflow.core.wizard.wizardAirport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class WizardFormTests {
    class Basics {
        @Test
        fun independentDefaults() {
            val first = WizardForm()
            val second = WizardForm()
            assertEquals(1, first.legs.size)
            assertEquals(listOf(WizardStay()), first.stays)
            assertNotEquals(first.legs[0].id, second.legs[0].id)
            assertNull(first.identity)
            assertNull(first.itinerary)
            assertEquals("one_way", first.end.kind)
            assertEquals(3, first.preparation.days)
        }

        @Test
        fun returnRetentionLimitsAndOutboundPropagation() {
            var form =
                WizardForm(end = WizardEnd(kind = "nights", nights = 8))
                    .updateLeg(0) { it.copy(toIata = "NRT", toCity = "Tokyo") }
                    .addReturn()
                    .updateLeg(1) { it.copy(flightNumber = "EK 148") }
            assertNull(form.takeFlight(0))
            assertNull(form.takeFlight(1))
            val returnedID = form.legs[1].id
            form = form.addFlight()
            assertEquals("NRT", form.legs[1].fromIata)
            assertEquals(returnedID, form.legs[2].id)
            assertEquals(2, form.returnBoundary)
            form = form.updateLeg(1) { it.copy(toIata = "SIN", toCity = "Singapore") }
            assertEquals("SIN", form.legs[2].fromIata)
            val removal = form.removeReturn()
            assertEquals("nights", removal.form.end.kind)
            assertEquals(8, removal.form.end.nights)
            form = removal.form.addReturn(removal.retained)
            assertEquals("EK 148", form.legs.last().flightNumber)
            assertEquals("SIN", form.legs[form.returnBoundary].fromIata)
            form = (form.legs.size until WizardForm.maxFlights).fold(form) { value, _ -> value.addFlight(home = true) }
            assertEquals(form, form.addFlight().addFlight(home = true))
            assertEquals(WizardForm.maxFlights, form.stays.size)
        }

        @Test
        fun stopSplitsMiddleTimesAndRemovalJoinsEndpoints() {
            val before = WizardForm.from(generationInput())
            var form = before.insertStop(wizardAirport("DXB", "Dubai", "Asia/Dubai"), 0)
            assertEquals(2, form.legs.size)
            assertEquals(2, form.stays.size)
            assertEquals(before.legs[0].departLocal, form.legs[0].departLocal)
            assertTrue(form.legs[0].arriveLocal.isEmpty())
            assertTrue(form.legs[1].departLocal.isEmpty())
            assertTrue(form.legs[1].arriveLocal.isEmpty())
            form = form.updateLeg(1) { it.copy(arriveLocal = before.legs[0].arriveLocal) }.removeStop(1)
            assertEquals(before, form)
            assertEquals(before, before.removeStop(0))
        }
    }

    @RunWith(Parameterized::class)
    class Removal(
        private val index: Int,
    ) {
        @Test
        fun removeRestoreKeepsStayBoundaryAndLaterChanges() {
            val original = WizardForm().addFlight().addReturn().addFlight(home = true)
            val taken = original.takeFlight(index)!!
            assertEquals(if (index < 2) 1 else 2, taken.form.end.returnFromLeg)
            assertEquals(3, taken.form.legs.size)
            assertEquals(3, taken.form.stays.size)
            assertEquals(original, taken.form.restoreFlight(taken.removed))
            assertEquals(original, original.restoreFlight(taken.removed))
            val edited =
                taken.form.updateLeg(0) { it.copy(flightNumber = "KL 861") }.let {
                    it.copy(
                        stays =
                            it.stays.mapIndexed { i, stay ->
                                if (i ==
                                    0
                                ) {
                                    stay.copy(arrivalTransferMin = 75)
                                } else {
                                    stay
                                }
                            },
                    )
                }
            val id = edited.legs[0].id
            val restored = edited.restoreFlight(taken.removed)
            assertEquals("KL 861", restored.legs.first { it.id == id }.flightNumber)
            assertEquals(75, restored.stays[restored.legs.indexOfFirst { it.id == id }].arrivalTransferMin)
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "index={0}")
            fun cases() = listOf(0, 1, 2, 3)
        }
    }

    @RunWith(Parameterized::class)
    class Mapping(
        private val kind: String,
    ) {
        @Test
        fun roundTripsAllFieldsWithoutLeakingLocalMetadata() {
            var form = WizardForm.from(generationInput()).addReturn()
            form =
                form.copy(
                    end = WizardEnd(kind, 5, "2027-01-15", if (kind == "return_legs") 1 else null),
                    legs =
                        form.legs.mapIndexed { index, leg ->
                            leg.copy(
                                fromCity = "Origin $index",
                                toCity = "Destination $index",
                                flightNumber = "CF$index",
                                seatLieFlat = true,
                                seatWindow = if (index == 0) "left" else "right",
                                leaveForAirportLocal = "2027-01-10T07:15",
                                airportTransferMin = 90,
                            )
                        },
                    stays =
                        form.stays.map {
                            WizardStay(
                                "declared",
                                "2027-01-10T15:00",
                                "2027-01-15T11:00",
                                true,
                                45,
                                "self_driven",
                                listOf(WizardBusy(startLocal = "2027-01-10T10:00", endLocal = "2027-01-10T12:00")),
                            )
                        },
                    override = WizardSleepOverride(true, "22:15", "06:30", "early"),
                    identity = ApiReviewIdentity("draft-token", 2, 3, "source", null),
                )
            form =
                form.copy(
                    preparation =
                        WizardPreparation(
                            4,
                            true,
                            "08:15",
                            "00:30",
                            "09:00",
                            true,
                            form.stays[0].busyIntervals,
                        ),
                )
            val input = form.reviewInput(form.identity, JsonObject(mapOf("age_band" to JsonPrimitive("18_64"))))
            assertEquals(form, WizardForm.from(input, retained = form))
            assertEquals(if (kind == "return_legs") 1 else 2, input.input.outbound.legs.size)
            val json = ApiJson.request.encodeToString(input)
            assertFalse(json.contains("fromCity"))
            assertFalse(json.contains("Origin"))
            assertFalse(json.contains(form.legs[0].id))
            assertNull(form.reviewInput(null).identity)
            val snapshot =
                ApiTripInputSnapshot(
                    input.input,
                    "source",
                    mapOf("AMS" to wizardAirport("AMS", "Amsterdam", "Europe/Amsterdam")),
                    null,
                )
            assertEquals("Amsterdam", WizardForm.from(snapshot).legs[0].fromCity)
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "kind={0}")
            fun cases() = listOf("return_legs", "nights", "check_out", "one_way")
        }
    }
}
