package net.chronoflow.core.wizard.model

import net.chronoflow.core.model.contracts.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

@RunWith(Enclosed::class)
class WizardDateTimeFormatTest {
    @RunWith(Parameterized::class)
    class Tables(
        private val value: String,
        private val language: Language,
        private val short: String,
        private val year: String,
    ) {
        @Test fun fixedCalendarTables() {
            val date = WizardDateTime.date(value)!!
            assertEquals(short, WizardDateTime.format(date, WizardDateTemplate.EEE_D_MMM, language))
            assertEquals(short.substringBefore(' '), WizardDateTime.format(date, WizardDateTemplate.EEE, language))
            assertEquals(year, WizardDateTime.format(date, WizardDateTemplate.EEE_D_MMM_Y, language))
        }

        companion object {
            @JvmStatic
            @Parameterized.Parameters(name = "{0}-{1}")
            fun cases() =
                listOf(
                    arrayOf("2026-03-02T00:00", Language.En, "Mon 2 Mar", "Mon, 2 Mar 2026"),
                    arrayOf("2026-03-02T00:00", Language.Nl, "ma 2 mrt", "ma 2 mrt 2026"),
                    arrayOf("2026-05-04T00:00", Language.En, "Mon 4 May", "Mon, 4 May 2026"),
                    arrayOf("2026-05-04T00:00", Language.Nl, "ma 4 mei", "ma 4 mei 2026"),
                    arrayOf("2026-09-07T00:00", Language.En, "Mon 7 Sep", "Mon, 7 Sep 2026"),
                    arrayOf("2026-09-07T00:00", Language.Nl, "ma 7 sep", "ma 7 sep 2026"),
                    arrayOf("2026-10-12T00:00", Language.En, "Mon 12 Oct", "Mon, 12 Oct 2026"),
                    arrayOf("2026-10-12T00:00", Language.Nl, "ma 12 okt", "ma 12 okt 2026"),
                )
        }
    }

    class Instants {
        @Test fun earlierOverlapAndInvalidZone() {
            assertEquals(
                Instant.parse("2026-10-25T00:30:00Z"),
                WizardDateTime.instant("2026-10-25T02:30", "Europe/Amsterdam"),
            )
            assertNull(WizardDateTime.instant("2026-10-25T02:30", "Not/AZone"))
        }

        @Test fun explicitInstantShapes() {
            assertEquals(Instant.parse("2026-11-07T22:00:00Z"), WizardDateTime.parseInstant("2026-11-07T22:00Z"))
            assertEquals(Instant.parse("2026-11-07T22:00:00Z"), WizardDateTime.parseInstant("2026-11-07T22:00:00Z"))
            assertEquals(
                Instant.parse("2026-11-07T21:00:00.123Z"),
                WizardDateTime.parseInstant("2026-11-07T22:00:00.123+01:00"),
            )
            assertNull(WizardDateTime.parseInstant("2026-11-07T22:00+01:00"))
            assertNull(WizardDateTime.parseInstant("2026-11-07"))
            assertNull(WizardDateTime.parseInstant(""))
        }

        @Test fun deviceDefaultsCannotChangeText() {
            val locale = Locale.getDefault()
            val zone = TimeZone.getDefault()
            try {
                Locale.setDefault(Locale.forLanguageTag("ar-EG"))
                TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
                val date = WizardDateTime.date("2026-10-12T00:00")!!
                assertEquals(
                    "Mon, 12 Oct 2026",
                    WizardDateTime.format(date, WizardDateTemplate.EEE_D_MMM_Y, Language.En),
                )
                assertEquals("ma 12 okt", WizardDateTime.format(date, WizardDateTemplate.EEE_D_MMM, Language.Nl))
                assertEquals(
                    "Mon 12 Oct",
                    WizardDateTime.format(date, WizardDateTemplate.EEE_D_MMM, Language.Unknown("ar")),
                )
            } finally {
                Locale.setDefault(locale)
                TimeZone.setDefault(zone)
            }
        }
    }
}
