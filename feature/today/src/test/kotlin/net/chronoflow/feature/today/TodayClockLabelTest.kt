package net.chronoflow.feature.today

import net.chronoflow.core.plan.calendarDaysUntil
import net.chronoflow.feature.today.ui.completedDate
import net.chronoflow.feature.today.ui.relativeMinutes
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TodayClockLabelTest {
    @Test fun departureTodayIsZeroDays() {
        assertEquals(0, calendarDaysUntil(todayNow.toString(), todayNow, "UTC"))
    }

    @Test fun departureSixLocalDaysAheadIsSixDays() {
        assertEquals(6, calendarDaysUntil(todayNow.plusSeconds(6 * 86400).toString(), todayNow, "Europe/Amsterdam"))
    }

    @Test fun remainingMinuteRoundsUp() {
        assertEquals(1, relativeMinutes(todayNow.plusMillis(1), todayNow))
    }

    @Test fun endedStepHasNoRemainingMinutes() {
        assertEquals(0, relativeMinutes(todayNow.minusSeconds(1), todayNow))
    }

    @Test fun completedDateCannotExceedTheDeviceLocalDate() {
        assertEquals("2026-11-08", completedDate("2026-11-09", LocalDate.of(2026, 11, 8)))
    }
}
