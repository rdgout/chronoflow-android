package net.chronoflow.core.plan.drawing

import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.plan.TimelineGeometry
import net.chronoflow.core.plan.timelineBlockFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** The drawing's accessibility targets: S3 in nl, one case per content width, density 3. */
@RunWith(Parameterized::class)
class TimelineTargetsTest(
    private val content: Int,
) {
    @Test fun targetsFollowBlocksDividersAndVisibleTicks() {
        val fixture = DrawingFixtures.prepared("S3", Language.Nl)
        for (day in fixture.plan.days) {
            val layout = fixture.layouts.getValue(day.id)
            val key =
                DrawingFixtures.key(day, fixture.plan.revision, content + 32, density = 3f, language = Language.Nl)
            val drawing = DrawingFixtures.build(day, layout, key)
            val geometry = TimelineGeometry(layout, key.availableWidthPx / 3.0)
            val blocks = drawing.targets.filter { it.kind == TimelineTarget.Kind.BLOCK }
            for (target in blocks) {
                val frame = timelineBlockFrame(target.blockId!!, geometry, layout)!!
                assertEquals(frame.minX * 3, target.left.toDouble(), 0.01)
                assertEquals(frame.minY * 3, target.top.toDouble(), 0.01)
                assertEquals(frame.maxX * 3, target.right.toDouble(), 0.01)
                assertEquals(frame.maxY * 3, target.bottom.toDouble(), 0.01)
            }
            val dividers = drawing.targets.filter { it.kind == TimelineTarget.Kind.DIVIDER }
            assertEquals(layout.dividers.size, dividers.size)
            for ((j, target) in dividers.withIndex()) {
                assertEquals(drawing.dividerFrames[j * 4], target.left, 0f)
                assertEquals(drawing.dividerFrames[j * 4 + 3], target.bottom, 0f)
                assertEquals(day.dividerLabels[j], target.label)
            }
            assertEquals(
                layout.ticks.count { it.visible },
                drawing.targets.count { it.kind == TimelineTarget.Kind.TICK },
            )
            val indices = drawing.targets.map { it.traversalIndex }
            assertTrue("traversal strictly increases on ${day.id}", indices.zipWithNext().all { (a, b) -> a < b })
            val tags = drawing.targets.mapNotNull { it.testTag }
            assertEquals(tags.size, tags.toSet().size)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "content {0}")
        fun cases(): List<Int> = listOf(288, 358, 637)
    }
}
