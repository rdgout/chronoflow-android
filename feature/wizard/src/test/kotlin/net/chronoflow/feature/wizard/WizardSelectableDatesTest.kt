@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.feature.wizard

import net.chronoflow.core.wizard.model.WizardPickerDates
import net.chronoflow.feature.wizard.times.WizardSelectableDates
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WizardSelectableDatesTest {
    @Test fun nativePickerReceivesOnlyTheAirportLocalLowerBound() {
        val minimum = LocalDate.of(2040, 3, 25)
        val dates = WizardSelectableDates(minimum)
        assertFalse(dates.isSelectableDate(WizardPickerDates.utcMillis(minimum.minusDays(1))))
        assertTrue(dates.isSelectableDate(WizardPickerDates.utcMillis(minimum)))
        assertTrue(dates.isSelectableDate(WizardPickerDates.utcMillis(minimum.plusDays(1))))
        assertTrue(dates.isSelectableDate(WizardPickerDates.utcMillis(LocalDate.of(9999, 12, 31))))
        assertFalse(dates.isSelectableYear(2039))
        assertTrue(dates.isSelectableYear(2040))
    }

    @Test fun noMinimumAcceptsEverything() {
        val dates = WizardSelectableDates(null)
        assertTrue(dates.isSelectableDate(Long.MIN_VALUE))
        assertTrue(dates.isSelectableDate(Long.MAX_VALUE))
        assertTrue(dates.isSelectableYear(1))
        assertTrue(dates.isSelectableYear(9999))
    }
}
