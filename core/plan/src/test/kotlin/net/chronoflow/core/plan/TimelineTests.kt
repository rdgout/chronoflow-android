package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlinx.serialization.json.jsonObject
import net.chronoflow.core.model.api.ApiJson
import net.chronoflow.core.model.contracts.BlockReasonTagsView
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.model.contracts.DayZoneView
import net.chronoflow.core.model.contracts.Lane
import net.chronoflow.core.model.contracts.TickView
import net.chronoflow.core.model.contracts.TimelineDayPresentationView
import net.chronoflow.core.model.contracts.TimelinePresentationView
import net.chronoflow.core.model.contracts.TransferWarningsView
import net.chronoflow.core.model.contracts.isSupported
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.testing.Fixtures
import org.junit.Test
import kotlin.math.abs

class TimelineTests {
    @Test fun dimensionsPackingAndTinyTiles() {
        assertThat(PlanMetrics.pointsPerHour).isEqualTo(40.0)
        assertThat(PlanMetrics.minTileHeight).isEqualTo(24.0)
        assertThat(PlanMetrics.tinyTileHeight).isEqualTo(56.0)
        assertThat(PlanMetrics.gutterWidth).isEqualTo(44.0)
        assertThat(PlanMetrics.laneGap).isEqualTo(4.0)
        assertThat(PlanMetrics.tileInset).isEqualTo(2.0)
        assertThat(timelineLanes).isEqualTo(listOf(Lane.Sleep, Lane.Light, Lane.Caffeine, Lane.Travel))
        val day = sampleDay()
        val items =
            listOf(
                block("a", 0.0, 150.0),
                block("b", 15.0, 90.0),
                block("c", 90.0, 210.0),
                block("d", 225.0, 300.0),
                block("e", 225.0, 300.0),
                block("f", 300.0, 360.0),
            )
        val result = layoutDay(day, items, emptyIndex()).placed.getValue(Lane.Sleep)
        assertThat(result.map { it.col }).isEqualTo(listOf(0, 1, 1, 0, 1, 0))
        assertThat(result.map { it.cols }).isEqualTo(listOf(2, 2, 2, 2, 2, 1))
        assertThat(result[2].top).isEqualTo(60.0)
        assertThat(result[2].height).isEqualTo(80.0)
        assertThat(result[2].tiny).isEqualTo(false)
        val short =
            layoutDay(
                day,
                listOf(
                    block("before", -60.0, 20.0),
                    block("short", 25.0, 26.0),
                    block("end", 1439.0, 1441.0),
                    block("instant", 1439.0, 1439.0),
                ),
                emptyIndex(),
            )
        val tiles = short.placed.getValue(Lane.Sleep)
        assertThat(tiles[0].continuesBefore).isEqualTo(true)
        assertThat(tiles[0].height).isEqualTo(24.0)
        assertThat(tiles[0].cols).isEqualTo(2)
        assertThat(tiles[0].tiny).isEqualTo(true)
        assertThat(tiles[1].col).isEqualTo(1)
        assertThat(tiles[2].continuesAfter).isEqualTo(true)
        assertThat(tiles[2].top + tiles[2].height).isEqualTo(short.height)
        assertThat(tiles[2].exactTop > tiles[2].top).isEqualTo(true)
        assertThat(tiles[3].col).isEqualTo(1)
        for ((duration, tiny) in listOf(0.0 to true, 84.0 to true, 84.01 to false)) {
            val tile = layoutDay(day, listOf(block("tile", 0.0, duration)), emptyIndex()).placed.getValue(Lane.Sleep)[0]
            assertThat(tile.tiny).isEqualTo(tiny)
        }
        assertThat(layoutDay(day, emptyList(), emptyIndex()).minWidth).isEqualTo(236.0)
        val stacked = listOf(block("a", 0.0, 100.0), block("b", 10.0, 60.0), block("c", 20.0, 50.0))
        assertThat(layoutDay(day, stacked, emptyIndex()).minWidth).isEqualTo(282.0)
    }

