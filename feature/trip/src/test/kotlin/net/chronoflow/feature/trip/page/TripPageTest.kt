package net.chronoflow.feature.trip.page

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.plan.planDate
import net.chronoflow.core.plan.trippage.PlanPreferences
import net.chronoflow.core.testing.LargeTimelineFixtures
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.timeline.CountingTimelineProbe
import net.chronoflow.feature.trip.timeline.LocalTimelineProbe
import net.chronoflow.feature.trip.timeline.TimelineProbe.Kind
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TripPageTest : PageComponentTest() {
    private val probe = CountingTimelineProbe()
    private var rig: TripPageTestRig? = null

    @After fun close() {
        rig?.close()
    }

    private fun start(
        day: Int = 33,
        fixture: String = "large60",
        summary: Boolean = false,
        past: Boolean = false,
    ): TripPageTestRig {
        val detail = LargeTimelineFixtures.fixture(fixture).detail
        val instant =
            if (past) {
                checkNotNull(
                    planDate(
                        detail.plan.days
                            .last()
                            .endUtc,
                    ),
                ).plusSeconds(3600)
            } else {
                checkNotNull(planDate(detail.plan.days[day].startUtc)).plusSeconds(7200)
            }
        val host = TripPageTestRig(fixture, instant, summary).also { rig = it }
        host.prewarm(compose.density)
        host { CompositionLocalProvider(LocalTimelineProbe provides probe) { host.Content() } }
        compose.waitForIdle()
        return host
    }

    private fun assertHeaderAtTop(id: String) {
        val item = compose.onNodeWithTag("trip.headerItem.$id").getUnclippedBoundsInRoot()
        val list = compose.onNodeWithTag("trip.scroll").getUnclippedBoundsInRoot()
        assertTrue("header item starts at viewport", abs(item.top.value - list.top.value) <= 1f)
    }

    @Test fun opensOnTheDayContainingNow() {
        val host = start()
        assertHeaderAtTop(
            host.model.prepared.days[33]
                .id,
        )
        compose.onNodeWithTag("trip.day.${host.model.prepared.days[32].id}").assertDoesNotExist()
    }

    @Test fun pastTripOpensOnLastDay() {
        val host = start(day = 0, fixture = "S3", past = true)
        assertHeaderAtTop(
            host.model.prepared.days
                .last()
                .id,
        )
    }

    @Test fun summarySeedOpensAtTheSummary() {
        start(day = 0, summary = true)
        assertEquals(
            compose.onNodeWithTag("trip.scroll").getUnclippedBoundsInRoot().top,
            compose.onNodeWithTag("trip.summary").getUnclippedBoundsInRoot().top,
        )
        compose.onNodeWithTag("trip.compactHeader").onChildren().assertCountEquals(0)
    }

    @Test fun menuJumpReachesTheEnd() {
        val host = start()
        compose.onNodeWithTag("trip.dayMenu").performClick()
        compose.onNodeWithTag("trip.dayMenu.day.59").performScrollTo().performClick()
        assertHeaderAtTop(
            host.model.prepared.days[59]
                .id,
        )
        compose.onNodeWithTag("trip.scroll").performScrollToNode(hasTestTag("trip.plan.end"))
        compose.onNodeWithTag("trip.plan.end").assertExists()
    }

    @Test fun pillJumpSelectsDay() {
        val host = start(day = 0, summary = true)
        compose.onNodeWithTag("trip.days.next").performClick()
        assertEquals(
            host.model.prepared.days[1]
                .id,
            host.vm.anchor.value.dayID,
        )
        assertHeaderAtTop(
            host.model.prepared.days[1]
                .id,
        )
    }

    @Test fun compactHeaderFadesIn() {
        val host = start()
        val id = host.vm.anchor.value.dayID!!
        val header = host.model.index.headerIndex(id)
        val visible =
            host.listState!!
                .layoutInfo.visibleItemsInfo
                .first { it.index == header }
        compose.runOnIdle { host.listState!!.dispatchRawDelta(visible.size - with(compose.density) { 12f * density }) }
        compose.waitForIdle()
        compose.onNodeWithTag("trip.compactHeader").onChildren().assertCountEquals(0)
        compose.runOnIdle { host.listState!!.dispatchRawDelta(with(compose.density) { 13f * density }) }
        compose.waitForIdle()
        compose.onNodeWithTag("trip.compactHeader").onChildren().assertCountEquals(1)
    }

    @Test fun compactHeaderSemanticsFollowTheAnchor() {
        val host = start(day = 0)
        compose.onNodeWithTag("trip.dayMenu").performClick()
        compose.onNodeWithTag("trip.dayMenu.day.2").performClick()
        assertEquals(
            host.model.prepared.days[2]
                .id,
            host.vm.anchor.value.dayID,
        )
        compose.onNodeWithTag("trip.compactHeader").onChildren().assertCountEquals(0)
        val header = host.model.index.headerIndex(host.vm.anchor.value.dayID!!)
        val item =
            host.listState!!
                .layoutInfo.visibleItemsInfo
                .first { it.index == header }
        compose.runOnIdle { host.listState!!.dispatchRawDelta(item.size + 1f) }
        compose.waitForIdle()
        compose.onNodeWithTag("trip.compactHeader").onChildren().assertCountEquals(1)
    }

    @Test fun anchorFollowsTheCurrentDay() {
        val host = start()
        val day = host.model.prepared.days[34]
        compose.runOnIdle { host.listState!!.requestScrollToItem(host.model.index.headerIndex(day.id)) }
        compose.waitForIdle()
        assertEquals(day.id, host.vm.anchor.value.dayID)
    }

    @Test fun subtitleSwitchesWhenTheHeaderScrolls() {
        val host = start()
        val before = probe.snapshot()
        compose.runOnIdle { host.listState!!.dispatchRawDelta(1f) }
        compose.waitForIdle()
        val delta = probe.snapshot().delta(before)
        assertEquals(1, delta.composed(Kind.ANCHOR, host.vm.anchor.value.dayID!!))
    }

    @Test fun endCardAndHiddenSteps() {
        val host = start()
        compose.runOnIdle {
            host.source.settings.value =
                PlanPreferences(hiddenCategories = listOf(BlockType.CaffeineServing))
        }
        compose.waitForIdle()
        compose.runOnIdle { host.listState!!.requestScrollToItem(host.model.index.hidden!!) }
        compose.waitForIdle()
        compose.onNodeWithTag("trip.hiddenSteps").assertExists()
        compose.runOnIdle { host.source.settings.value = PlanPreferences() }
        compose.waitForIdle()
        compose.onNodeWithTag("trip.hiddenSteps").assertDoesNotExist()
    }

    @Test fun replacementKeepsTheAnchor() {
        val host = start()
        val old = host.vm.anchor.value.dayID
        val detail = host.model.detail
        compose.runOnIdle {
            host.replace(
                detail.copy(
                    plan =
                        detail.plan.copy(
                            days =
                                detail.plan.days.map {
                                    if (it.id == old) it.copy(id = "renamed") else it
                                },
                        ),
                ),
            )
        }
        compose.waitForIdle()
        assertHeaderAtTop("renamed")
    }
}
