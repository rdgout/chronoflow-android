package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.AirportRef
import net.chronoflow.core.model.contracts.CopyArg
import net.chronoflow.core.model.contracts.CopyArgKind
import net.chronoflow.core.model.contracts.CopyArgValue
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.contracts.TripDetailLegView
import net.chronoflow.core.model.contracts.TripDetailView
import org.junit.Test

class LabelTests {
    private val f = CopyLineFormatter(testCatalog)

    @Test fun datesZonesAndIntervals() {
        val day = sampleDay()
        val rows =
            listOf(
                listOf(Language.En, "2026-10-15", null, "Thu 15 Oct"),
                listOf(Language.Nl, "2026-10-15", null, "do 15 okt"),
                listOf(Language.En, "2026-10-15", "2026-10-15", "Thu 15 Oct"),
                listOf(Language.En, "2026-10-15", "2026-10-16", "Thu 15 – Fri 16 Oct"),
                listOf(Language.En, "2026-10-31", "2026-11-01", "Sat 31 Oct – Sun 1 Nov"),
                listOf(Language.Nl, "2026-10-31", "2026-11-01", "za 31 okt – zo 1 nov"),
            )
        for ((language, date, range, expected) in rows) {
            val labelled = day.copy(labelDate = date as String, labelRange = range as String?)
            assertThat(f.dayDateLabel(labelled, language as Language)).isEqualTo(expected)
        }
        assertThat(f.formatDayMonth("2026-03-29", Language.En)).isEqualTo("29 Mar")
        assertThat(f.formatMonthYear("2026-03-29", Language.Nl)).isEqualTo("mrt 2026")
        assertThat(localDate("2026-10-14T23:45Z", "Asia/Tokyo")).isEqualTo("2026-10-15")
        assertThat(localDate("2026-10-14T23:45Z", "America/New_York")).isEqualTo("2026-10-14")
        val tokyo =
            block(
                "sleep",
                0.0,
                60.0,
            ).copy(displayTz = "Asia/Tokyo", startUtc = "2026-10-14T14:30Z", endUtc = "2026-10-14T22:30Z")
        assertThat(f.blockInterval(tokyo, CopyFormat(Language.En))).isEqualTo("14 Oct 2026 23:30 – 15 Oct 2026 07:30")
        val amsterdam =
            tokyo.copy(displayTz = "Europe/Amsterdam", startUtc = "2026-10-25T00:30Z", endUtc = "2026-10-25T01:30Z")
        assertThat(
            f.blockInterval(amsterdam, CopyFormat(Language.En)),
        ).isEqualTo("02:30 (GMT+02:00) – 02:30 (GMT+01:00)")
        assertThat(utcOffset("2026-01-01T00:00Z", "America/New_York")).isEqualTo("GMT-05:00")
        val from = AirportRef("JFK", "New York", "America/New_York")
        val to = AirportRef("BOS", "Boston", "America/New_York")
        var trip = TripDetailView("trip", from, to, "", "", "", 0, emptyList())
        assertThat(zoneLabel(from.zone, trip)).isEqualTo("New York")
        trip = trip.copy(from = from.copy(city = null))
        assertThat(zoneLabel(from.zone, trip)).isEqualTo("Boston")
        trip =
            trip.copy(
                legs =
                    listOf(
                        TripDetailLegView(
                            0,
                            null,
                            AirportRef("NRT", "Narita", "Asia/Tokyo"),
                            AirportRef("HND", "Tokyo", "Asia/Tokyo"),
                            false,
                        ),
                    ),
            )
        assertThat(zoneLabel("Asia/Tokyo", trip)).isEqualTo("Narita")
        assertThat(zoneLabel("America/Argentina/Buenos_Aires")).isEqualTo("Buenos Aires")
        assertThat(zoneLabel("UTC")).isEqualTo("UTC")
        val arg = CopyArg("untilUtc", CopyArgKind.Instant, CopyArgValue.Text("2026-01-15T00:00Z"), "Asia/Tokyo")
        assertThat(
            f.formatArgument(
                arg,
                CopyFormat(Language.En, TimeFormat.Value12, "UTC", annotateInstantZone = true, trip = trip),
            ),
        ).isEqualTo("09:00 AM (Narita)")
        val table =
            listOf(
                Triple("00:05", "12:05 AM", "12:05 a.m."),
                Triple("03:00", "03:00 AM", "03:00 a.m."),
                Triple("11:59", "11:59 AM", "11:59 a.m."),
                Triple("12:00", "12:00 PM", "12:00 p.m."),
                Triple("15:00", "03:00 PM", "03:00 p.m."),
                Triple("23:59", "11:59 PM", "11:59 p.m."),
            )
        val mismatches = mutableListOf<String>()
        for (language in listOf(Language.En, Language.Nl)) {
            for ((value, en, nl) in table) {
                val actual = f.formatInstant("2026-01-15T${value}Z", CopyFormat(language, TimeFormat.Value12))
                if (actual != (if (language == Language.En) en else nl)) mismatches += "${language.raw} $value: $actual"
            }
        }
        assertThat(mismatches).isEqualTo(emptyList<String>())
    }
}
