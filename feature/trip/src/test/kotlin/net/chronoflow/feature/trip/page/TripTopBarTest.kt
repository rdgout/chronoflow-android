package net.chronoflow.feature.trip.page

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TripTopBarTest : PageComponentTest() {
    private val title: TripTitleParts get() {
        val trip = LargeTimelineFixtures.fixture("S1").detail.trip
        return TripTitleParts(trip.from.iata, trip.to.iata, trip.from.city!!, trip.to.city!!)
    }

    @Test fun preparingHasAnInertTitleAndDisabledActions() {
        host { TripTopBar(null, null, false, null, false, {}, {}, null) }
        compose.onNodeWithTag("trip.title").assertHasNoClickAction()
        compose.onNodeWithTag("trip.dayMenu").assertDoesNotExist()
        compose.onNodeWithTag("trip.actions.open").assertIsNotEnabled()
    }

    @Test fun readyTitleExposesDayStateAndSubtitle() {
        val plan = prepared
        val scrolled = mutableStateOf(false)
        host { TripTopBar(title, plan, true, plan.days[1], scrolled.value, {}, {}, null) }
        compose
            .onNodeWithTag(
                "trip.dayMenu",
            ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
        compose.onNodeWithTag("trip.subtitle", useUnmergedTree = true).assertExists()
        compose.runOnIdle { scrolled.value = true }
        compose
            .onNodeWithTag(
                "trip.dayMenu",
            ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
    }

    @Test fun nativeMenuContainsEachDayAndClosesAfterPicking() {
        val plan = prepared
        var picked = -1
        host { TripTopBar(title, plan, true, plan.days[1], false, {}, { picked = it }, null) }
        compose.onNodeWithTag("trip.dayMenu").performClick()
        val tags =
            plan.days.indices
                .map { "trip.dayMenu.day.$it" }
                .toSet()
        compose
            .onAllNodes(
                SemanticsMatcher("fixture day tags") {
                    it.config.getOrNull(SemanticsProperties.TestTag) in
                        tags
                },
            ).assertCountEquals(plan.days.size)
        compose.onNodeWithTag("trip.dayMenu.day.1").assertIsSelected()
        compose.onNodeWithTag("trip.dayMenu.check.1", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("trip.dayMenu.day.4").performClick()
        assertEquals(4, picked)
        compose.onNodeWithTag("trip.dayMenu.day.1").assertDoesNotExist()
    }
}
