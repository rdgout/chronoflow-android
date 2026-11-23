package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.BlockType
import org.junit.Test

class TimelineGeometryTests {
    @Test fun bucketedLayoutMatchesFullScan() {
        val mismatches = mutableListOf<String>()
        for (name in listOf("S1", "S2", "S3", "S4", "S5", "S6", "S6-RETURN-10N")) {
            val f = fixture(name)
            val plan = f.plan.copy(days = f.plan.days.reversed(), blocks = f.plan.blocks.reversed())
            val index = indexTimeline(f.timeline)
            for (hidden in listOf(emptyList(), listOf(BlockType.Sleep, BlockType.ZoneChange))) {
                val layouts = layoutPlan(plan, f.timeline, hidden)
                for (day in plan.days) {
                    if (layouts[day.id] != layoutDay(day, plan.blocks, index, hidden.toSet())) {
                        mismatches += "$name ${day.id} $hidden"
                    }
                }
            }
        }
        assertThat(mismatches).isEqualTo(emptyList<String>())
    }

    /** (new) The day scrolls horizontally when the lane minimum exceeds the available width. */
    @Test fun overflowWhenMinWidthExceedsAvailable() {
        val layout =
            layoutDay(
                sampleDay(),
                listOf(block("a", 0.0, 100.0), block("b", 10.0, 60.0), block("c", 20.0, 50.0)),
                emptyIndex(),
            )
        assertThat(layout.minWidth).isEqualTo(282.0)
        val narrow = TimelineGeometry(layout, 248.0)
        assertThat(narrow.width).isEqualTo(282.0)
        assertThat(narrow.laneWidth).isEqualTo(55.5)
        val wide = TimelineGeometry(layout, 400.0)
        assertThat(wide.width).isEqualTo(400.0)
        assertThat(wide.laneWidth).isEqualTo(85.0)
        for (tile in narrow.tiles) {
            assertThat(tile.hitFrame.width >= 48 && tile.hitFrame.height >= 48).isEqualTo(true)
            assertThat(tile.hitFrame.minX >= 0 && tile.hitFrame.maxX <= narrow.width).isEqualTo(true)
            assertThat(tile.hitFrame.minY >= 0 && tile.hitFrame.maxY <= narrow.height).isEqualTo(true)
        }
    }
}
