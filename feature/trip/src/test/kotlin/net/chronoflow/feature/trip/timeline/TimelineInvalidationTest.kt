package net.chronoflow.feature.trip.timeline

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CompletableDeferred
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.contracts.CheckMark
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.model.store.CheckMarkRecord
import net.chronoflow.core.plan.actions.TripAction
import net.chronoflow.core.plan.checkIdentity
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.WindowSizeFake
import net.chronoflow.core.testing.compose.WindowSizeFakeHost
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.actions.TripWizardPrepareOutcome
import net.chronoflow.feature.trip.actions.TripWizardPreparer
import net.chronoflow.feature.trip.page.TripPage
import net.chronoflow.feature.trip.timeline.TimelineProbe.Kind
import org.junit.After
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

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(DeviceProfiles.foldInner.copy(widthDp = 1000, heightDp = 914)),
            ).around(composeRule)

    private val probe = CountingTimelineProbe()
    private lateinit var host: TimelineTestHost
    private var listState: LazyListState? = null

    private val thresholdPx: Int get() = with(composeRule.density) { PlanMetrics.compactHeaderHeight.sp.roundToPx() }
    private val dayIds: List<String> get() =
        host.model.prepared.days
            .map { it.id }

    @After fun close() {
        if (::host.isInitialized) host.close()
    }

    private fun start(
        summary: Boolean = false,
        reduceMotion: Boolean = true,
        list: Boolean = false,
        applyChecks: Boolean = false,
        wide: Boolean = false,
        wizardPreparer: TripWizardPreparer? = null,
        realPage: Boolean = false,
    ) {
        host =
            TimelineTestHost(
                startAtSummary = summary,
                initialTimeline = !list,
                applyChecks = applyChecks,
                wizardPreparer = wizardPreparer,
            )
        if (wizardPreparer != null) {
            val snapshot = host.source.snapshot.value
            val card =
                net.chronoflow.core.testing.LargeTimelineFixtures
                    .fixture("large60")
                    .card
                    .copy(canEdit = true)
            host.source.snapshot.value =
                snapshot.copy(
                    trip =
                        snapshot.trip!!.copy(
                            card =
                                net.chronoflow.core.model.api.ApiJson.response.encodeToString(
                                    net.chronoflow.core.model.contracts.TripCardView
                                        .serializer(),
                                    card,
                                ),
                        ),
                )
        }
        val density = composeRule.density
        val width =
            if (wide) {
                841.0
            } else {
                (DeviceProfiles.pixel8.widthDp * density.density).toInt() / density.density.toDouble()
            }
        val pixels = kotlin.math.round(width * density.density).toInt()
        host.prewarm(density, windowDp = pixels / density.density.toDouble())
        val window = WindowSizeFake(DpSize(width.dp, 914.dp))
        composeRule.setContent {
            CompositionLocalProvider(LocalTimelineProbe provides probe) {
                WindowSizeFakeHost(window, insets = DeviceProfiles.pixel8.insets) {
                    ChronoFlowTheme(reduceMotion = reduceMotion) {
                        if (realPage) TripPage(host.vm, {}, null) else host.Content()
                        listState = host.listState
                    }
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
        val index = host.model.index.timelineIndex(dayId)
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

    private fun assertWidePin() {
        val list = composeRule.onNodeWithTag("trip.wide.list.${dayIds[0]}").fetchSemanticsNode().boundsInRoot
        val band = composeRule.onNodeWithTag("trip.compactHeader").fetchSemanticsNode().boundsInRoot
        assertEquals("the list pins in the same layout pass", band.bottom, list.top, 1f)
    }

    private fun wideScrollDeltas(count: Int) {
        repeat(count) {
            composeRule.runOnUiThread { listState!!.dispatchRawDelta(composeRule.density.density) }
            composeRule.mainClock.advanceTimeByFrame()
        }
    }

    @Test fun wideScrollInvalidatesNoStaticLayerOrRow() {
        start(wide = true, reduceMotion = false)
        composeRule.runOnIdle {
            listState!!.requestScrollToItem(
                host.model.index.timelineIndex(dayIds[0]),
                with(composeRule.density) { 360.dp.roundToPx() },
            )
        }
        settle()
        val passes = composeRule.runOnIdle { CompositionPassProbe() }
        composeRule.mainClock.autoAdvance = false
        try {
            val before = probe.snapshot()
            val beforePasses = passes.count
            assertTrue("both measured days are already composed", dayIds[1] in before.composedIds(Kind.STATIC))
            wideScrollDeltas(1)
            assertWidePin()
            wideScrollDeltas(39)
            assertWidePin()
            wideScrollDeltas(40)
            assertWidePin()
            wideScrollDeltas(40)
            assertWidePin()
            composeRule.mainClock.autoAdvance = true
            settle()
            val delta = probe.snapshot().delta(before)
            assertNothingStaticMoved(before, delta)
            assertEquals("no composition during within-day scroll: ${delta.composed}", 0, passes.count - beforePasses)
            assertEquals(
                0,
                delta.composed
                    .filterKeys {
                        it.kind in
                            setOf(Kind.PILLS, Kind.COMPACT, Kind.LIST, Kind.LIST_ROW)
                    }.values
                    .sum(),
            )
            assertTrue(
                delta.composed
                    .filterKeys { it.kind == Kind.ANCHOR }
                    .values
                    .sum() <= 1,
            )
            val second = dayIds[1]
            composeRule.runOnIdle {
                listState!!.requestScrollToItem(host.model.index.headerIndex(second), thresholdPx)
            }
            settle()
            val crossing = probe.snapshot().delta(before)
            assertNothingStaticMoved(before, crossing)
            for (id in before.composedIds(Kind.LIST)) {
                assertEquals("LIST composition of $id across the boundary", 0, crossing.composed(Kind.LIST, id))
            }
            for (key in before.composedIds(Kind.LIST_ROW)) {
                assertEquals(
                    "list row composition of $key across the boundary",
                    0,
                    crossing.composed(Kind.LIST_ROW, key),
                )
            }
            assertEquals(1, crossing.composed(Kind.COMPACT, second))
            assertTrue(
                "positive control: the day boundary updates the anchor menu",
                crossing.composed
                    .filterKeys { it.kind == Kind.ANCHOR }
                    .values
                    .sum() >= 1,
            )
        } finally {
            composeRule.mainClock.autoAdvance = true
            composeRule.runOnIdle { passes.close() }
        }
    }

    @Test fun wideMinuteTickRedrawsOnlyTheClock() {
        start(wide = true)
        val before = probe.snapshot()
        host.clock.advance(host.dayStart(0).plusSeconds(60))
        settle()
        val delta = probe.snapshot().delta(before)
        assertNothingStaticMoved(before, delta, listOf(Kind.STATIC, Kind.HEADER, Kind.TARGETS))
        assertEquals(1, delta.composed(Kind.CLOCK, dayIds[0]))
        assertEquals(
            0,
            delta.composed
                .filterKeys { it.kind == Kind.LIST }
                .values
                .sum(),
        )
    }

    @Test fun wideSelectionRedrawsOneRow() {
        start(wide = true)
        val day = host.model.prepared.days[0]
        val before = probe.snapshot()
        composeRule.runOnIdle { host.vm.openStep(day.blocks[0].id) }
        settle()
        val delta = probe.snapshot().delta(before)
        assertNothingStaticMoved(before, delta)
        assertEquals(
            0,
            delta.composed
                .filterKeys { it.kind == Kind.LIST || it.kind == Kind.LIST_ROW }
                .values
                .sum(),
        )
    }

    @Test fun scrollingAndTicksInvalidateOnlyAffectedClockChildrenWhileScrolling() {
        start()
        val header =
            listState!!.layoutInfo.visibleItemsInfo.first {
                it.index ==
                    host.model.index.headerIndex(
                        dayIds[0],
                    )
            }
        scrollBy(header.size + 1f)
        settle()
        val before = probe.snapshot()
        repeat(120) { scrollBy(composeRule.density.density) }
        settle()
        val afterDeltas = probe.snapshot().delta(before)
        assertNothingStaticMoved(before, afterDeltas)
        assertEquals(
            0,
            afterDeltas.composed
                .filterKeys { it.kind in setOf(Kind.PILLS, Kind.COMPACT) }
                .values
                .sum(),
        )
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
        assertEquals(1, delta.composed(Kind.COMPACT, second))
        assertEquals(second, host.vm.anchor.value.dayID)
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

    @Test fun actionBusyChangesDoNotRecomposePageChrome() {
        val gate = CompletableDeferred<TripWizardPrepareOutcome>()
        start(wizardPreparer = TripWizardPreparer { _, _ -> gate.await() }, realPage = true)
        val passes = composeRule.runOnIdle { CompositionPassProbe() }
        try {
            val beforePasses = passes.count
            val before = probe.snapshot()
            composeRule.runOnIdle { host.vm.startAction(TripAction.Edit) }
            settle()
            assertTrue(
                "the real action must become busy",
                host.vm.tripActions.value!!
                    .availability.busy,
            )
            gate.complete(TripWizardPrepareOutcome.Ignored)
            settle()
            assertTrue(
                "the real action must finish",
                !host.vm.tripActions.value!!
                    .availability.busy,
            )
            assertEquals("busy must not schedule a page composition", 0, passes.count - beforePasses)
            val delta = probe.snapshot().delta(before)
            assertEquals(
                "busy belongs to the sheet, not page chrome",
                0,
                delta.composed
                    .filterKeys { it.kind in setOf(Kind.PILLS, Kind.COMPACT, Kind.ANCHOR) }
                    .values
                    .sum(),
            )
        } finally {
            composeRule.runOnIdle { passes.close() }
        }
    }

    @Test fun scrollingWithinOneDayDoesNotRecomposePageChrome() {
        start(reduceMotion = false)
        val passes = composeRule.runOnIdle { CompositionPassProbe() }
        try {
            val header =
                listState!!.layoutInfo.visibleItemsInfo.first {
                    it.index ==
                        host.model.index.headerIndex(
                            dayIds[0],
                        )
                }
            scrollBy(header.size + 1f)
            settle()
            assertTrue("positive control: crossing the header threshold schedules composition", passes.count > 0)
            val before = probe.snapshot()
            val beforePasses = passes.count

            val offset = listState!!.firstVisibleItemScrollOffset
            val anchor = host.vm.anchor.value
            repeat(30) { scrollBy(composeRule.density.density) }
            settle()
            assertTrue("the measured scroll must move the list", listState!!.firstVisibleItemScrollOffset > offset)
            assertEquals("the measured scroll stays within one day", anchor, host.vm.anchor.value)
            assertEquals("within-day scrolling must not schedule composition passes", 0, passes.count - beforePasses)
            val delta = probe.snapshot().delta(before)

            assertNothingStaticMoved(before, delta)
            assertEquals(
                0,
                delta.composed
                    .filterKeys { it.kind in setOf(Kind.ANCHOR, Kind.COMPACT, Kind.PILLS) }
                    .values
                    .sum(),
            )
        } finally {
            composeRule.runOnIdle { passes.close() }
        }
    }

    @Test fun scrollingAndTicksInvalidateOnlyAffectedClockChildrenOnMinuteTick() {
        start()
        val before = probe.snapshot()
        host.clock.advance(host.dayStart(0).plusSeconds(60))
        settle()
        val delta = probe.snapshot().delta(before)
        assertNothingStaticMoved(before, delta, listOf(Kind.STATIC, Kind.HEADER, Kind.TARGETS))
        assertEquals(
            0,
            delta.composed
                .filterKeys { it.kind == Kind.COMPACT || it.kind == Kind.ANCHOR }
                .values
                .sum(),
        )
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
                host.model.index.timelineIndex(first.id),
                heightPx - with(composeRule.density) { 300.dp.roundToPx() },
            )
        }
        settle()
        val before = probe.snapshot()
        host.clock.advance(host.dayStart(1))
        settle()
        val delta = probe.snapshot().delta(before)
        assertNothingStaticMoved(before, delta, listOf(Kind.STATIC, Kind.HEADER, Kind.TARGETS))
        assertEquals(
            0,
            delta.composed
                .filterKeys { it.kind == Kind.COMPACT || it.kind == Kind.ANCHOR }
                .values
                .sum(),
        )
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

    @Test fun checkOffRedrawsOnlyThatDay() = assertCheckOffRedrawsOneDay(false)

    @Test fun customActionCheckOffRedrawsOneDay() = assertCheckOffRedrawsOneDay(true)

    private fun assertCheckOffRedrawsOneDay(custom: Boolean) {
        start(applyChecks = custom)
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
        val key = checkNotNull(block.block.checkKey)
        val snapshot = host.source.snapshot.value
        if (custom) {
            composeRule.runOnIdle { host.vm.checkFromAccessibility(block.id, CheckStatus.Done) }
        } else {
            host.source.snapshot.value =
                snapshot.copy(
                    checks =
                        snapshot.checks +
                            CheckMarkRecord(
                                snapshot.trip!!.id,
                                snapshot.plan!!.inputHash,
                                key.type.raw,
                                key.localDate,
                                key.ordinal,
                                "done",
                                "now",
                            ),
                )
        }
        settle()
        val delta = probe.snapshot().delta(before)
        assertEquals("the day is recomposed once", 1, delta.composed(Kind.STATIC, dayId))
        // Check actions arrive before the asynchronous replacement drawing; a batched delivery needs only one pass.
        assertTrue("changed targets compose at most twice", delta.composed(Kind.TARGETS, dayId) in 1..2)
        val draws = delta.drew(Kind.STATIC, dayId)
        assertTrue("and redrawn once or twice, was $draws", draws in 1..2)
        assertEquals(0, delta.composed(Kind.CLOCK, dayId))
        assertTrue(delta.composed(Kind.HEADER, dayId) <= 1)
        for (id in before.composedIds(Kind.STATIC) - dayId) {
            for (kind in listOf(Kind.STATIC, Kind.HEADER, Kind.CLOCK, Kind.TARGETS)) {
                assertEquals("$kind composition of $id", 0, delta.composed(kind, id))
            }
            assertEquals("static draws of $id", 0, delta.drew(Kind.STATIC, id))
        }
    }

    @Test fun listScrollingWithinOneDayDoesNotRecomposeRowsOrPageChrome() {
        start(list = true, reduceMotion = false)
        val passes = composeRule.runOnIdle { CompositionPassProbe() }
        try {
            val header =
                listState!!.layoutInfo.visibleItemsInfo.first {
                    it.index == host.model.listIndex.headerIndex(dayIds.first())
                }
            scrollBy(header.size + 1f)
            settle()
            assertTrue("crossing the header must schedule composition", passes.count > 0)
            val before = probe.snapshot()
            val beforePasses = passes.count
            val offset = listState!!.firstVisibleItemScrollOffset
            val anchor = host.vm.anchor.value.dayID
            scrollBy(composeRule.density.density)
            scrollBy(composeRule.density.density)
            settle()
            assertTrue("the list must move", listState!!.firstVisibleItemScrollOffset > offset)
            assertEquals(anchor, host.vm.anchor.value.dayID)
            assertEquals("scrolling within a day must schedule no composition", 0, passes.count - beforePasses)
            val delta = probe.snapshot().delta(before)
            assertEquals(
                0,
                delta.composed
                    .filterKeys {
                        it.kind in setOf(Kind.LIST_ROW, Kind.HEADER, Kind.PILLS, Kind.COMPACT, Kind.ANCHOR)
                    }.values
                    .sum(),
            )
        } finally {
            composeRule.runOnIdle { passes.close() }
        }
    }

    @Test fun minuteTickRecomposesOnlyTheSummaryClock() {
        start(summary = true)
        val before = probe.snapshot()
        host.clock.advance(host.dayStart(0).plusSeconds(60))
        settle()
        val delta = probe.snapshot().delta(before)
        assertEquals(1, delta.composed(Kind.SUMMARY_CLOCK, "summary"))
        assertNothingStaticMoved(before, delta, listOf(Kind.STATIC, Kind.HEADER, Kind.TARGETS))
        assertEquals(
            0,
            delta.composed
                .filterKeys { it.kind in setOf(Kind.PILLS, Kind.COMPACT, Kind.ANCHOR) }
                .values
                .sum(),
        )
        assertEquals(1, delta.composed(Kind.CLOCK, dayIds[0]))
        assertEquals(
            1,
            delta.composed
                .filterKeys { it.kind == Kind.CLOCK }
                .values
                .sum(),
        )
    }
}
