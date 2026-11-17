package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class LocaleIndependenceTest {
    private val f = CopyLineFormatter(testCatalog)
    private lateinit var locale: Locale
    private lateinit var zone: TimeZone

    @Before fun setDefaults() {
        locale = Locale.getDefault()
        zone = TimeZone.getDefault()
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
        TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"))
    }

    @After fun restoreDefaults() {
        Locale.setDefault(locale)
        TimeZone.setDefault(zone)
    }

    private fun snapshot() =
        listOf(
            f.formatInstant("2026-03-29T00:59:00Z", CopyFormat(Language.En), "Europe/Amsterdam"),
            f.formatInstant("2026-03-29T01:00:00Z", CopyFormat(Language.Nl), "Europe/Amsterdam"),
            f.formatDate("2026-09-15", Language.Nl, "Asia/Tokyo"),
            f.formatMinutes(135.0, Language.En, duration = true),
            f.formatOffset(-570.0, Language.Nl),
            f.formatNumber(1500.0, Language.Nl),
            f.formatInstant("2026-01-15T15:00Z", CopyFormat(Language.Nl, TimeFormat.Value12)),
            f.dayDateLabel(sampleDay().copy(labelDate = "2026-10-31", labelRange = "2026-11-01"), Language.En),
        )

    @Test fun defaultLocaleAndZoneDoNotChangeOutput() {
        val hostile = snapshot()
        Locale.setDefault(Locale.ROOT)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        assertThat(snapshot()).isEqualTo(hostile)
        assertThat(hostile[5]).isEqualTo("1.500")
        assertThat(hostile[6]).isEqualTo("03:00 p.m.")
        assertThat(hostile[7]).isEqualTo("Sat 31 Oct – Sun 1 Nov")
    }
}
