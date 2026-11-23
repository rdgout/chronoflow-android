package net.chronoflow.feature.trip.timeline

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.timeline.TimelineProbe.Kind
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The JankStats seam reports the visible day only when the anchor day changes, never per scroll offset. */
@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TimelineJankTaggerTest {
    private val composeRule = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(composeRule)

    private val probe = CountingTimelineProbe()
    private val host = TimelineTestHost()
    private val calls = ArrayList<Pair<Int, String>>()
    private var listState: LazyListState? = null

    private fun settle() {
        composeRule.waitForIdle()
        composeRule.onRoot().captureToImage()
    }

    private fun start() {
        val density = composeRule.density
        host.prewarm(
            density,
            windowDp = (DeviceProfiles.pixel8.widthDp * density.density).toInt() / density.density.toDouble(),
        )
        val tagger = TimelineJankTagger { index, mode -> calls += index to mode }
        composeRule.setContent {
            CompositionLocalProvider(LocalTimelineJankTagger provides tagger, LocalTimelineProbe provides probe) {
                ProfileHost(DeviceProfiles.pixel8) {
                    TimelinePage(host.viewModel, onBlock = {}, onListState = { listState = it })
                }
            }
        }
        settle()
    }

    private val openingIndex: Int get() =
        host.model.prepared.days
            .indexOfFirst { it.id == host.model.openingDayId }

    @Test fun reportsTheOpeningDayOnceAndNothingWhileScrollingInsideIt() {
        start()
        assertEquals(listOf(openingIndex to "timeline"), calls.toList())
        val before = probe.snapshot()
        repeat(120) {
            composeRule.runOnIdle { listState!!.dispatchRawDelta(1f) }
            composeRule.waitForIdle()
        }
        settle()
        assertEquals(1, calls.size)
        val delta = probe.snapshot().delta(before)
        assertEquals(
            0,
            delta.composed
                .filterKeys { it.kind == Kind.STATIC }
                .values
                .sum(),
        )
        assertEquals(
            0,
            delta.composed
                .filterKeys { it.kind == Kind.CLOCK }
                .values
                .sum(),
        )
    }

    @Test fun reportsOneCallWhenTheAnchorMovesToAnotherDay() {
        start()
        val target = openingIndex + 3
        val dayId =
            host.model.prepared.days[target]
                .id
        composeRule.runOnIdle { listState!!.requestScrollToItem(host.model.index.headerIndex(dayId)) }
        settle()
        assertEquals(listOf(openingIndex to "timeline", target to "timeline"), calls.toList())
    }
}
