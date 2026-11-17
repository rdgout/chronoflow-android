package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.AirportRef
import net.chronoflow.core.model.contracts.BlockParams
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.CopyArg
import net.chronoflow.core.model.contracts.CopyArgKind
import net.chronoflow.core.model.contracts.CopyArgValue
import net.chronoflow.core.model.contracts.CopyLine
import net.chronoflow.core.model.contracts.DayNoticesView
import net.chronoflow.core.model.contracts.DayStatus
import net.chronoflow.core.model.contracts.DayZoneView
import net.chronoflow.core.model.contracts.LabelPhase
import net.chronoflow.core.model.contracts.Lane
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.NoteRole
import net.chronoflow.core.model.contracts.NoteView
import net.chronoflow.core.model.contracts.PlanNoticesView
import net.chronoflow.core.model.contracts.Priority
import net.chronoflow.core.model.contracts.SleepKind
import net.chronoflow.core.model.contracts.SleepParamsView
import net.chronoflow.core.model.contracts.SleepSetting
import net.chronoflow.core.model.contracts.TimeFormat
import net.chronoflow.core.model.contracts.TripCardView
import net.chronoflow.core.model.contracts.TripDetailLegView
import net.chronoflow.core.model.contracts.TripDetailView
import net.chronoflow.core.model.contracts.TripProgress
import net.chronoflow.core.model.contracts.TripStatus
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

    @Test fun optionalRulesAndUnknowns() {
        val optional =
            mapOf(
                BlockType.AvoidLight to listOf(Priority.Optional),
                BlockType.SeekLight to listOf(Priority.Optional),
                BlockType.CaffeineOK to listOf(Priority.Core, Priority.Recommended, Priority.Optional, Priority.Info),
                BlockType.LightOK to listOf(Priority.Info),
                BlockType.LightNeutral to listOf(Priority.Info),
            )
        val types = BlockType.known
        assertThat(types.size).isEqualTo(17)
        val mismatches = mutableListOf<String>()
        for (type in types) {
            for (priority in listOf(Priority.Core, Priority.Recommended, Priority.Optional, Priority.Info)) {
                val item = block("test", 0.0, 60.0).copy(type = type, priority = priority)
                if (isOptionalStep(item) !=
                    (optional[type]?.contains(priority) ?: false)
                ) {
                    mismatches += "${type.raw}/${priority.raw}"
                }
            }
        }
        assertThat(mismatches).isEqualTo(emptyList<String>())
        assertThat(
            isOptionalStep(block("unknown", 0.0, 60.0).copy(type = BlockType.Unknown("future"))),
        ).isEqualTo(false)
        val unknownType = block("unknown", 0.0, 60.0).copy(type = BlockType.Unknown("future"))
        assertThat(layoutDay(sampleDay(), listOf(unknownType), emptyIndex()).placed.values.all { it.isEmpty() })
            .isEqualTo(true)
        val unknownLane = unknownType.copy(type = BlockType.Sleep, lane = Lane.Unknown("future"))
        assertThat(layoutDay(sampleDay(), listOf(unknownLane), emptyIndex()).placed.values.all { it.isEmpty() })
            .isEqualTo(true)
    }

    private fun sleep(
        start: String,
        end: String,
        kind: SleepKind = SleepKind.Main,
    ) = block(
        "main",
        0.0,
        60.0,
    ).copy(startUtc = start, endUtc = end, params = BlockParams.Sleep(SleepParamsView(kind, SleepSetting.Bed, 60)))

    @Test fun summaryClippingSplittingAndMerging() {
        val start = "2026-10-14T22:00Z"
        val end = "2026-10-15T22:00Z"
        val day =
            sampleDay().copy(
                startUtc = start,
                endUtc = end,
                zones =
                    listOf(
                        DayZoneView("Europe/Amsterdam", start, "2026-10-15T00:00Z"),
                        DayZoneView("Asia/Tokyo", "2026-10-15T00:00Z", "2026-10-15T12:00Z"),
                        DayZoneView("Europe/Amsterdam", "2026-10-15T12:00Z", end),
                    ),
            )
        val intervals = summaryIntervals(day, listOf(sleep("2026-10-14T20:00Z", "2026-10-16T01:00Z")), BlockType.Sleep)
        assertThat(intervals.map { it.tz }).isEqualTo(listOf("Europe/Amsterdam", "Asia/Tokyo", "Europe/Amsterdam"))
        assertThat(intervals.first().start).isEqualTo(planDate(start))
        assertThat(intervals.last().end).isEqualTo(planDate(end))
        val merged =
            summaryIntervals(
                day,
                listOf(
                    sleep("2026-10-15T01:00Z", "2026-10-15T04:00Z"),
                    sleep("2026-10-14T23:00Z", "2026-10-15T01:00Z"),
                    sleep("2026-10-15T03:00Z", "2026-10-15T05:00Z"),
                    sleep("2026-10-15T06:00Z", "2026-10-15T07:00Z"),
                ),
                BlockType.Sleep,
            )
        assertThat(merged.size).isEqualTo(3)
        assertThat(merged[1].start).isEqualTo(planDate("2026-10-15T00:00Z"))
        assertThat(merged[1].end).isEqualTo(planDate("2026-10-15T05:00Z"))
        assertThat(
            summaryIntervals(day, listOf(sleep(start, end, SleepKind.InFlightRecovery)), BlockType.Sleep),
        ).isEmpty()
        assertThat(
            summaryIntervals(
                day,
                listOf(sleep("2026-10-14T20:00Z", start), sleep(end, "2026-10-15T23:00Z")),
                BlockType.Sleep,
            ),
        ).isEmpty()
        assertThat(dayZoneLabel(day)).isEqualTo("Amsterdam \u2192 Tokyo")
    }

    @Test fun noticesStatusAndLabelCatalog() {
        val f = fixture()
        val missing = mutableListOf<String>()
        for (type in f.plan.blocks
            .map { it.type }
            .toSet()) {
            val key = stepLabelKey(type)
            if (key == null || !testCatalog.contains(key)) missing += "step ${type.raw}"
        }
        for (phase in LabelPhase.known) {
            val key = dayLabelKey(phase)
            if (key == null || !testCatalog.contains(key)) missing += "phase ${phase.raw}"
        }
        for (status in DayStatus.known) {
            val key = dayStatusLabelKey(status)
            if (key == null || !testCatalog.contains(key)) missing += "status ${status.raw}"
        }
        assertThat(missing).isEqualTo(emptyList<String>())
        val note = NoteView(NoteRole.Info, false, listOf(CopyLine("Sleep", emptyList())), null)
        val day = f.plan.days[0].copy(notes = listOf(note, note))
        val plan = f.plan.copy(warnings = listOf(note))
        val notices = PlanNoticesView(listOf(DayNoticesView(day.id, listOf(0, 0))), emptyList())
        assertThat(noticeItems(plan, notices, day)).isEqualTo(listOf(NoticeEntry(note, warning = true)))
        assertThat(noticeItems(plan, notices, day.copy(id = "other", notes = emptyList()))).isEmpty()
        val card =
            TripCardView(
                "trip",
                AirportRef("AMS", null, "Europe/Amsterdam"),
                AirportRef("NRT", null, "Asia/Tokyo"),
                "",
                "",
                at(0.0),
                at(60.0),
                null,
                emptyList(),
                0,
                TripProgress(TripStatus.Upcoming, null),
                canComplete = false,
                canReopen = false,
                canEdit = false,
                canDuplicate = false,
                canDelete = false,
            )
        val statuses =
            listOf(-1.0, 0.0, 59.0, 60.0).map {
                tripDisplayStatus(card, at(0.0), at(60.0), planDate(at(it))!!)
            }
        assertThat(
            statuses,
        ).isEqualTo(listOf(TripStatus.Upcoming, TripStatus.Active, TripStatus.Active, TripStatus.Completed))
        assertThat(tripDisplayStatus(card, null, at(60.0), planDate(at(30.0))!!)).isEqualTo(TripStatus.Upcoming)
        val completed = card.copy(progress = TripProgress(TripStatus.Completed, null))
        assertThat(
            tripDisplayStatus(completed, at(0.0), at(60.0), planDate(at(-1.0))!!),
        ).isEqualTo(TripStatus.Completed)
    }
}
