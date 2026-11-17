package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.MockTripTemplates
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Port of `TimelineRenderingTests.accessibilityTargetsFollowTilesAndGutterMarkers`, one case per fixture and width. */
@RunWith(Parameterized::class)
class TimelineGeometryProfilesTest(
    private val fixtureName: String,
    @Suppress("unused") private val label: String,
    private val width: Double,
) {
    @Test fun accessibilityTargetsFollowTilesAndGutterMarkers() {
        val layouts = layouts.getValue(fixtureName)
        val problems = mutableListOf<String>()
        for (layout in layouts) {
            val geometry = TimelineGeometry(layout, width)
            val blocks =
                layout.placed.values
                    .flatten()
                    .map { it.block.id } +
                    layout.markers.map { it.block.id } + layout.changes.map { it.block.id }
            for (id in blocks) {
                val frame = timelineBlockFrame(id, geometry, layout)
                if (frame == null) {
                    problems += "$id: no frame"
                    continue
                }
                if (frame.height < PlanMetrics.minTouchTarget || frame.width < PlanMetrics.minTouchTarget) {
                    problems += "$id: ${frame.width}x${frame.height} is below the minimum target"
                }
                if (frame.minX < 0 || frame.minY < 0 || frame.maxX > geometry.width + 1e-9 ||
                    frame.maxY > geometry.height + 1e-9
                ) {
                    problems += "$id: outside the day"
                }
                val tile = geometry.tiles.firstOrNull { it.blockID == id }
                if (tile != null && tile.hitFrame != frame) problems += "$id: not the hit frame"
            }
        }
        assertThat(problems.take(5)).isEqualTo(emptyList<String>())
    }

    companion object {
        private val layouts: Map<String, List<DayLayout>> by lazy {
            val s3: TripDetail = MockTripTemplates.templates()[2].detail
            mapOf("S3" to layoutPlan(s3.plan, s3.timeline).values.toList())
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0} {1}")
        fun cases(): List<Array<Any>> {
            val widths = ArrayList<Pair<String, Double>>()
            for (fixed in listOf(288.0, 358.0, 637.0)) widths += "content $fixed" to fixed
            for (profile in DeviceProfiles.all) {
                widths += "${profile.name} content" to contentWidth(profile)
                if (profile.widthDp >= PlanMetrics.wideBreakpoint) {
                    val list =
                        if (profile.widthDp >= PlanMetrics.wideLargeBreakpoint) {
                            PlanMetrics.wideLargeListWidth
                        } else {
                            PlanMetrics.wideListWidth
                        }
                    val window = profile.widthDp - profile.insets.left.toDouble() - profile.insets.right
                    widths +=
                        "${profile.name} wide" to window - 2 * PlanMetrics.wideGutter - PlanMetrics.wideColumnGap - list
                }
            }
            return listOf("S3").flatMap { name -> widths.map { (label, width) -> arrayOf(name, label, width) } }
        }
    }
}