    @Test fun boundariesShortDaysAndLongDays() {
        val day = sampleDay()
        val items =
            listOf(
                block("ends", -10.0, 0.0),
                block("before", -10.0, -10.0),
                block("start", 0.0, 0.0),
                block("end", 1440.0, 1440.0),
                block("after", 1440.0, 1450.0),
            )
        assertThat(layoutDay(day, items, emptyIndex()).placed.getValue(Lane.Sleep).map { it.block.id })
            .isEqualTo(listOf("start"))
        val short = layoutDay(sampleDay(1.0 / 6), listOf(block("a", -1.0, 2.0), block("b", 9.0, 11.0)), emptyIndex())
        assertThat(abs(short.height - 20.0 / 3) < 1e-8).isEqualTo(true)
        assertThat(
            short.placed.getValue(Lane.Sleep).all { it.top == 0.0 && it.height == short.height && it.cols == 2 },
        ).isEqualTo(true)
        for (hours in listOf(23.0, 25.0, 31.0)) {
            val long = sampleDay(hours)
            assertThat(layoutDay(long, emptyList(), emptyIndex()).height).isEqualTo(hours * 40)
            assertThat(nowLineOffset(long, planDate(at(hours * 30))!!)).isEqualTo(hours * 20)
        }
        val expectations =
            listOf(-1.0 to null, 0.0 to 0.0, 60.0 to 40.0, 1439.0 to 1439 * 2.0 / 3, 1440.0 to null, 1441.0 to null)
        for ((minute, expected) in expectations) {
            val offset = nowLineOffset(day, planDate(at(minute))!!)
            if (expected ==
                null
            ) {
                assertThat(offset).isNull()
            } else {
                assertThat(abs(offset!! - expected) < 1e-8).isEqualTo(true)
            }
        }
        val millisecond = nowLineOffset(day, planDate(day.startUtc)!!.plusMillis(1))!!
        assertThat(abs(millisecond - PlanMetrics.pointsPerMinute / 60_000) < 1e-8).isEqualTo(true)
        assertThat(nowLineOffset(day.copy(startUtc = "garbage"), planDate(at(60.0))!!)).isNull()
    }

    @Test fun markersZonesAndTickThinning() {
        val day =
            sampleDay().copy(
                endUtc = at(270.0),
                zones =
                    listOf("Europe/Amsterdam", "Asia/Dubai", "Asia/Tokyo").mapIndexed { index, tz ->
                        DayZoneView(tz, at(index * 90.0), at((index + 1) * 90.0))
                    },
            )
        val changes =
            listOf(-2.0, 0.0, 90.0, 90.0, 270.0).mapIndexed { index, minute ->
                block("change-$index", minute, minute + 5).copy(type = BlockType.ZoneChange)
            }
        assertThat(zoneChangePositions(day, changes).map { it.block.id })
            .isEqualTo(listOf("change-1", "change-2", "change-3"))
        val shown = layoutDay(day, listOf(block("span", 30.0, 240.0)) + changes, emptyIndex())
        val hidden = layoutDay(day, changes, emptyIndex(), setOf(BlockType.ZoneChange))
        assertThat(shown.height).isEqualTo(180.0)
        assertThat(shown.placed.getValue(Lane.Sleep)[0].height).isEqualTo(140.0)
        assertThat(shown.dividers.map { it.top }).isEqualTo(listOf(60.0, 120.0))
        assertThat(hidden.changes).isEmpty()
        assertThat(hidden.dividers).isEqualTo(shown.dividers)
        val notes =
            listOf("later" to 80.0, "first" to 10.0, "equal" to 10.0, "close" to 11.0).map { (id, minute) ->
                block(id, minute, minute + 1).copy(type = BlockType.Note, lane = Lane.Info)
            }
        val markers = layoutDay(day, notes, emptyIndex()).markers
        assertThat(markers.map { it.block.id }).isEqualTo(listOf("first", "equal", "close", "later"))
        for (i in 1 until markers.size) {
            assertThat(markers[i].top >= markers[i - 1].top + PlanMetrics.gutterMarkerSize).isEqualTo(true)
        }
        assertThat(layoutDay(day, notes, emptyIndex(), setOf(BlockType.Note)).markers).isEmpty()

        fun tick(
            minute: Double,
            tz: String = "Europe/Amsterdam",
            offset: Int = 60,
        ) = TickView(at(minute), tz, offset)
        val ticks =
            listOf(
                tick(-1.0),
                tick(0.0),
                tick(60.0),
                tick(65.0, "Asia/Tokyo", 540),
                tick(70.0, "Asia/Tokyo", 540),
                tick(120.0, "Asia/Tokyo", 540),
                tick(270.0),
            )
        assertThat(tickPositions(day, ticks).map { it.visible }).isEqualTo(listOf(true, false, true, false, true))
        val close = listOf(tick(0.0), tick(5.0, "Asia/Tokyo", 540), tick(10.0, "Asia/Tokyo", 600))
        assertThat(tickPositions(day, close).all { it.visible }).isEqualTo(true)
        val fold = day.copy(startUtc = "2026-10-25T00:00Z", endUtc = "2026-10-26T00:00Z")
        val folded =
            tickPositions(
                fold,
                listOf(
                    TickView(fold.startUtc, "Europe/Amsterdam", 120),
                    TickView("2026-10-25T01:00Z", "Europe/Amsterdam", 60),
                ),
            )
        assertThat(folded.map { it.top }).isEqualTo(listOf(0.0, 40.0))
    }

