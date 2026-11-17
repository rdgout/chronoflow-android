package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.Language
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class DateTextTest {
    private val utc = ZoneOffset.UTC.rules

    private fun all(
        iso: String,
        language: Language,
    ) = DateTemplate.entries.map { dateText(Instant.parse(iso), language, utc, it) }

    @Test fun september() {
        assertThat(all("2026-09-15T12:00:00Z", Language.En))
            .isEqualTo(listOf("15 Sep 2026", "15 Sep", "Tue 15 Sep", "Tue 15", "Tue", "Sep 2026"))
        assertThat(all("2026-09-15T12:00:00Z", Language.Nl))
            .isEqualTo(listOf("15 sep 2026", "15 sep", "di 15 sep", "di 15", "di", "sep 2026"))
    }

    @Test fun may() {
        assertThat(all("2026-05-03T12:00:00Z", Language.En))
            .isEqualTo(listOf("3 May 2026", "3 May", "Sun 3 May", "Sun 3", "Sun", "May 2026"))
        assertThat(all("2026-05-03T12:00:00Z", Language.Nl))
            .isEqualTo(listOf("3 mei 2026", "3 mei", "zo 3 mei", "zo 3", "zo", "mei 2026"))
    }

    @Test fun preEpochInstantInTokyoIsTheNextDay() {
        val tokyo = planZoneRules.rules("Asia/Tokyo")
        assertThat(dateText(Instant.parse("1969-12-31T23:30:00Z"), Language.En, tokyo, DateTemplate.D_MMM_YYYY))
            .isEqualTo("1 Jan 1970")
    }

    @Test fun nullInstantIsEmptyAndUnknownLanguageUsesEnglish() {
        assertThat(dateText(null, Language.En, utc, DateTemplate.EEE)).isEqualTo("")
        assertThat(dateText(Instant.parse("2026-09-15T12:00:00Z"), Language.Unknown("fr"), utc, DateTemplate.EEE))
            .isEqualTo("Tue")
    }

    @Test fun unknownZoneIsUtc() {
        assertThat(planZoneRules.rules("Not/AZone")).isEqualTo(utc)
        assertThat(planZoneRules.rules(null)).isEqualTo(utc)
    }
}
