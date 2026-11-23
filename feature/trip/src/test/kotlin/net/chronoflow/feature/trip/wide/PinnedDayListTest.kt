package net.chronoflow.feature.trip.wide

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
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
class PinnedDayListTest : WideComponentTest() {
    @Test fun listPinsBelowTheCompactBand() {
        val host = start()
        val id =
            host.model.prepared.days[0]
                .id
        val before = compose.onNodeWithTag("trip.wide.timeline.$id").getUnclippedBoundsInRoot()
        compose.onNodeWithTag("trip.scroll").performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, with(compose.density) { 480.dp.toPx() })
        }
        compose.waitForIdle()
        val list = compose.onNodeWithTag("trip.wide.list.$id").getUnclippedBoundsInRoot()
        val band = compose.onNodeWithTag("trip.compactHeader").getUnclippedBoundsInRoot()
        val page = compose.onNodeWithTag("trip.scroll").getUnclippedBoundsInRoot()
        val timeline = compose.onNodeWithTag("trip.wide.timeline.$id").getUnclippedBoundsInRoot()
        val day = compose.onNodeWithTag("trip.day.$id").getUnclippedBoundsInRoot()
        val tolerance = 0.5f / compose.density.density
        assertTrue("pins below band: $list / $band", list.top >= band.bottom - tolerance.dp)
        assertTrue(list.top >= page.top - tolerance.dp)
        assertTrue(list.bottom <= day.bottom + tolerance.dp)
        assertTrue(list.height > 0.dp && list.height <= 700.dp)
        assertEquals(480f, before.top.value - timeline.top.value, tolerance)
    }

    @Test fun listFollowsTheDayEnd() {
        val host = start()
        val id =
            host.model.prepared.days[0]
                .id
        val height = compose.onNodeWithTag("trip.day.$id").getUnclippedBoundsInRoot().height
        compose.runOnIdle {
            host.listState!!.requestScrollToItem(
                host.model.index.timelineIndex(id),
                with(compose.density) {
                    (height - 36.dp - 100.dp).roundToPx()
                },
            )
        }
        compose.waitForIdle()
        val list = compose.onNodeWithTag("trip.wide.list.$id").getUnclippedBoundsInRoot()
        assertTrue("list shrinks to its day end", list.height <= 100.dp + (0.5f / compose.density.density).dp)
    }

    @Test fun bandHeightFollowsTheFontScale() {
        start()
        val band = compose.onNodeWithTag("trip.compactHeader").getUnclippedBoundsInRoot()
        assertEquals(36f, band.height.value, 0.5f / compose.density.density)
    }
}
