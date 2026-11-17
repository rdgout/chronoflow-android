package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.BlockParams
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.DayView
import net.chronoflow.core.model.contracts.Lane
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.core.testing.MockTripTemplates
import org.junit.Test

class LargeTimelineFixtureTests {
    private val counts = listOf(30, 60)

    private fun detail(count: Int) = LargeTimelineFixtures.fixture("large$count").detail

    @Test fun generatedTripsStayDenseAndResolveExistingCopy() {
        val problems = mutableListOf<String>()
        for (count in counts) {
            val detail = detail(count)
            val plan = detail.plan
            if (plan.days.size != count) problems += "$count: days"
            if (plan.days
                    .map { it.id }
                    .toSet()
                    .size != count
            ) {
                problems += "$count: day ids"
            }
            if (plan.blocks
                    .map { it.id }
                    .toSet()
                    .size != plan.blocks.size
            ) {
                problems += "$count: block ids"
            }
            if (plan.days
                    .flatMap { it.zones }
                    .map { it.tz }
                    .toSet()
                    .size < 3
            ) {
                problems += "$count: zones"
            }
            if (plan.blocks.count { it.type == BlockType.ZoneChange } < count / 6) problems += "$count: zone changes"
            for (lane in listOf(Lane.Sleep, Lane.Light, Lane.Caffeine)) {
                if (plan.blocks.count { it.lane == lane } < count) problems += "$count: ${lane.raw} blocks"
            }
            if (plan.blocks.none { (it.params as? BlockParams.Flight)?.value?.overnight == true }) {
                problems +=
                    "$count: overnight"
            }
            for ((a, b) in plan.days.zipWithNext()) if (a.endUtc != b.startUtc) problems += "$count: ${a.id} gap"
            for (block in plan.blocks) {
                if (planDate(block.startUtc)!! > planDate(block.endUtc)!!) problems += "$count: ${block.id} reversed"
                if (!testCatalog.contains(block.text.key)) problems += "$count: ${block.text.key}"
                block.secondaryText?.let { if (!testCatalog.contains(it.key)) problems += "$count: ${it.key}" }
            }
            for (language in listOf(Language.En, Language.Nl)) {
                val prepared = prepare(detail, language)
                if (prepared.days.size != count) problems += "$count ${language.raw}: prepared days"
                if (!prepared.days.all { it.blocks.isNotEmpty() }) problems += "$count ${language.raw}: empty day"
            }
        }
        assertThat(problems).isEqualTo(emptyList<String>())
    }

    @Test fun sixtyDayGridHeightMatchesTheRepeatedUTCSpans() {
        val detail = detail(60)
        val layouts = layoutPlan(detail.plan, detail.timeline)
        val cycleHours = listOf(24.0, 24.0, 13.5, 25.5, 24.0, 24.0)
        val heights = detail.plan.days.map { layouts.getValue(it.id).height }
        assertThat(heights).isEqualTo((0 until 10).flatMap { cycleHours.map { it * PlanMetrics.pointsPerHour } })
        assertThat(heights.sum()).isEqualTo(54_000.0)
        val prepared = prepare(detail, Language.En, layouts = layouts)
        val last = prepared.days.last()
        assertThat(last.title + " · " + last.date).isEqualTo("Day 2 · Tue 8 Dec")
        println(
            "60-day layout: grids=${heights.sum()}pt; gaps=${62 * PlanMetrics.sectionGap}pt; " +
                "warning headers=${prepared.days.count { it.warningCount > 0 }}; " +
                "list rows=${prepared.days.sumOf { it.blocks.size }}; " +
                "zone rows=${prepared.days.flatMap { it.blocks }.count { it.block.type == BlockType.ZoneChange }}; " +
                "widths=${layouts.values.map { it.minWidth }.toSortedSet()}",
        )
    }

    private class Golden(
        val count: Int,
        val hash: String,
        val days: Int,
        val blocks: Int,
        val perLane: List<Int>,
        val zoneChanges: Int,
        val overnight: Int,
        val lastDay: String,
        val lastDayRange: Pair<String, String>,
        val lastLabelDate: String,
        val tripEnd: String,
        val lastBlock: String,
        val rows: Int,
        val zoneRows: Int,
        val lastLabel: String,
    )

    private val goldens =
        listOf(
            Golden(
                30,
                "1cc3eb625fba1af74dcdd6beefc9e777716d9e0a541323caa1ca498c75b8c0af",
                30,
                490,
                listOf(35, 210, 210, 35),
                10,
                5,
                "day-29871120-tour-4",
                "2026-11-09T08:00:00Z" to "2026-11-10T08:00:00Z",
                "2026-11-10",
                "2026-11-10T08:00:00Z",
                "caffeine-no_caffeine-29867390-caffeine-day-29",
                370,
                10,
                "Day 2 · Tue 10 Nov",
            ),
            Golden(
                60,
                "0de45f266582126384ac3cd1cca4e776b9e569acbbe92bdc1580565ceae374c5",
                60,
                980,
                listOf(70, 420, 420, 70),
                20,
                10,
                "day-29871120-tour-9",
                "2026-12-07T11:00:00Z" to "2026-12-08T11:00:00Z",
                "2026-12-08",
                "2026-12-08T11:00:00Z",
                "caffeine-no_caffeine-29867390-caffeine-day-59",
                740,
                20,
                "Day 2 · Tue 8 Dec",
            ),
        )

