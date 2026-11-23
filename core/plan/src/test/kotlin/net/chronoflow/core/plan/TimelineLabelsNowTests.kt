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

    private val start = "2026-10-24T22:00:00.000Z"
    private val changeOver = "2026-10-25T10:00:00.000Z"
    private val end = "2026-10-25T23:00:00.000Z"

    private fun layout(zones: List<DayZoneView>): DayLayout {
        val day = sampleDay(25.0).copy(startUtc = start, endUtc = end, zones = zones)
        return layoutDay(day, emptyList(), emptyIndex())
    }

    // Amsterdam falls back from CEST to CET at 2026-10-25T01:00Z, so this 25 h day has one clock hour twice.
    private val twoZones =
        listOf(
            DayZoneView("Europe/Amsterdam", start, changeOver),
            DayZoneView("Asia/Tokyo", changeOver, end),
        )

    @Test fun nowBeforeAndAfterTheFallBackReadsTheSameAmsterdamClock() {
        assertThat(
            formatter.nowLabel(layout(twoZones), planDate("2026-10-25T00:30:00.000Z")!!, options),
        ).isEqualTo("02:30")
        assertThat(
            formatter.nowLabel(layout(twoZones), planDate("2026-10-25T01:30:00.000Z")!!, options),
        ).isEqualTo("02:30")
    }

    @Test fun nowInsideTheSecondZoneUsesThatZone() {
        assertThat(
            formatter.nowLabel(layout(twoZones), planDate("2026-10-25T12:00:00.000Z")!!, options),
        ).isEqualTo("21:00")
    }

    @Test fun nowAfterTheLastZoneUsesTheLastZone() {
        val shortened =
            listOf(
                DayZoneView("Europe/Amsterdam", start, "2026-10-25T01:00:00.000Z"),
                DayZoneView("Asia/Tokyo", "2026-10-25T01:00:00.000Z", "2026-10-25T02:00:00.000Z"),
            )
        assertThat(
            formatter.nowLabel(layout(shortened), planDate("2026-10-25T12:00:00.000Z")!!, options),
        ).isEqualTo("21:00")
    }

    @Test fun aDayWithoutZonesFormatsInUtc() {
        assertThat(
            formatter.nowLabel(layout(emptyList()), planDate("2026-10-25T12:00:00.000Z")!!, options),
        ).isEqualTo("12:00")
    }
}
