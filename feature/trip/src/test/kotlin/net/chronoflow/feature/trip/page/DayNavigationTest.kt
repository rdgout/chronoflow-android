package net.chronoflow.feature.trip.page

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
class DayNavigationTest : PageComponentTest() {
    @Test fun previousNextAndSelectedPillFollowTheSelection() {
        val days = prepared.days.take(7)
        var picked: String? = null
        host { DayNavigation(days, days[0].id, { picked = it }) }
        compose.onNodeWithTag("trip.days.previous").assertIsNotEnabled()
        compose.onNodeWithTag("trip.days.next").assertIsEnabled().performClick()
        assertEquals(days[1].id, picked)
        compose.onNodeWithTag("trip.days.pill.0").assertIsSelected()
    }

    @Test fun changingSelectionScrollsThePillIntoTheViewport() {
        val days = prepared.days.take(7)
        val selection = mutableStateOf(days[0].id)
        host { DayNavigation(days, selection.value, {}) }
        compose.runOnIdle { selection.value = days[6].id }
        compose.onNodeWithTag("trip.days.pill.6").assertIsDisplayed().assertIsSelected()
        val pill = compose.onNodeWithTag("trip.days.pill.6").getUnclippedBoundsInRoot()
        val row = compose.onNodeWithTag("trip.days").getUnclippedBoundsInRoot()
        assertTrue(pill.left >= row.left)
        assertTrue(pill.right <= row.right)
        compose.onNodeWithTag("trip.days.next").assertIsNotEnabled()
    }

    @Test fun oneDayDisablesBothChevrons() {
        val days = prepared.days.take(1)
        host { DayNavigation(days, null, {}) }
        compose.onNodeWithTag("trip.days.previous").assertIsNotEnabled()
        compose.onNodeWithTag("trip.days.next").assertIsNotEnabled()
    }
}
