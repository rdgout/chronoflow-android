package net.chronoflow.feature.trip.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import net.chronoflow.core.plan.TimelineGeometry
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
@OptIn(ExperimentalTestApi::class)
class DayTimelineTest {
    private val composeRule = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(composeRule)

    private val opened = mutableListOf<String>()
    private var rig: DayTimelineRig? = null

    @After fun tearDown() {
        rig?.close()
    }

    private fun show(
        rig: DayTimelineRig,
        availableWidthDp: Double = 379.0,
    ) {
        this.rig = rig
        composeRule.setContent {
            ProfileHost(DeviceProfiles.pixel8) { Host(rig, availableWidthDp) }
        }
    }

    @Composable
    private fun Host(
        rig: DayTimelineRig,
        availableWidthDp: Double,
    ) {
        Box(Modifier.width(availableWidthDp.dp)) {
            DayTimeline(
                day = rig.day,
                layout = rig.layout,
                key = rig.key,
                dayIndex = 0,
                availableWidthDp = availableWidthDp,
                repository = rig.repository,
                resources = rig.resources,
                clock = rig.clock,
                onBlock = { opened += it },
                onFirstReplay = {},
                prefetch = { _, _ -> },
            )
        }
    }

    private fun tag(blockId: String) = "trip.block.$blockId"

    @Test fun placeholderThenReady() {
        val rig = DayTimelineRig(gated = true)
        show(rig)
        val first = rig.blockTargets.first()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("trip.timeline.preparing").assertExists()
        composeRule.onNodeWithTag(tag(checkNotNull(first.blockId))).assertDoesNotExist()
        rig.gate!!.complete(Unit)
        composeRule.waitUntilExactlyOneExists(hasTestTag("trip.timeline.ready"), 10_000)
        composeRule
            .onNodeWithTag(tag(checkNotNull(first.blockId)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test fun tapResolvesTile() {
        val rig = DayTimelineRig()
        show(rig)
        composeRule.waitUntilExactlyOneExists(hasTestTag("trip.timeline.ready"), 10_000)
        val target = rig.blockTargets.first()
        val resolved = rig.reference.hits.resolve((target.left + target.right) / 2, (target.top + target.bottom) / 2)
        assertNotNull(resolved)
        composeRule.onNodeWithTag("trip.day.${rig.day.id}").performTouchInput {
            click(Offset((target.left + target.right) / 2, (target.top + target.bottom) / 2))
        }
        composeRule.waitForIdle()
        assertEquals(listOf(resolved), opened)

        val empty = emptySpot(rig)
        composeRule.onNodeWithTag("trip.day.${rig.day.id}").performTouchInput { click(empty) }
        composeRule.waitForIdle()
        assertEquals(1, opened.size)
    }

    /** A point of the day that no tile, marker or zone change resolves; found on the fixture's hit index, not on the tree. */
    private fun emptySpot(rig: DayTimelineRig): Offset {
        val x = rig.reference.widthPx - 4f
        var y = 4f
        while (y < rig.reference.heightPx && rig.reference.hits.resolve(x, y) != null) y += 8f
        assertTrue("the fixture day has an empty strip at its right edge", y < 400 * TimelineTestPlans.DENSITY)
        return Offset(x, y)
    }

    @Test fun semanticsClickOpensBlock() {
        val rig = DayTimelineRig()
        show(rig)
        composeRule.waitUntilExactlyOneExists(hasTestTag("trip.timeline.ready"), 10_000)
        val id = checkNotNull(rig.blockTargets.first().blockId)
        composeRule.onNodeWithTag(tag(id)).performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(listOf(id), opened)
    }

    @Test fun targetsAreAtLeast48dp() {
        val rig = DayTimelineRig()
        show(rig)
        composeRule.waitUntilExactlyOneExists(hasTestTag("trip.timeline.ready"), 10_000)
        val dayBounds = composeRule.onNodeWithTag("trip.day.${rig.day.id}").getBoundsInRoot()
        for (target in rig.blockTargets) {
            val bounds = composeRule.onNodeWithTag(tag(checkNotNull(target.blockId))).getBoundsInRoot()
            assertTrue("${target.blockId} width ${bounds.width}", bounds.width >= 48.dp - 0.5.dp)
            assertTrue("${target.blockId} height ${bounds.height}", bounds.height >= 48.dp - 0.5.dp)
            assertTrue(bounds.left >= dayBounds.left - 0.5.dp && bounds.right <= dayBounds.right + 0.5.dp)
            assertTrue(bounds.top >= dayBounds.top - 0.5.dp && bounds.bottom <= dayBounds.bottom + 0.5.dp)
        }
    }

    @Test fun overflowScrollsHorizontally() {
        val rig = DayTimelineRig()
        assertTrue(
            "a 248 dp window overflows this day",
            TimelineGeometry(rig.layout, 248.0).width > 248.0,
        )
        show(rig, availableWidthDp = 248.0)
        composeRule.waitForIdle()
        composeRule
            .onNodeWithTag("trip.day.${rig.day.id}")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange))
    }

    @Test fun anAvailableWidthDoesNotScroll() {
        val rig = DayTimelineRig()
        show(rig)
        composeRule.waitForIdle()
        composeRule
            .onNodeWithTag("trip.day.${rig.day.id}")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.HorizontalScrollAxisRange))
    }
}