    @Test fun unparseableInstantsNeverCrash() {
        val day = sampleDay()
        val broken = day.copy(id = "broken", endUtc = "garbage")
        val layout = layoutDay(broken, listOf(block("a", 0.0, 60.0)), emptyIndex())
        assertThat(layout.height).isEqualTo(0.0)
        assertThat(layout.minWidth).isEqualTo(236.0)
        assertThat(layout.placed.values.all { it.isEmpty() }).isEqualTo(true)
        val items = listOf(block("ok", 0.0, 60.0), block("bad", 0.0, 60.0).copy(startUtc = "garbage"))
        assertThat(layoutDay(day, items, emptyIndex()).placed.getValue(Lane.Sleep).map { it.block.id })
            .isEqualTo(listOf("ok"))
        val ticks = listOf(TickView("garbage", "UTC", 0), TickView(at(10.0), "UTC", 0))
        assertThat(tickPositions(day, ticks).size).isEqualTo(1)
        val plan = fixture().plan.copy(days = listOf(day, broken), blocks = items)
        val bucketed = timelineBlocksByDay(plan)
        assertThat(bucketed.getValue(broken.id)).isEmpty()
        assertThat(bucketed.getValue(day.id).map { it.id }).isEqualTo(listOf("ok"))
    }

    @Test fun emittedLanesPresentationAndEmptyPlan() {
        val day = sampleDay()
        var item = block("transfer", 0.0, 60.0)
        val reasons = fixture().timeline.blockReasons[0].lines
        val tick = TickView(day.startUtc, "Europe/Amsterdam", 60)
        val presentation =
            indexTimeline(
                TimelinePresentationView(
                    days = listOf(TimelineDayPresentationView(day.id, listOf(tick))),
                    blockReasons = listOf(BlockReasonTagsView(item.id, reasons)),
                    transferWarnings = listOf(TransferWarningsView(item.id, listOf(1, 0))),
                ),
            )
        val layout = layoutDay(day, listOf(item), presentation)
        assertThat(
            layout.placed.getValue(Lane.Sleep)[0].presentation,
        ).isEqualTo(BlockPresentation(reasons, listOf(1, 0)))
        assertThat(layout.ticks[0].tick).isEqualTo(tick)
        assertThat(layout.ticks[0].top).isEqualTo(0.0)
        val cases =
            listOf(
                BlockType.MovementBreak to Lane.Travel,
                BlockType.Rest to Lane.Sleep,
                BlockType.CaffeineRoutine to Lane.Caffeine,
                BlockType.Note to Lane.Info,
            )
        for ((type, lane) in cases) {
            item = item.copy(type = type, lane = lane)
            val result = layoutDay(day, listOf(item), emptyIndex())
            if (lane == Lane.Info) {
                assertThat(result.markers[0].block).isEqualTo(item)
            } else {
                assertThat(result.placed.getValue(lane)[0].block).isEqualTo(item)
            }
        }
        val plan = fixture().plan.copy(days = emptyList(), blocks = emptyList())
        val empty =
            TimelinePresentationView(days = emptyList(), blockReasons = emptyList(), transferWarnings = emptyList())
        assertThat(layoutPlan(plan, empty)).isEmpty()
        val original = fixture().plan
        val intervals = summaryIntervals(original.days[2], original.blocks, BlockType.SeekLight)
        assertThat(intervals).isEqualTo(
            listOf(
                DisplayInterval(
                    planDate("2026-10-15T06:05:00Z")!!,
                    planDate("2026-10-15T07:35:00Z")!!,
                    "Asia/Tokyo",
                ),
            ),
        )
    }

