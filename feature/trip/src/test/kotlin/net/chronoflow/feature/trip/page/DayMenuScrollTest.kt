package net.chronoflow.feature.trip.page

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.timeline.TimelineTestPlans
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DayMenuScrollTest : PageComponentTest() {
    @Test fun openingTheMenuRevealsTheCurrentDay() {
        val plan = TimelineTestPlans.prepared("large60").plan
        val title = TripTitleParts("AMS", "SYD", "Amsterdam", "Sydney")
        host { TripTopBar(title, plan, true, plan.days[50], false, {}, {}, null) }
        compose.onNodeWithTag("trip.dayMenu").performClick()
        compose.onNodeWithTag("trip.dayMenu.day.50").assertIsDisplayed().assertIsSelected()
    }
}
