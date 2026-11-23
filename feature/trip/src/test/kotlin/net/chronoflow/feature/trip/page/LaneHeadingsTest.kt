package net.chronoflow.feature.trip.page

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LaneHeadingsTest : PageComponentTest() {
    @Test fun compactLanesExposeOneDayDescription() {
        val day = prepared.days[0]
        host { LaneHeadings(day) }
        compose
            .onNodeWithTag(
                "trip.lanes",
            ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
    }

    @Test fun itemLanesHideTheirLabels() {
        host { LaneHeadings(null) }
        compose
            .onNodeWithTag(
                "trip.lanes",
            ).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }
}
