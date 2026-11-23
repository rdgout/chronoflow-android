package net.chronoflow.app.perf

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.chronoflow.core.wizard.model.MemberWizardDraft
import net.chronoflow.core.wizard.model.MemberWizardState
import net.chronoflow.feature.wizard.debug.WizardSaveJourney
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.Locale
import java.util.UUID

class PerfWizardJourneyTest {
    @Test fun journeyEqualsWizardSaveJourneyApartFromFreshLegIdentity() {
        val base =
            MemberWizardDraft(
                state =
                    MemberWizardState(
                        instance = "fixed-instance",
                        revision = 7,
                        firstTrip = false,
                        profile = JsonObject(mapOf("chronotype" to JsonPrimitive("intermediate"))),
                        sleepNeeded = false,
                    ),
            )
        val now = Instant.parse("2027-02-15T12:00:00Z")
        val locale = Locale.forLanguageTag("nl-NL")
        val expected = WizardSaveJourney.apply(base, locale, now)
        val actual = PerfWizardJourney.apply(base, locale, now)
        assertEquals(expected.form.legs.size, actual.form.legs.size)
        val legs =
            actual.form.legs.zip(expected.form.legs) { a, e ->
                assertEquals(a.id, UUID.fromString(a.id).toString())
                a.copy(id = e.id)
            }
        assertEquals(expected, actual.copy(form = actual.form.copy(legs = legs)))
    }
}
