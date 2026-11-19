package net.chronoflow.feature.trip.page

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EndOfPlanCardTest : PageComponentTest() {
    @Test fun endHeadingAndReturnActionAreAvailable() {
        val plan = prepared.copy(returnDayID = prepared.days[3].id)
        var picked: String? = null
        host { EndOfPlanCard(plan, { picked = it }) }
        compose.onNodeWithTag("trip.plan.end.title").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        compose.onNodeWithTag("trip.plan.end.return").performClick()
        assertEquals(plan.returnDayID, picked)
    }

    @Test fun absentReturnAndAssumptionDoNotReserveContent() {
        val plan = prepared.copy(returnDayID = null, assumedEnd = null)
        host { EndOfPlanCard(plan, {}) }
        compose.onNodeWithTag("trip.plan.end.return").assertDoesNotExist()
        compose.onNodeWithTag("trip.plan.end.assumed").assertDoesNotExist()
        compose.onNodeWithTag("trip.plan.end.text").assertExists()
    }
}
