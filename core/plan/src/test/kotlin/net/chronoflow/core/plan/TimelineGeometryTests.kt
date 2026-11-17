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
}