    @Test fun everyFixturePreservesLayoutAndResolvesCopy() {
        val failures = mutableListOf<String>()
        var lines = 0
        for (name in FIXTURE_NAMES) {
            val decodedTwice =
                ApiJson.response.decodeFromString(
                    PlanFixture.serializer(),
                    Fixtures.text("plans/$name.json"),
                )
            val original = fixture(name).plan
            if (decodedTwice.plan != original) failures += "$name: decode is not repeatable"
            val once = withoutOptionalSteps(original)
            if (withoutOptionalSteps(once) != once) failures += "$name: withoutOptionalSteps is not idempotent"
            if (original != fixture(name).plan) failures += "$name: plan mutated"
            val root = ApiJson.response.parseToJsonElement(Fixtures.text("plans/$name.json")).jsonObject
            lines += unresolvedCopyLines(root.getValue("plan"), name, failures)
            checkLayouts(name, failures)
        }
        assertThat(failures).isEqualTo(emptyList<String>())
        assertThat(lines).isEqualTo(1_630)
    }

    private fun checkLayouts(
        name: String,
        failures: MutableList<String>,
    ) {
        val f = fixture(name)
        val types =
            f.plan.blocks
                .map { it.type }
                .toSet()
                .toList()
        val allHiddenLayouts = layoutPlan(f.plan, f.timeline, types)
        val reasons = f.timeline.blockReasons.associate { it.blockId to it.lines }
        val tickIndex = f.timeline.days.associate { it.dayId to it.ticks }
        for (hidden in listOf(emptyList(), listOf(BlockType.Sleep, BlockType.Note, BlockType.ZoneChange), types)) {
            val layouts = layoutPlan(f.plan, f.timeline, hidden)
            if (layouts.size != f.plan.days.size) failures += "$name: layout count"
            for (day in f.plan.days) {
                val layout = layouts.getValue(day.id)
                val dayStart = planDate(day.startUtc)!!
                val dayEnd = planDate(day.endUtc)!!
                if (layout.height != (dayEnd.epochSecond - dayStart.epochSecond) / 3600.0 * 40) {
                    failures += "$name ${day.id}: height"
                }
                for (lane in timelineLanes) {
                    val tiles = layout.placed.getValue(lane)
                    val expected =
                        f.plan.blocks
                            .filter {
                                val start = planDate(it.startUtc)!!
                                val end = planDate(it.endUtc)!!
                                it.isSupported && !isOptionalStep(it) && it.type !in hidden &&
                                    it.type != BlockType.ZoneChange && it.lane == lane && start < dayEnd &&
                                    (end > dayStart || (it.startUtc == it.endUtc && start >= dayStart))
                            }.map { it.id }
                    if (tiles.map { it.block.id }.sorted() !=
                        expected.sorted()
                    ) {
                        failures += "$name ${day.id} $lane: tiles"
                    }
                    for (tile in tiles) {
                        if (!(tile.top >= 0 && tile.height > 0 && tile.top + tile.height <= layout.height + 1e-6)) {
                            failures += "$name ${tile.block.id}: bounds"
                        }
                        if (!(tile.col >= 0 && tile.col < tile.cols)) failures += "$name ${tile.block.id}: column"
                        if (tile.continuesBefore !=
                            (planDate(tile.block.startUtc)!! < dayStart)
                        ) {
                            failures += "$name: before"
                        }
                        if (tile.continuesAfter != (planDate(tile.block.endUtc)!! > dayEnd)) failures += "$name: after"
                        if (tile.presentation.reasons != reasons[tile.block.id].orEmpty()) failures += "$name: reasons"
                    }
                    for ((i, a) in tiles.withIndex()) {
                        for (b in tiles.drop(i + 1)) {
                            if (a.top + a.height > b.top && !(a.cols == b.cols && a.col != b.col)) {
                                failures += "$name ${a.block.id}/${b.block.id}: overlap"
                            }
                        }
                    }
                }
                if (layout.ticks !=
                    tickPositions(day, tickIndex[day.id].orEmpty())
                ) {
                    failures += "$name ${day.id}: ticks"
                }
                val allHidden = allHiddenLayouts.getValue(day.id)
                if (!allHidden.placed.values.all { it.isEmpty() } || allHidden.markers.isNotEmpty() ||
                    allHidden.changes.isNotEmpty()
                ) {
                    failures += "$name ${day.id}: all-hidden layout is not empty"
                }
                if (allHidden.dividers != layout.dividers ||
                    allHidden.ticks != layout.ticks
                ) {
                    failures += "$name: dividers"
                }
            }
        }
    }

    private companion object {
        val FIXTURE_NAMES = listOf("S1", "S2", "S3", "S4", "S5", "S6", "S6-RETURN-10N")
    }
}
