package net.chronoflow.feature.trip.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.model.contracts.CheckStatus
import net.chronoflow.core.plan.trippage.NowLineValue
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.page.PageComponentTest
import net.chronoflow.feature.trip.timeline.DayClock
import net.chronoflow.feature.trip.timeline.LocalTimelineProbe
import net.chronoflow.feature.trip.timeline.TimelineProbe
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
class DayListRowTest : PageComponentTest() {
    @Test fun labelAndTagFollowPriority() {
        val row = ListTestRows.row
        var selected: String? = null
        var calls = 0
        host {
            DayListRow(row, null, {
                selected = it
                calls++
            })
        }
        compose
            .onNodeWithTag("trip.block.${row.blockId}")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
            .performClick()
        assertEquals(row.blockId, selected)
        assertEquals(1, calls)
        compose.onNodeWithTag("trip.block.${row.blockId}.priority", useUnmergedTree = true).assertExists()
    }

    @Test fun runningRowSaysNow() {
        val row = ListTestRows.row
        val clock = DayClock().also { it.value.value = NowLineValue(0.0, "", row.startMs) }
        var compositions = 0
        val probe =
            object : TimelineProbe {
                override fun composed(
                    kind: TimelineProbe.Kind,
                    dayId: String,
                ) {
                    if (kind ==
                        TimelineProbe.Kind.LIST_ROW
                    ) {
                        compositions++
                    }
                }

                override fun drew(
                    kind: TimelineProbe.Kind,
                    dayId: String,
                ) = Unit
            }
        host { CompositionLocalProvider(LocalTimelineProbe provides probe) { DayListRow(row, clock, {}) } }
        compose.waitForIdle()
        val before = compositions
        compose.runOnIdle { clock.value.value = NowLineValue(0.0, "", row.endMs) }
        compose.waitForIdle()
        assertEquals(before + 1, compositions)
        compose.onNodeWithTag("trip.block.${row.blockId}.interval", useUnmergedTree = true).assertExists()
    }

    @Test fun markedRowIsDimmedAndShowsIcon() {
        val row = ListTestRows.row.copy(status = CheckStatus.Done)
        host { Column(Modifier.background(CfTheme.colors.background)) { DayListRow(row, null, {}) } }
        compose.onNodeWithTag("trip.block.${row.blockId}.status", useUnmergedTree = true).assertExists()
        val pixels = compose.onNodeWithTag("trip.block.${row.blockId}").captureToImage().toPixelMap()
        val actual = pixels[pixels.width / 2, 6]
        val expected = CfLightColors.card.copy(alpha = 0.7f).compositeOver(CfLightColors.background)
        assertTrue(
            "marked fill $actual differs from $expected",
            abs(actual.red - expected.red) < 0.01f && abs(actual.green - expected.green) < 0.01f &&
                abs(actual.blue - expected.blue) < 0.01f,
        )
    }

    @Test fun trailingStacksWhenNarrow() {
        val row =
            ListTestRows.row.copy(
                title = "Ochtendlichtblootstelling",
                priorityTag = "Belangrijk",
                status = CheckStatus.Done,
            )
        val narrow = row.copy(blockId = "narrow")
        val wide = row.copy(blockId = "wide")
        host {
            Column {
                Box(Modifier.width(280.dp)) { DayListRow(narrow, null, {}) }
                Box(Modifier.width(411.dp)) { DayListRow(wide, null, {}) }
            }
        }
        val tag = compose.onNodeWithTag("trip.block.narrow.priority", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val icon = compose.onNodeWithTag("trip.block.narrow.status", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("narrow trailing must stack", tag.bottom <= icon.top)
        val wideTag =
            compose
                .onNodeWithTag(
                    "trip.block.wide.priority",
                    useUnmergedTree = true,
                ).getUnclippedBoundsInRoot()
        val wideIcon =
            compose
                .onNodeWithTag(
                    "trip.block.wide.status",
                    useUnmergedTree = true,
                ).getUnclippedBoundsInRoot()
        assertEquals(
            (wideTag.top.value + wideTag.bottom.value) / 2,
            (wideIcon.top.value + wideIcon.bottom.value) / 2,
            0.1f,
        )
    }

    @Test fun touchTargetAtLeast48() {
        val row = ListTestRows.row
        host { DayListRow(row, null, {}) }
        compose.onNodeWithTag("trip.block.${row.blockId}").assertHeightIsAtLeast(48.dp)
    }
}
