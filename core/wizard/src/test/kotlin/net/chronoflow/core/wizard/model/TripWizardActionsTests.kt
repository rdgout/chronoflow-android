package net.chronoflow.core.wizard.model

import net.chronoflow.core.testing.wizard.generationInput
import net.chronoflow.core.testing.wizard.generationReview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TripWizardActionsTests(
    private val end: String,
) {
    @Test fun duplicateClearsEveryDateBoundAnswerAndRetainsRouteAndOtherAnswers() {
        var form = WizardForm.from(generationInput())
        form =
            if (end ==
                "return_legs"
            ) {
                form.addReturn().updateLeg(1) { it.copy(toIata = form.legs[0].fromIata) }
            } else {
                form.copy(end = WizardEnd(kind = end))
            }
        val busy = listOf(WizardBusy(startLocal = "2030-10-10T09:00", endLocal = "2030-10-10T17:00"))
        form =
            form.copy(
                end = form.end.copy(nights = 9, checkOutDate = "2030-10-22"),
                identity = generationReview().identity,
                override = WizardSleepOverride(enabled = true, habitualBed = "00:30"),
                preparation = WizardPreparation(busyAnswered = true, busyIntervals = busy),
                legs =
                    form.legs.map {
                        it.copy(
                            seatWindow = "left",
                            seatLieFlat = true,
                            leaveForAirportLocal = "2030-10-12T10:00",
                            airportTransferMin = 90,
                        )
                    },
                stays =
                    form.stays.map {
                        it.copy(
                            bedAvailableMode = "declared",
                            bedAvailableFromLocal = "2030-10-13T15:00",
                            checkOutLocal = "2030-10-22T11:00",
                            busyIntervals = busy,
                            transferMode = "passenger",
                        )
                    },
            )
        val original = form
        form = form.clearDuplicateDates()
        assertNull(form.identity)
        assertNull(form.itinerary)
        assertEquals(original.legs.map { it.fromIata }, form.legs.map { it.fromIata })
        assertEquals(original.legs.map { it.toIata }, form.legs.map { it.toIata })
        assertTrue(
            form.legs.all {
                it.departLocal.isEmpty() && it.arriveLocal.isEmpty() &&
                    it.leaveForAirportLocal == null
            },
        )
        assertTrue(form.legs.all { it.seatWindow == "left" && it.seatLieFlat && it.airportTransferMin == 90 })
        assertTrue(
            form.stays.all {
                it.bedAvailableMode == null && it.bedAvailableFromLocal == null &&
                    it.checkOutLocal == null &&
                    it.busyIntervals.isEmpty() &&
                    it.transferMode == "passenger"
            },
        )
        assertTrue(form.preparation.busyIntervals.isEmpty())
        assertFalse(form.preparation.busyAnswered)
        assertEquals(original.preparation.days, form.preparation.days)
        assertEquals(original.override, form.override)
        assertEquals(if (end == "return_legs") "return_legs" else "one_way", form.end.kind)
        assertNull(form.end.nights)
        assertNull(form.end.checkOutDate)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "end={0}")
        fun cases() = listOf("return_legs", "nights", "check_out")
    }
}
