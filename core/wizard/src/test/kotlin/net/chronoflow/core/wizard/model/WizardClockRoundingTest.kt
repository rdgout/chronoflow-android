package net.chronoflow.core.wizard.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale

@RunWith(Enclosed::class)
class WizardClockRoundingTest {
    @RunWith(Parameterized::class)
    class Nearest(
        private val hour: Int,
        private val minute: Int,
        private val expected: String,
    ) {
        @Test fun roundsToTheNearestFiveMinutes() {
            assertEquals(expected, WizardClockRounding.roundToStep(hour, minute))
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}:{1} to {2}")
            fun cases() =
                listOf(
                    arrayOf<Any>(7, 12, "07:10"),
                    arrayOf<Any>(7, 13, "07:15"),
                    arrayOf<Any>(7, 15, "07:15"),
                    arrayOf<Any>(0, 2, "00:00"),
                    arrayOf<Any>(12, 3, "12:05"),
                    arrayOf<Any>(23, 57, "23:55"),
                    arrayOf<Any>(23, 58, "00:00"),
                    arrayOf<Any>(0, 0, "00:00"),
                )
        }
    }

    @RunWith(Parameterized::class)
    class Invalid(
        private val hour: Int,
        private val minute: Int,
    ) {
        @Test fun rejectsOutOfRangeValues() {
            assertNull(WizardClockRounding.roundToStep(hour, minute))
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}:{1}")
            fun cases() = listOf(arrayOf<Any>(24, 0), arrayOf<Any>(12, 60), arrayOf<Any>(-1, 0))
        }
    }

    class Confirm {
        @Test fun wrapsPastMidnight() {
            assertEquals("00:00", WizardClockRounding.roundToStep(23, 58))
        }

        @Test fun outputIsZeroPaddedUnderAnyDefaultLocale() {
            val previous = Locale.getDefault()
            try {
                Locale.setDefault(Locale.forLanguageTag("ar-EG"))
                assertEquals("07:05", WizardClockRounding.roundToStep(7, 3))
            } finally {
                Locale.setDefault(previous)
            }
        }

        @Test fun changeIsNullWhenUntouched() {
            assertNull(WizardClockRounding.change("07:12", 7, 12, touched = false))
        }

        @Test fun changeIsNullWhenRoundingToTheStoredValue() {
            assertNull(WizardClockRounding.change("07:15", 7, 13, touched = true))
        }

        @Test fun changeReturnsTheRoundedValue() {
            assertEquals("07:15", WizardClockRounding.change("07:12", 7, 13, touched = true))
        }
    }
}
