package net.chronoflow.core.plan

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.model.contracts.TripDetail
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.plan.trippage.TripPageLayout
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.LargeTimelineFixtures
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
        val (layouts, prepared) = fixtures.getValue(fixtureName)
        val problems = mutableListOf<String>()
        for (day in prepared.days) {
            val layout = layouts.getValue(day.id)
            val geometry = TimelineGeometry(layout, width)
            for (id in day.blocks.map { it.id }) {
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
        private val fixtures: Map<String, Pair<Map<String, DayLayout>, PreparedPlan>> by lazy {
            mapOf("S3" to Language.Nl, "large60" to Language.En).mapValues { (name, language) ->
                val detail: TripDetail = LargeTimelineFixtures.fixture(name).detail
                val layouts = layoutPlan(detail.plan, detail.timeline)
                layouts to prepare(detail, language, layouts = layouts)
            }
        }

        @JvmStatic
        @Parameterized.Parameters(name = "{0} {1}")
        fun cases(): List<Array<Any>> {
            val widths = ArrayList<Pair<String, Double>>()
            for (fixed in listOf(288.0, 358.0, 637.0)) widths += "content $fixed" to fixed
            for (profile in DeviceProfiles.all) {
                widths += "${profile.name} content" to contentWidth(profile)
                if (profile.widthDp >= PlanMetrics.wideBreakpoint) {
                    val list = checkNotNull(TripPageLayout.resolve(profile.widthDp.toDouble()).listWidth)
                    val window = profile.widthDp - profile.insets.left.toDouble() - profile.insets.right
                    widths +=
                        "${profile.name} wide" to window - 2 * PlanMetrics.wideGutter - PlanMetrics.wideColumnGap - list
                }
            }
            return listOf("S3", "large60").flatMap { name ->
                widths.map { (label, width) -> arrayOf(name, label, width) }
            }
        }
    }
}
