package net.chronoflow.feature.trip.timeline

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.plan.checkIdentity
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.timeline.TimelineProbe.Kind
import net.chronoflow.feature.trip.timeline.host.TimelineSourceInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Port of iOS `TimelineInvalidationTests.scrollingAndTicksInvalidateOnlyAffectedClockChildren` over all 60 days of
 * large60, split into one test per event. The probe counts compositions and static draws per day; every drawing is
 * prewarmed, so no placeholder draws in the measured windows.
 */
@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TimelineInvalidationTest {
    private val composeRule = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(composeRule)

    private val probe = CountingTimelineProbe()
    private val host = TimelineTestHost()
    private var listState: LazyListState? = null

    private val thresholdPx: Int get() = with(composeRule.density) { PlanMetrics.compactHeaderHeight.sp.roundToPx() }
    private val dayIds: List<String> get() =
        host.model.prepared.days
            .map { it.id }

    private fun start() {
        val density = composeRule.density
        host.prewarm(
            density,
            windowDp =
                (DeviceProfiles.pixel8.widthDp * density.density).toInt() / density.density.toDouble(),
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalTimelineProbe provides probe) {
                ProfileHost(DeviceProfiles.pixel8) {
                    TimelinePage(host.viewModel, onBlock = {}, onListState = { listState = it })
                }
            }
        }
        settle()
        assertEquals("a key mismatch would build drawings", 0, host.cache.buildCount)
    }

    /** Idle, then force a draw: under Robolectric a draw lambda only runs when the window is drawn. */
    private fun settle() {
        composeRule.waitForIdle()
        composeRule.onRoot().captureToImage()
    }

    private fun scrollBy(px: Float) {
        composeRule.runOnIdle { listState!!.dispatchRawDelta(px) }
        composeRule.waitForIdle()
    }

    private fun dayItemOffset(dayId: String): Int? {
        val index = host.model.index.dayIndex(dayId)
        return listState!!
            .layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == index }
            ?.offset
    }

    private fun assertNothingStaticMoved(
        before: CountingTimelineProbe.Counts,
        delta: CountingTimelineProbe.Counts,
        kinds: List<Kind> = listOf(Kind.STATIC, Kind.HEADER, Kind.CLOCK, Kind.TARGETS),
    ) {
        for (id in before.composedIds(Kind.STATIC)) {
            for (kind in kinds) assertEquals("$kind composition of $id", 0, delta.composed(kind, id))
            assertEquals("static draws of $id", 0, delta.drew(Kind.STATIC, id))
        }
    }

    @Test fun scrollingAndTicksInvalidateOnlyAffectedClockChildrenWhileScrolling() {
        start()
        val before = probe.snapshot()
        repeat(120) { scrollBy(composeRule.density.density) }
        settle()
        val afterDeltas = probe.snapshot().delta(before)
        assertNothingStaticMoved(before, afterDeltas)
        assertTrue(
            "the anchor menu flips once, not once per delta",
            afterDeltas.composed
                .filterKeys { it.kind == Kind.ANCHOR }
                .values
                .sum() <= 1,
        )

        val first = dayIds[0]
        val second = dayIds[1]
        var guard = 0
        while ((dayItemOffset(second) ?: Int.MAX_VALUE) > thresholdPx &&
            guard++ < 60
        ) {
            scrollBy(40f * composeRule.density.density)
        }
        settle()
        val delta = probe.snapshot().delta(before)
        assertNothingStaticMoved(before, delta)
        for (id in delta.composedIds(Kind.STATIC) - before.composedIds(Kind.STATIC)) {
            assertEquals("a day entering composes once", 1, delta.composed(Kind.STATIC, id))
            assertTrue("and draws at most once", delta.drew(Kind.STATIC, id) <= 1)
        }
        assertEquals(second, host.viewModel.anchorDayId.value)
        assertTrue(
            "positive control: the anchor change is observed by the menu",
            delta.composed
                .filterKeys { it.kind == Kind.ANCHOR }
                .values
                .sum() >= 1,
        )
        assertTrue(first != second)
    }

    @Test fun scrollingWithinThresholdDoesNotRecomposeDayMenu() {
        start()
        scrollBy(2f * composeRule.density.density)
        settle()
        val before = probe.snapshot()
        repeat(20) { scrollBy(composeRule.density.density) }
        settle()
        val delta = probe.snapshot().delta(before)
        assertEquals(
            "DayMenu must not observe per-frame layoutInfo changes within the same threshold",
            0,
            delta.composed
                .filterKeys { it.kind == Kind.ANCHOR }
                .values
                .sum(),
        )
    }

    @Test fun scrollingAndTicksInvalidateOnlyAffectedClockChildrenOnMinuteTick() {
        start()
        val before = probe.snapshot()
        host.clock.advance(host.dayStart(0).plusSeconds(60))
        settle()
        val delta = probe.snapshot().delta(before)
        assertNothingStaticMoved(before, delta, listOf(Kind.STATIC, Kind.HEADER, Kind.TARGETS))
        assertEquals(1, delta.composed(Kind.CLOCK, dayIds[0]))
        assertEquals(
            "only one clock child",
            1,
            delta.composed
                .filterKeys { it.kind == Kind.CLOCK }
                .values
                .sum(),
        )
    }

    @Test fun scrollingAndTicksInvalidateOnlyAffectedClockChildrenOnDayBoundary() {
        start()
        val first = host.model.prepared.days[0]
        val heightPx =
            with(composeRule.density) {
                host.model.layouts
                    .getValue(first.id)
                    .height.dp
                    .roundToPx()
            }
        composeRule.runOnIdle {
            listState!!.requestScrollToItem(
                host.model.index.dayIndex(first.id),
                heightPx - with(composeRule.density) { 300.dp.roundToPx() },
            )
        }
        settle()
        val before = probe.snapshot()
        host.clock.advance(host.dayStart(1))
        settle()
        val delta = probe.snapshot().delta(before)
        assertNothingStaticMoved(before, delta, listOf(Kind.STATIC, Kind.HEADER, Kind.TARGETS))
        assertEquals(1, delta.composed(Kind.CLOCK, dayIds[0]))
        assertEquals(1, delta.composed(Kind.CLOCK, dayIds[1]))
        assertEquals(
            "exactly two clock children",
            2,
            delta.composed
                .filterKeys { it.kind == Kind.CLOCK }
                .values
                .sum(),
        )
    }

    @Test fun checkOffRedrawsOnlyThatDay() {
        start()
        val prepared = host.model.prepared
        val identities =
            host.model.detail.plan.blocks
                .mapNotNull { b -> b.checkKey?.let { checkIdentity(it) } }
        val (dayIndex, block) =
            prepared.days
                .withIndex()
                .flatMap { (i, day) ->
                    day.blocks
                        .filter {
                            it.block.actionable && it.block.checkKey != null && !it.continuesBefore &&
                                !it.continuesAfter &&
                                identities.count { id -> id == checkIdentity(checkNotNull(it.block.checkKey)) } == 1
                        }.map { i to it }
                }.first()
        val dayId = prepared.days[dayIndex].id
        composeRule.runOnIdle { listState!!.requestScrollToItem(host.model.index.headerIndex(dayId)) }
        settle()
        val before = probe.snapshot()
        host.inputs.value =
            TimelineSourceInput(
                host.bytes,
                listOf(CheckMark(checkNotNull(block.block.checkKey), CheckStatus.Done, "")),
                host.model.timeFormat,
                emptyList(),
            )
        settle()
        val delta = probe.snapshot().delta(before)
        assertEquals("the day is recomposed once", 1, delta.composed(Kind.STATIC, dayId))
        val draws = delta.drew(Kind.STATIC, dayId)
        assertTrue("and redrawn once or twice, was $draws", draws in 1..2)
        assertEquals(0, delta.composed(Kind.CLOCK, dayId))
        for (id in before.composedIds(Kind.STATIC) - dayId) {
            for (kind in listOf(Kind.STATIC, Kind.HEADER, Kind.CLOCK)) {
                assertEquals("$kind composition of $id", 0, delta.composed(kind, id))
            }
            assertEquals("static draws of $id", 0, delta.drew(Kind.STATIC, id))
        }
    }
}
