package net.chronoflow.core.wizard.model

import net.chronoflow.core.wizard.wizardAirport
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class WizardReviewFlowTests {
    @Test fun returnPastUsesFinalArrivalAndAirportZones() {
        var form =
            WizardForm()
                .updateLeg(0) {
                    it.copy(
                        fromIata = "AMS",
                        toIata = "HND",
                        departLocal = "2026-10-10T14:00",
                        arriveLocal = "2026-10-11T09:00",
                    )
                }.addReturn()
                .updateLeg(1) {
                    it.copy(toIata = "AMS", departLocal = "2026-11-12T10:00", arriveLocal = "2026-11-12T18:00")
                }
        val airports =
            mapOf(
                "AMS" to wizardAirport("AMS", "Amsterdam", "Europe/Amsterdam"),
                "HND" to wizardAirport("HND", "Tokyo", "Asia/Tokyo"),
            )
        val now = Instant.parse("2026-11-09T12:00:00Z")
        assertTrue(WizardFlightRules.errors(form, airports, now, true).isEmpty())
        form = form.updateLeg(1) { it.copy(departLocal = "2026-10-12T10:00", arriveLocal = "2026-10-12T18:00") }
        assertTrue(WizardFlightRules.errors(form, airports, now, false).isEmpty())
        assertNotNull(WizardFlightRules.errors(form, airports, now, true)["review"])
        form = form.updateLeg(1) { it.copy(arriveLocal = "2026-10-11T18:00") }
        assertNotNull(WizardFlightRules.errors(form, airports, now, true)["legs.1.arriveLocal"])
    }
}
