package net.chronoflow.feature.wizard

import net.chronoflow.core.wizard.model.MemberWizardStep
import net.chronoflow.core.wizard.model.WizardDateTime
import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.util.Locale

class WizardSaveJourneySeedTest {
    @Test fun journeyUsesTheInjectedNowAndIsNeverFrozenOrCaptured() {
        val now = Instant.parse("2027-02-15T12:00:00Z")
        val draft = WizardBoardCaptureSeed.draft("WizardSaveJourney", Locale.ENGLISH, now)
        assertTrue(WizardBoardCaptureSeed.knows("WizardSaveJourney"))
        assertFalse("WizardSaveJourney" in WizardBoardCaptureSeed.names)
        assertNull(WizardBoardCaptureSeed.uiOverride("WizardSaveJourney"))
        assertEquals(MemberWizardStep.returnChoice, draft.state.step)
        assertEquals(
            "HND",
            draft.form.legs
                .single()
                .toIata,
        )
        assertEquals(
            "Asia/Tokyo",
            draft.state.airports
                .getValue("HND")
                .timezone,
        )
        assertTrue(
            Duration
                .between(
                    now,
                    WizardDateTime
                        .date(
                            draft.form.legs
                                .single()
                                .departLocal,
                        )!!
                        .toInstant(java.time.ZoneOffset.UTC),
                ).toDays() >=
                29,
        )
        assertNull(draft.form.identity)
        assertNull(draft.state.review)
    }
}
