package net.chronoflow.feature.trip.timeline

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.sp
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

@OptIn(ExperimentalTestApi::class)
@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TimelinePageTest {
    private val composeRule = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(composeRule)

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private var listState: LazyListState? = null

    @After fun restoreAnimators() {
        AnimatorScale.set(1f)
    }

    private fun show(
        host: TimelineTestHost,
        reduceMotion: Boolean = false,
    ) {
        if (reduceMotion) AnimatorScale.set(0f)
        composeRule.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                TimelinePage(host.viewModel, onBlock = {}, onListState = { listState = it })
            }
        }
        composeRule.waitForIdle()
    }

    private val thresholdPx: Int get() = with(composeRule.density) { PlanMetrics.compactHeaderHeight.sp.roundToPx() }

    private fun TimelineTestHost.dayId(position: Int) = model.prepared.days[position].id

    private fun assertPinnedBelowHeader(dayId: String) {
        composeRule.onNodeWithTag("trip.day.$dayId").assertIsDisplayed()
        val scrollTop = composeRule.onNodeWithTag("trip.scroll").getUnclippedBoundsInRoot().top
        val dayTop = composeRule.onNodeWithTag("trip.day.$dayId").getUnclippedBoundsInRoot().top
        val expected = scrollTop + with(composeRule.density) { thresholdPx.toDp() }
        assertEquals("day top vs list top + threshold", expected.value, dayTop.value, 1f)
    }

    @Test fun opensOnTheDayContainingNow() {
        val probe = TimelineTestHost()
        val host = TimelineTestHost(clockStart = probe.dayStart(33).plusSeconds(2 * 3_600))
        show(host)
        assertPinnedBelowHeader(host.dayId(33))
        composeRule.onNodeWithTag("trip.day.${host.dayId(32)}").assertDoesNotExist()
    }

    @Test fun pastTripOpensOnLastDay() {
        val probe = TimelineTestHost(fixture = "S3")
        val last =
            probe.model.prepared.days
                .last()
        val end =
            checkNotNull(
                net.chronoflow.core.plan
                    .planDate(last.day.endUtc),
            )
        val host = TimelineTestHost(fixture = "S3", clockStart = end.plusSeconds(86_400))
        show(host)
        assertEquals(last.id, host.model.openingDayId)
        // A short last day cannot reach the top of the list: the list stops at its end.
        composeRule.onNodeWithTag("trip.day.${last.id}").assertIsDisplayed()
    }

    @Test fun menuListsEveryDay() {
        val host = TimelineTestHost()
        show(host)
        composeRule.onNodeWithTag("trip.dayMenu").performClick()
        composeRule.waitUntilExactlyOneExists(hasTestTag("trip.dayMenu.day.0"), 10_000)
        composeRule.waitUntilExactlyOneExists(hasTestTag("trip.dayMenu.day.59"), 10_000)
    }

    @Test fun menuJumpReachesTheEnd() {
        val host = TimelineTestHost()
        show(host, reduceMotion = true)
        composeRule.onNodeWithTag("trip.dayMenu").performClick()
        composeRule.waitUntilExactlyOneExists(hasTestTag("trip.dayMenu.day.59"), 10_000)
        composeRule.onNodeWithTag("trip.dayMenu.day.59").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertPinnedBelowHeader(host.dayId(59))
        composeRule.onNodeWithTag("trip.scroll").performScrollToNode(hasTestTag("trip.plan.end"))
        composeRule.onNodeWithTag("trip.plan.end").assertIsDisplayed()
    }

    @Test fun stickyHeaderStaysPinned() {
        val probe = TimelineTestHost()
        val host = TimelineTestHost(clockStart = probe.dayStart(33).plusSeconds(2 * 3_600))
        show(host)
        val day = host.model.prepared.days[33]
        composeRule.runOnIdle { listState!!.dispatchRawDelta(300f * composeRule.density.density) }
        composeRule.waitForIdle()
        val title = context.getString(R.string.plan_day_title_date, day.title, day.date)
        val header = composeRule.onNode(hasText(title))
        val scrollTop = composeRule.onNodeWithTag("trip.scroll").getUnclippedBoundsInRoot().top
        assertEquals(scrollTop.value, header.getUnclippedBoundsInRoot().top.value, 1f)
    }

    @Test fun anchorFollowsTheCurrentDay() {
        val probe = TimelineTestHost()
        val host = TimelineTestHost(clockStart = probe.dayStart(33).plusSeconds(2 * 3_600))
        show(host)
        val next = host.model.prepared.days[34]
        composeRule.runOnIdle { listState!!.requestScrollToItem(host.model.index.headerIndex(next.id)) }
        composeRule.waitForIdle()
        assertEquals(next.id, host.viewModel.anchorDayId.value)
        val node = composeRule.onNodeWithTag("trip.dayMenu").fetchSemanticsNode()
        assertEquals(
            context.getString(R.string.plan_day_title_date, next.title, next.date),
            node.config[SemanticsProperties.StateDescription],
        )
    }
}
