package net.chronoflow.feature.trip.wide

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DayMenuWideTest : WideComponentTest() {
    @Test fun dayMenuKeepsNativeSelectionAndJumpsToTheDay() {
        val host = start(841)
        val day = host.model.prepared.days[0]
        compose.runOnIdle {
            host.listState!!.requestScrollToItem(
                host.model.index.timelineIndex(day.id),
                with(compose.density) {
                    180.dp.roundToPx()
                },
            )
        }
        compose.waitForIdle()
        compose.onNodeWithTag("trip.dayMenu").performClick()
        compose.onNodeWithTag("trip.dayMenu.day.0").assertIsSelected()
        compose.onNodeWithTag("trip.dayMenu.day.1").performClick()
        compose.waitForIdle()
        val next = host.model.prepared.days[1]
        val header = compose.onNodeWithTag("trip.headerItem.${next.id}").getUnclippedBoundsInRoot()
        val page = compose.onNodeWithTag("trip.scroll").getUnclippedBoundsInRoot()
        assertEquals(page.top.value, header.top.value, 1f / compose.density.density)
    }
}
