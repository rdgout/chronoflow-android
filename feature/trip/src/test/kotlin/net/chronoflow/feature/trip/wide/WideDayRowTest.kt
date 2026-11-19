package net.chronoflow.feature.trip.wide

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.width
import net.chronoflow.core.navigation.TripSheet
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WideDayRowTest : WideComponentTest() {
    @Test fun wideRowPlacesListBesideTimeline() {
        val host = start()
        val id =
            host.model.prepared.days[0]
                .id
        val list = compose.onNodeWithTag("trip.wide.list.$id").getUnclippedBoundsInRoot()
        val timeline = compose.onNodeWithTag("trip.wide.timeline.$id").getUnclippedBoundsInRoot()
        val tolerance = 0.5f / compose.density.density
        assertEquals(280f, list.width.value, tolerance)
        assertEquals(16f, list.left.value - timeline.right.value, tolerance)
        assertTrue(timeline.left.value >= 20f - tolerance)
        assertTrue(list.right.value <= 649f + tolerance)
        compose.onNodeWithTag("trip.viewToggle").assertDoesNotExist()
    }

    @Test fun timelineTapOutlinesTheListRow() {
        val host = start()
        val day = host.model.prepared.days[0]
        val id = day.blocks.first { it.block.type != net.chronoflow.core.model.contracts.BlockType.ZoneChange }.id
        val target =
            compose
                .onNode(hasTestTag("trip.block.$id") and hasAnyAncestor(hasTestTag("trip.wide.timeline.${day.id}")))
                .fetchSemanticsNode()
                .boundsInRoot
        val row = compose.onNodeWithTag("trip.day.${day.id}").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("trip.day.${day.id}").performTouchInput {
            click(Offset(target.center.x - row.left, target.center.y - row.top))
        }
        compose.waitForIdle()
        assertEquals(TripSheet.Step(id), host.vm.sheet.value)
        compose
            .onNode(hasTestTag("trip.block.$id") and hasAnyAncestor(hasTestTag("trip.wide.list.${day.id}")))
            .assertExists()
    }
}
