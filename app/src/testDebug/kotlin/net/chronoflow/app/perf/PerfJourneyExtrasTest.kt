package net.chronoflow.app.perf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerfJourneyExtrasTest {
    @Test fun emptyMeansNone() = assertEquals(PerfJourneyExtras.None, PerfJourneyExtras.parse(emptyMap()))

    @Test fun demoAccountIsAccepted() =
        assertTrue(
            PerfJourneyExtras.parse(mapOf("cf.perf-account" to "demo")).demoAccount,
        )

    @Test fun otherAccountIsIgnored() {
        val extras = PerfJourneyExtras.parse(mapOf("cf.perf-account" to "other"))
        assertFalse(extras.demoAccount)
        assertEquals(listOf("cf.perf-account=other"), extras.ignored)
    }

    @Test fun wizardJourneyIsAccepted() =
        assertEquals(
            PerfWizard.WizardSaveJourney,
            PerfJourneyExtras
                .parse(
                    mapOf("cf.perf-wizard" to "WizardSaveJourney"),
                ).wizard,
        )

    @Test fun unknownWizardIsIgnored() =
        assertEquals(
            PerfJourneyExtras(false, null, listOf("cf.perf-wizard=unknown")),
            PerfJourneyExtras.parse(
                mapOf("cf.perf-wizard" to "unknown"),
            ),
        )
}
