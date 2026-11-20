package net.chronoflow.core.wizard.model

import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale

@RunWith(Parameterized::class)
class WizardDateFormatTest(
    private val processLanguage: String,
) {
    @Test fun formatsUseFixedTablesAndAsciiDigits() {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag(processLanguage))
            assertEquals("12 Oct 2026", WizardDateFormat.datePill("2026-10-12", Language.En))
            assertEquals("12 okt 2026", WizardDateFormat.datePill("2026-10-12", Language.Nl))
            assertEquals("2 sep 2026", WizardDateFormat.datePill("2026-09-02", Language.Nl))
            assertEquals("Monday 12 October 2026", WizardDateFormat.dateSpoken("2026-10-12", Language.En))
            assertEquals("maandag 12 oktober 2026", WizardDateFormat.dateSpoken("2026-10-12", Language.Nl))
            assertEquals("Sunday 11 October 2026", WizardDateFormat.dateSpoken("2026-10-11", Language.En))
            assertEquals("woensdag 2 september 2026", WizardDateFormat.dateSpoken("2026-09-02", Language.Nl))
            assertEquals("14:40", WizardDateFormat.time("14:40", TimeFormat.Value24, Language.En))
            assertEquals("00:05", WizardDateFormat.time("00:05", TimeFormat.Unknown("future"), Language.Nl))
            assertEquals("2:40 PM", WizardDateFormat.time("14:40", TimeFormat.Value12, Language.En))
            assertEquals("12:05 AM", WizardDateFormat.time("00:05", TimeFormat.Value12, Language.En))
            assertEquals("12:00 PM", WizardDateFormat.time("12:00", TimeFormat.Value12, Language.En))
            assertEquals("02:40 p.m.", WizardDateFormat.time("14:40", TimeFormat.Value12, Language.Nl))
            assertEquals("12:05 a.m.", WizardDateFormat.time("00:05", TimeFormat.Value12, Language.Nl))
            assertEquals("12 Oct 2026", WizardDateFormat.datePill("2026-10-12", Language.Unknown("future")))
        } finally {
            Locale.setDefault(old)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun cases() = listOf("en-US", "ar-EG")
    }
}
