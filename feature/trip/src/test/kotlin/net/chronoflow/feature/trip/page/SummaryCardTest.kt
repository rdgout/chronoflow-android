package net.chronoflow.feature.trip.page

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.plan.trippage.ClockLabel
import net.chronoflow.core.plan.trippage.DualClockValue
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.timeline.CountingTimelineProbe
import net.chronoflow.feature.trip.timeline.LocalTimelineProbe
import net.chronoflow.feature.trip.timeline.TimelineProbe.Kind
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SummaryCardTest : PageComponentTest() {
    @Test fun legendDisablesUnavailableActionsAndInvokesAvailableOnes() {
        val enabled = mutableStateOf(false)
        var clicks = 0
        val plan = prepared
        host { SummaryCard(plan, SummaryClockState(), if (enabled.value) ({ clicks++ }) else null) }
        compose.onNodeWithTag("trip.summary").assertExists()
        compose.onNodeWithTag("trip.legend").assertIsNotEnabled()
        compose.runOnIdle { enabled.value = true }
        compose.onNodeWithTag("trip.legend").assertIsEnabled().performClick()
        assertEquals(1, clicks)
    }

    @Test fun minuteValuesRecomposeOnlyTheClockReader() {
        val probe = CountingTimelineProbe()
        val clocks = SummaryClockState()
        val plan = prepared
        host { CompositionLocalProvider(LocalTimelineProbe provides probe) { SummaryCard(plan, clocks, null) } }
        val before = probe.snapshot()
        compose.runOnIdle { clocks.apply(listOf(DualClockValue(ClockLabel.HomeTime, "AMS", "12:01", false))) }
        compose.onNodeWithTag("trip.summary.clock.0").assertExists()
        assertEquals(1, probe.snapshot().delta(before).composed(Kind.SUMMARY_CLOCK, "summary"))
    }
}
