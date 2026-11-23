@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)

package net.chronoflow.feature.trip.wide

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.navigation.TripSheet
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.WindowSizeFake
import net.chronoflow.core.testing.compose.WindowSizeFakeHost
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.page.TripPageTestRig
import net.chronoflow.feature.trip.step.TripPageSheets
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1000dp-h1100dp-port-420dpi")
class TripPageResizeTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain =
        RuleChain
            .outerRule(
                ProfileRule(
                    DeviceProfiles.foldInner.copy(widthDp = 1000, heightDp = 1100),
                ),
            ).around(compose)
    private lateinit var rig: TripPageTestRig
    private val window = WindowSizeFake(DpSize(411.dp, 914.dp))
    private var density by mutableFloatStateOf(2.625f)
    private val day get() = rig.model.prepared.days[0]
    private val block get() = day.blocks.first().id

    private fun start(
        list: Boolean = false,
        summary: Boolean = false,
        fixture: String = "S3",
    ) {
        val template =
            net.chronoflow.core.testing.LargeTimelineFixtures
                .fixture(fixture)
        val snapshot =
            net.chronoflow.feature.trip.page.FakeTripPageSource
                .fixture(fixture)
                .snapshot.value
        val trip =
            checkNotNull(snapshot.trip).copy(
                card =
                    net.chronoflow.core.model.api.ApiJson.response
                        .encodeToString(
                            net.chronoflow.core.model.contracts.TripCardView
                                .serializer(),
                            template.card,
                        ),
            )
        rig =
            TripPageTestRig(
                fixture,
                initialTimeline = !list,
                startAtSummary = summary,
                initialSnapshot = snapshot.copy(trip = trip),
            )
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density, 1f)) {
                WindowSizeFakeHost(window) {
                    ChronoFlowTheme(reduceMotion = true) {
                        rig.Content()
                        TripPageSheets(rig.vm)
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @After fun close() {
        if (::rig.isInitialized) rig.close()
    }

    private fun top(tag: String) =
        compose
            .onNodeWithTag(tag)
            .fetchSemanticsNode()
            .boundsInRoot.top

    private fun dayTop() = top("trip.day.${day.id}") - top("trip.scroll")

    private fun scroll180() {
        compose
            .onNodeWithTag(
                "trip.scroll",
            ).performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 180f * density) }
        compose.waitForIdle()
    }

    private fun resize(width: Int) {
        compose.runOnIdle { window.size = DpSize(width.dp, 914.dp) }
        compose.waitForIdle()
    }

    private fun savedOffset(): Double = checkNotNull(rig.savedState.get<Double>("trip.anchor.offset"))

    private fun tapFirstTile(wide: Boolean) {
        val parent = if (wide) "trip.wide.timeline.${day.id}" else "trip.day.${day.id}"
        val target =
            compose
                .onNode(hasTestTag("trip.block.$block") and hasAnyAncestor(hasTestTag(parent)))
                .fetchSemanticsNode()
                .boundsInRoot
        val row = compose.onNodeWithTag("trip.day.${day.id}").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("trip.day.${day.id}").performTouchInput {
            click(Offset(target.center.x - row.left, target.center.y - row.top))
        }
        compose.waitForIdle()
        assertEquals(TripSheet.Step(block), rig.vm.sheet.value)
    }

    @Test fun sizeChangeKeepsDayScrollAndPresentedStep() {
        start()
        scroll180()
        val id = rig.vm.anchor.value.dayID
        val before = savedOffset()
        assertTrue(before > 0)
        val y = dayTop()
        val scrollId = compose.onNodeWithTag("trip.scroll").fetchSemanticsNode().id
        compose.runOnIdle { rig.vm.openStep(block) }
        compose.waitUntilExactlyOneExists(hasTestTag("step.sheet"), 2_000)
        resize(841)
        compose.onNodeWithTag("trip.wide.list.${day.id}").assertExists()
        assertEquals(id, rig.vm.anchor.value.dayID)
        assertEquals(before, savedOffset(), 1.0)
        assertEquals(y, dayTop(), 1f)
        assertEquals(TripSheet.Step(block), rig.vm.sheet.value)
        compose.onNodeWithTag("step.sheet").assertExists()
        assertEquals(scrollId, compose.onNodeWithTag("trip.scroll").fetchSemanticsNode().id)
        resize(411)
        compose.onNodeWithTag("trip.wide.list.${day.id}").assertDoesNotExist()
        assertEquals(id, rig.vm.anchor.value.dayID)
        assertEquals(before, savedOffset(), 1.0)
        assertEquals(y, dayTop(), 1f)
        assertEquals(TripSheet.Step(block), rig.vm.sheet.value)
        compose.onNodeWithTag("step.sheet").assertExists()
        assertEquals(scrollId, compose.onNodeWithTag("trip.scroll").fetchSemanticsNode().id)
    }

    @Test fun timelineKeepsItsIdentityAcrossTheBreakpoint() {
        start()
        val id = compose.onNodeWithTag("trip.day.${day.id}").fetchSemanticsNode().id
        tapFirstTile(false)
        compose.runOnIdle { rig.vm.dismissSheet() }
        compose.waitForIdle()
        resize(841)
        assertEquals(id, compose.onNodeWithTag("trip.day.${day.id}").fetchSemanticsNode().id)
        tapFirstTile(true)
        compose.runOnIdle { rig.vm.dismissSheet() }
        compose.waitForIdle()
        resize(411)
        assertEquals(id, compose.onNodeWithTag("trip.day.${day.id}").fetchSemanticsNode().id)
        tapFirstTile(false)
    }

    @Test fun listModeToWideLandsOnTheDayTop() {
        start(list = true)
        scroll180()
        val id = checkNotNull(rig.vm.anchor.value.dayID)
        resize(841)
        assertEquals(top("trip.scroll"), top("trip.headerItem.$id"), 1f)
        compose.onNodeWithTag("trip.mode.timeline").assertExists()
        assertEquals(false, rig.vm.timeline.value)
        resize(411)
        assertEquals(top("trip.scroll"), top("trip.headerItem.$id"), 1f)
        compose.onNodeWithTag("trip.mode.list").assertExists()
        compose.onNodeWithTag("trip.viewToggle").assertExists()
    }

    @Test fun listModeUnfoldThenJumpUsesTheWideTimelineIndex() {
        start(list = true, fixture = "S1")
        resize(841)
        val target =
            rig.model.prepared.days[1]
                .id
        assertTrue(rig.model.listIndex.headerIndex(target) != rig.model.index.headerIndex(target))
        compose.runOnIdle { rig.vm.jump(target) }
        compose.waitForIdle()
        assertEquals(top("trip.scroll"), top("trip.headerItem.$target"), 1f)
    }

    @Test fun wideFoldThenToggleToListAndJumpUsesTheListIndex() {
        window.size = DpSize(841.dp, 914.dp)
        start(fixture = "S1")
        resize(411)
        compose.onNodeWithTag("trip.viewToggle").performClick()
        compose.waitForIdle()
        val target =
            rig.model.prepared.days[1]
                .id
        assertTrue(rig.model.listIndex.headerIndex(target) != rig.model.index.headerIndex(target))
        compose.runOnIdle { rig.vm.jump(target) }
        compose.waitForIdle()
        assertEquals(top("trip.scroll"), top("trip.headerItem.$target"), 1f)
    }

    @Test fun legendAndTripActionsSurviveResize() {
        start()
        compose.runOnIdle { rig.vm.openLegend() }
        compose.waitForIdle()
        resize(841)
        compose.onNodeWithTag("legend.priorities").assertExists()
        resize(411)
        compose.onNodeWithTag("legend.priorities").assertExists()
        compose.runOnIdle {
            rig.vm.dismissSheet()
            rig.vm.openTripActions()
        }
        compose.waitForIdle()
        resize(841)
        compose.onNodeWithTag("trip.actions").assertExists()
        resize(411)
        compose.onNodeWithTag("trip.actions").assertExists()
    }

    @Test fun densityChangeKeepsTheDayTop() {
        start()
        resize(841)
        scroll180()
        val before = dayTop() / density
        compose.runOnIdle { density = 3f }
        compose.waitForIdle()
        assertEquals(before, dayTop() / density, 1f / density)
    }

    @Test fun negativeHeaderOffsetIsAcceptedDuringResize() {
        start()
        compose.runOnIdle { rig.listState!!.requestScrollToItem(rig.model.index.headerIndex(day.id), -20) }
        compose.waitForIdle()
        val before = top("trip.headerItem.${day.id}") - top("trip.scroll")
        assertEquals(20f, before, 1f)
        resize(841)
        assertEquals(before, top("trip.headerItem.${day.id}") - top("trip.scroll"), 1f)
    }

    @Test fun summaryPositionIsNotRestoredAsADay() {
        start(summary = true)
        resize(841)
        assertEquals(0, rig.listState!!.firstVisibleItemIndex)
        assertTrue(abs(top("trip.summary") - top("trip.scroll")) <= 1f)
    }
}
