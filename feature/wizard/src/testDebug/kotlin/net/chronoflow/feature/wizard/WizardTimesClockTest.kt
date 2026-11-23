package net.chronoflow.feature.wizard

import net.chronoflow.feature.wizard.debug.WizardBoardCaptureSeed
import net.chronoflow.feature.wizard.times.WizardPicker
import net.chronoflow.feature.wizard.times.WizardPickerKind
import net.chronoflow.feature.wizard.times.WizardTimesUi
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class WizardTimesClockTest {
    @Test fun emptyCardsStayEqualWhenTheClockMovesByOneMinute() {
        val draft = WizardBoardCaptureSeed.draft("WizardOutboundStopEmpty", Locale.ENGLISH)
        val now = WizardBoardCaptureSeed.now
        val first = WizardTimesUi.from(draft, false, now = now)
        val later = WizardTimesUi.from(draft, false, now = now.plusSeconds(60))
        assertEquals(first.cards[0], later.cards[0])
        assertEquals(first.cards[1], later.cards[1])
    }

    @Test fun pickerDefaultsUseTheOpeningClockWithoutChangingTheCards() {
        val draft = WizardBoardCaptureSeed.draft("WizardOutboundStopEmpty", Locale.ENGLISH)
        val now = WizardBoardCaptureSeed.now
        val picker = WizardPicker(1, false, WizardPickerKind.Time)
        val first = WizardTimesUi.from(draft, false, now = now, picker = picker)
        val later = WizardTimesUi.from(draft, false, now = now.plusSeconds(60), picker = picker)
        assertEquals(first.cards, later.cards)
        assertEquals(13, first.picker!!.initialHour)
        assertEquals(0, first.picker.initialMinute)
        assertEquals(1, later.picker!!.initialMinute)
    }
}