    @Test fun idsHashesAndLabelsMatchIos() {
        val problems = mutableListOf<String>()
        for (g in goldens) {
            val detail = detail(g.count)
            val plan = detail.plan
            val id = "0199a3c0-0000-7000-8000-0000000000${g.count}"

            fun check(
                label: String,
                actual: Any?,
                expected: Any?,
            ) {
                if (actual != expected) problems += "${g.count} $label: $actual != $expected"
            }
            check("trip id", detail.trip.id, id)
            check("hash", detail.plan.meta.inputHash, g.hash)
            check("planInputHash", detail.checkState.planInputHash, g.hash)
            check("detailVersion", detail.detailVersion, g.hash)
            check("days", plan.days.size, g.days)
            check("blocks", plan.blocks.size, g.blocks)
            check(
                "lanes",
                listOf(Lane.Sleep, Lane.Light, Lane.Caffeine, Lane.Travel).map { l ->
                    plan.blocks.count {
                        it.lane ==
                            l
                    }
                },
                g.perLane,
            )
            check("zone changes", plan.blocks.count { it.type == BlockType.ZoneChange }, g.zoneChanges)
            check(
                "overnight",
                plan.blocks.count { (it.params as? BlockParams.Flight)?.value?.overnight == true },
                g.overnight,
            )
            val first: DayView = plan.days.first()
            check(
                "first day",
                listOf(first.id, first.startUtc, first.endUtc, first.labelDate),
                listOf("day-29864460-tour-0", "2026-10-13T05:00:00Z", "2026-10-14T05:00:00Z", "2026-10-13"),
            )
            check("first phase", first.phase.raw, "pre")
            val last = plan.days.last()
            check(
                "last day",
                listOf(last.id, last.startUtc, last.endUtc, last.labelDate, last.postArrivalIndex),
                listOf(g.lastDay, g.lastDayRange.first, g.lastDayRange.second, g.lastLabelDate, 2),
            )
            check("last phase", last.phase.raw, "post")
            check("trip end", detail.trip.endUtc, g.tripEnd)
            check("last block", plan.blocks.last().id, g.lastBlock)
            check(
                "first blocks",
                plan.blocks.take(2).map {
                    it.id
                },
                listOf("sleep-sleep-29865420-tour-0", "light-avoid_light-29865900-tour-0"),
            )
            check(
                "zones",
                plan.days
                    .flatMap {
                        it.zones
                    }.map { it.tz }
                    .toSortedSet()
                    .toList(),
                listOf("Asia/Singapore", "Australia/Sydney", "Europe/Amsterdam"),
            )
            check("assumed return", plan.meta.assumedReturn, null)
            check("marks", detail.checkState.marks, emptyList<Any>())
            val layouts = layoutPlan(plan, detail.timeline)
            val cycle = listOf(960.0, 960.0, 540.0, 1020.0, 960.0, 960.0)
            check("heights", plan.days.map { layouts.getValue(it.id).height }, (0 until g.count / 6).flatMap { cycle })
            check(
                "min widths",
                layouts.values
                    .map { it.minWidth }
                    .toSortedSet()
                    .toList(),
                listOf(252.0, 268.0),
            )
            val prepared = prepare(detail, Language.En, layouts = layouts)
            check("rows", prepared.days.sumOf { it.blocks.size }, g.rows)
            check(
                "zone rows",
                prepared.days.flatMap { it.blocks }.count { it.block.type == BlockType.ZoneChange },
                g.zoneRows,
            )
            check("last label", prepared.days.last().title + " · " + prepared.days.last().date, g.lastLabel)
        }
        val caffeineSource = MockTripTemplates.templates()[0].detail.plan
        val sourceDay = caffeineSource.days.first { it.id == "day-29866935" }
        val perDay =
            caffeineSource.blocks.count {
                it.lane == Lane.Caffeine && planDate(it.startUtc)!! >= planDate(sourceDay.startUtc)!! &&
                    planDate(it.startUtc)!! < planDate(sourceDay.endUtc)!!
            }
        if (perDay != 7) problems += "caffeine blocks per day: $perDay"
        val large60 = detail(60)
        val lastTour = large60.plan.blocks.last()
        if (lastTour.startUtc != "2026-12-07T18:35:00Z" || lastTour.endUtc != "2026-12-08T03:15:00Z") {
            problems += "last block instants ${lastTour.startUtc} ${lastTour.endUtc}"
        }
        assertThat(problems).isEqualTo(emptyList<String>())
    }

    @Test fun templatesAreUnchanged() {
        val before = MockTripTemplates.templates()[2].detail
        detail(60)
        assertThat(MockTripTemplates.templates()[2].detail).isEqualTo(before)
        assertThat(
            MockTripTemplates
                .templates()[2]
                .detail.plan.days.size,
        ).isEqualTo(18)
    }
}
