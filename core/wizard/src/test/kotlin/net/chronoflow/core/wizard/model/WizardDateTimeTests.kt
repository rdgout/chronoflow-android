package net.chronoflow.core.wizard.model

import net.chronoflow.core.model.contracts.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.TimeZone

@RunWith(Enclosed::class)
class WizardDateTimeTests {
    class Basics {
        @Test fun leapDaysAndMonths() {
            assertEquals("2028-02-29", WizardDateTime.shiftDate("2028-02-28", 1))
            assertEquals("2027-01-01", WizardDateTime.shiftDate("2026-12-31", 1))
            // OQ-9a-7: Android uses separate date and time pills; formatLocal is not ported.
        }

        @Test fun actualPhoneGapIsValidAtAnotherAirport() {
            val value = "2027-03-28T02:30"
            assertNull(WizardDateTime.instant(value, "Europe/Amsterdam"))
            assertNotNull(WizardDateTime.instant(value, "Asia/Tokyo"))
            val picker = WizardDateTime.pickerDate(value)!!
            assertEquals(value, WizardDateTime.pickedValue(picker))
            assertEquals("2027-03-29", WizardDateTime.shiftDate(value, 1))
            assertEquals("2027-03-29T02:30", WizardDateTime.pickedValue(picker.plusDays(1)))
            assertEquals("2026-11-02T12:00", WizardDateTime.pickerValue(null, "2026-11-02"))
            assertEquals("23:00", WizardDateTime.pickerValue("23:00", "2026-11-02", false))
            assertEquals("02:30", WizardDateTime.pickedValue(WizardDateTime.pickerDate("02:30", false)!!, false))
            assertEquals("invalid", WizardDateTime.formatWeekdayTime("invalid", Language.En))
        }
    }

    @RunWith(Parameterized::class)
    class Invalid(
        private val value: String,
    ) {
        @Test
        fun rejectsInvalid() {
            assertNull(WizardDateTime.date(value))
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}")
            fun cases() =
                listOf(
                    "2026-02-30T12:00",
                    "2026-11-01T24:00",
                    "2026-11-01T12:60",
                    "2026-11-01",
                    "",
                    "invalid",
                    "0000-01-01T12:00",
                )
        }
    }

    @RunWith(Parameterized::class)
    class Picker(
        private val value: String,
        private val zone: String,
    ) {
        @Test fun pickerPreservesMinutesAcrossDeviceDSTChanges() {
            val previous = TimeZone.getDefault()
            try {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                val date = WizardDateTime.pickerDate(value)!!
                assertEquals(value, WizardDateTime.pickedValue(date))
                assertEquals(
                    value,
                    WizardDateTime.pickedValue(
                        WizardDateTime.fromPickerMillis(WizardDateTime.pickerMillis(date)),
                    ),
                )
            } finally {
                TimeZone.setDefault(previous)
            }
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}-{1}")
            fun cases() =
                listOf(
                    "2026-11-01T10:07",
                    "2027-01-01T00:00",
                    "2027-03-28T02:30",
                    "2026-10-25T02:30",
                ).flatMap { value ->
                    listOf("Europe/Amsterdam", "America/New_York", "Asia/Tokyo").map { zone -> arrayOf(value, zone) }
                }
        }
    }

    @RunWith(Parameterized::class)
    class Weekday(
        private val value: String,
        private val language: Language,
        private val twelveHour: Boolean,
        private val expected: String,
    ) {
        @Test
        fun weekdayAndTimeRespectLocaleAndClock() {
            assertEquals(
                expected,
                WizardDateTime.formatWeekdayTime(value, language, twelveHour),
            )
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}-{1}-{2}")
            fun cases() =
                listOf(
                    arrayOf("2026-10-12T23:25", Language.En, false, "Mon 23:25"),
                    arrayOf("2026-10-13T18:05", Language.En, false, "Tue 18:05"),
                    arrayOf("2026-10-12T23:25", Language.Nl, false, "ma 23:25"),
                    arrayOf("2026-10-13T18:05", Language.Nl, false, "di 18:05"),
                    arrayOf("2026-10-12T23:25", Language.En, true, "Mon 11:25 PM"),
                    arrayOf("2026-10-13T18:05", Language.Nl, true, "di 06:05 p.m."),
                    arrayOf("2026-11-01T00:07", Language.En, false, "Sun 00:07"),
                    arrayOf("2026-11-01T00:07", Language.En, true, "Sun 12:07 AM"),
                    arrayOf("2026-11-01T12:00", Language.Nl, true, "zo 12:00 p.m."),
                    arrayOf("2026-12-31T23:59", Language.En, false, "Thu 23:59"),
                    arrayOf("2027-01-01T00:01", Language.Nl, false, "vr 00:01"),
                )
        }
    }
}
