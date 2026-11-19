package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Content width of a profile: window minus horizontal insets minus both page gutters. */
fun contentWidth(profile: DeviceProfile): Double =
    profile.widthDp - profile.insets.left.toDouble() - profile.insets.right - 2 * PlanMetrics.gutter

@RunWith(Parameterized::class)
class TimelineGeometryFitTests(
    private val profileName: String,
) {
    @Test fun allScenarioDaysFitDeviceWidths() {
        val available = contentWidth(DeviceProfiles[profileName])
        val problems = mutableListOf<String>()
        for (name in listOf("S1", "S2", "S3", "S4", "S5", "S6", "S6-RETURN-10N")) {
            val f = fixture(name)
            val layouts = layoutPlan(f.plan, f.timeline)
            for (day in f.plan.days) {
                val layout = layouts.getValue(day.id)
                val geometry = TimelineGeometry(layout, available)
                val label = "$name ${day.id}"
                if (geometry.height != layout.height) problems += "$label: height"
                if (!(geometry.width >= available && geometry.width >= layout.minWidth)) problems += "$label: width"
                if (geometry.laneWidth < PlanMetrics.minColumnWidth) problems += "$label: laneWidth"
                for (tile in geometry.tiles) {
                    val frame = tile.frame
                    val hit = tile.hitFrame
                    if (!(frame.width > 0 && frame.height > 0)) problems += "$label ${tile.blockID}: empty"
                    if (!(frame.minX >= PlanMetrics.gutterWidth && frame.maxX <= geometry.width)) {
                        problems +=
                            "$label ${tile.blockID}: x"
                    }
                    if (!(frame.minY >= 0 && frame.maxY <= geometry.height + 0.001)) {
                        problems +=
                            "$label ${tile.blockID}: y"
                    }
                    if (!(hit.width >= PlanMetrics.minTouchTarget && hit.height >= PlanMetrics.minTouchTarget)) {
                        problems += "$label ${tile.blockID}: hit size"
                    }
                    if (!(hit.minX >= 0 && hit.maxX <= geometry.width)) problems += "$label ${tile.blockID}: hit x"
                }
            }
        }
        assertThat(problems).isEqualTo(emptyList<String>())
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun profiles(): List<String> = DeviceProfiles.all.map { it.name }
    }
}
