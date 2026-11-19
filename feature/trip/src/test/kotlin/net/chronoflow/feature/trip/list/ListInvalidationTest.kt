package net.chronoflow.feature.trip.list

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.model.store.CheckMarkRecord
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.page.TripPageTestRig
import net.chronoflow.feature.trip.timeline.CompositionPassProbe
import net.chronoflow.feature.trip.timeline.CountingTimelineProbe
import net.chronoflow.feature.trip.timeline.LocalTimelineProbe
import net.chronoflow.feature.trip.timeline.TimelineProbe.Kind
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
class ListInvalidationTest(
    private val scenario: String,
    private val step: Int,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private val probe = CountingTimelineProbe()
    private lateinit var rig: TripPageTestRig

    @After fun close() {
        if (::rig.isInitialized) rig.close()
    }

    private fun start() {
        rig = TripPageTestRig(initialTimeline = false)
        compose.setContent {
            net.chronoflow.core.testing.compose.ProfileHost(DeviceProfiles.pixel8) {
                net.chronoflow.core.designsystem.theme.ChronoFlowTheme(reduceMotion = true) {
                    CompositionLocalProvider(LocalTimelineProbe provides probe) { rig.Content() }
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun invalidation() {
        start()
        when (scenario) {
            "scrollRecomposesNoComposedRow" -> scroll()
            "minuteTickWithinARowRecomposesNothing" -> tick(false)
            "tickAcrossABoundaryRecomposesTheTwoRows" -> tick(true)
            "checkOffRecomposesOnlyThatRow" -> checkOff()
            "scrollingWithinOneDayDoesNotRecomposePageChrome" -> chrome()
            else -> error("unknown invalidation scenario")
        }
    }

    private fun scroll() {
        val header =
            rig.model.listIndex.headerIndex(
                rig.model.prepared.days[0]
                    .id,
            )
        compose.runOnIdle { rig.listState!!.requestScrollToItem(header, step * 37) }
        compose.waitForIdle()
        val visibleBefore =
            rig.listState!!
                .layoutInfo.visibleItemsInfo
                .map { it.key }
                .toSet()
        val before = probe.snapshot()
        compose.runOnIdle { rig.listState!!.dispatchRawDelta(37f) }
        compose.waitForIdle()
        val visibleAfter =
            rig.listState!!
                .layoutInfo.visibleItemsInfo
                .map { it.key }
                .toSet()
        val retained = visibleBefore intersect visibleAfter
        assertTrue("the measured scroll must retain rows", retained.any { it.toString().startsWith("r:") })
        val changed = probe.snapshot().delta(before).composedIds(Kind.LIST_ROW)
        assertTrue(
            "scroll recomposed retained rows: ${changed intersect retained}",
            (changed intersect retained).isEmpty(),
        )
    }

    private fun tick(boundary: Boolean) {
        val rows =
            rig.model.listRows[0]
                .rows
                .filter { !it.zone && it.startMs < it.endMs }
        val ending =
            rows.first { candidate ->
                rows.any {
                    it.startMs == candidate.endMs &&
                        it.blockId != candidate.blockId
                }
            }
        val at = if (boundary) ending.endMs else ending.startMs + 120_000
        val beforeAt = at - 60_000
        val index = rig.model.listIndex.rowIndex(ending.key)
        compose.runOnIdle { rig.listState!!.requestScrollToItem(index) }
        rig.clock.advance(Instant.ofEpochMilli(beforeAt))
        compose.waitForIdle()
        val before = probe.snapshot()
        val composed = before.composedIds(Kind.LIST_ROW)
        val expected =
            rig.model.listRows
                .flatMap { it.rows }
                .filter { row ->
                    row.key in composed &&
                        (row.startMs <= beforeAt && beforeAt < row.endMs) != (row.startMs <= at && at < row.endMs)
                }.map { it.key }
                .toSet()
        if (boundary) {
            assertEquals("the fixture must exercise both boundary rows", 2, expected.size)
        } else {
            assertTrue("the tick must stay inside the rows", expected.isEmpty())
        }
        rig.clock.advance(Instant.ofEpochMilli(at))
        compose.waitForIdle()
        val delta = probe.snapshot().delta(before)
        assertEquals(expected, delta.composedIds(Kind.LIST_ROW))
        assertTrue(
            delta.composed
                .filterKeys { it.kind == Kind.LIST_ROW }
                .values
                .all { it == 1 },
        )
    }

    private fun checkOff() {
        val day = rig.model.prepared.days[0]
        val block =
            day.blocks.first {
                it.block.actionable && it.block.checkKey != null && !it.continuesBefore &&
                    !it.continuesAfter
            }
        val row =
            rig.model.listRows[0]
                .rows
                .first { it.blockId == block.id }
        compose.runOnIdle { rig.listState!!.requestScrollToItem(rig.model.listIndex.rowIndex(row.key)) }
        compose.waitForIdle()
        val before = probe.snapshot()
        val key = block.block.checkKey!!
        val snapshot = rig.source.snapshot.value
        rig.source.snapshot.value =
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
        compose.waitForIdle()
        assertEquals(setOf(row.key), probe.snapshot().delta(before).composedIds(Kind.LIST_ROW))
    }

    private fun chrome() {
        val row = rig.model.listRows[0].rows[1]
        compose.runOnIdle { rig.listState!!.requestScrollToItem(rig.model.listIndex.rowIndex(row.key), 1) }
        compose.waitForIdle()
        val passes = compose.runOnIdle { CompositionPassProbe() }
        try {
            val before = probe.snapshot()
            val beforePasses = passes.count
            val offset = rig.listState!!.firstVisibleItemScrollOffset
            compose.runOnIdle { rig.listState!!.dispatchRawDelta(1f) }
            compose.waitForIdle()
            assertTrue(rig.listState!!.firstVisibleItemScrollOffset > offset)
            assertEquals(0, passes.count - beforePasses)
            assertTrue(
                probe
                    .snapshot()
                    .delta(before)
                    .composed
                    .isEmpty(),
            )
        } finally {
            compose.runOnIdle { passes.close() }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> =
            (0 until 120).map { arrayOf<Any>("scrollRecomposesNoComposedRow", it) } +
                listOf(
                    arrayOf("minuteTickWithinARowRecomposesNothing", 0),
                    arrayOf("tickAcrossABoundaryRecomposesTheTwoRows", 0),
                    arrayOf("checkOffRecomposesOnlyThatRow", 0),
                    arrayOf("scrollingWithinOneDayDoesNotRecomposePageChrome", 0),
                )
    }
}
