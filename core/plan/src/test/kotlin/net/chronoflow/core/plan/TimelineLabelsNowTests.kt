package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.DayZoneView
import net.chronoflow.core.model.contracts.Language
import org.junit.Test

/** (new) `nowLabel` zone selection on a 25 h DST day. */
class TimelineLabelsNowTests {
    private val formatter = CopyLineFormatter(testCatalog)
    private val options = CopyFormat(Language.En)

    private fun layout(zones: List<DayZoneView>): DayLayout {
        val day = sampleDay(25.0).copy(zones = zones)
        return layoutDay(day, emptyList(), emptyIndex())
    }

    private val twoZones =
        listOf(
            DayZoneView("Europe/Amsterdam", at(0.0), at(600.0)),
            DayZoneView("Asia/Tokyo", at(600.0), at(1500.0)),
        )

    @Test fun nowInsideTheSecondZoneUsesThatZone() {
        // 2026-01-01T12:00Z is 21:00 in Tokyo, 13:00 in Amsterdam.
        assertThat(formatter.nowLabel(layout(twoZones), planDate(at(720.0))!!, options)).isEqualTo("21:00")
    }

    @Test fun nowAfterTheLastZoneUsesTheLastZone() {
        val shortened =
            listOf(DayZoneView("Europe/Amsterdam", at(0.0), at(60.0)), DayZoneView("Asia/Tokyo", at(60.0), at(120.0)))
        assertThat(formatter.nowLabel(layout(shortened), planDate(at(720.0))!!, options)).isEqualTo("21:00")
    }

    @Test fun aDayWithoutZonesFormatsInUtc() {
        assertThat(formatter.nowLabel(layout(emptyList()), planDate(at(720.0))!!, options)).isEqualTo("12:00")
    }
}
