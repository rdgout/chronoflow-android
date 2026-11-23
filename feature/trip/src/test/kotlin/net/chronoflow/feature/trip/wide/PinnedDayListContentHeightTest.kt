package net.chronoflow.feature.trip.wide

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.config.AppLanguage
import net.chronoflow.core.plan.generated.PlanMetrics
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.timeline.CountingTimelineProbe
import net.chronoflow.feature.trip.timeline.LocalTimelineProbe
import net.chronoflow.feature.trip.timeline.TimelineProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PinnedDayListContentHeightTest {
    private val compose = createComposeRule()
    private val profile = DeviceProfiles.foldInner.copy(fontScale = DeviceProfiles.largeFont.fontScale)

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile, AppLanguage.Nl)).around(compose)

    @Test fun growingRowKeepsTheLastRowInsideTheListViewport() {
        val grown = mutableStateOf(false)
        val probe = CountingTimelineProbe()
        val rows: @Composable ColumnScope.() -> Unit = {
            GrowingRow { grown.value }
            Box(
                Modifier.fillMaxWidth().height(PlanMetrics.tinyTileHeight.dp).testTag("pin.lastRow"),
            )
        }
        compose.setContent {
            ProfileHost(profile) {
                ChronoFlowTheme {
                    CompositionLocalProvider(LocalTimelineProbe provides probe) {
                        val listState = rememberLazyListState()
                        val maxHeight = with(LocalDensity.current) { PlanMetrics.wideListMaxHeight.dp.roundToPx() }
                        val pin =
                            remember(listState, maxHeight) {
                                WidePinSource(listState).apply {
                                    maxHeightPx =
                                        maxHeight
                                }
                            }
                        LazyColumn(Modifier.fillMaxSize(), state = listState) {
                            item("t:growing-day", "dayTimeline") {
                                WideDayRow(
                                    "growing-day",
                                    TripPageFrame.resolve(profile.widthDp.toDouble(), profile.widthDp.toDouble()),
                                    timeline = { Box(Modifier.height(PlanMetrics.wideListMaxHeight.dp)) },
                                    list = { PinnedDayList("growing-day", rows, pin) },
                                )
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        val viewportTag = "trip.wide.list.growing-day"
        val before = compose.onNodeWithTag(viewportTag).getUnclippedBoundsInRoot()
        val lastBefore = compose.onNodeWithTag("pin.lastRow").getUnclippedBoundsInRoot()
        assertEquals(1.3f, compose.density.fontScale, 0.001f)
        assertEquals(PlanMetrics.wideListWidth.toFloat(), before.width.value, 1f / compose.density.density)
        assertTrue(lastBefore.bottom <= before.bottom)
        val counts = probe.snapshot()

        compose.runOnIdle { grown.value = true }
        compose.waitForIdle()
        val viewport = compose.onNodeWithTag(viewportTag).getUnclippedBoundsInRoot()
        val last = compose.onNodeWithTag("pin.lastRow").getUnclippedBoundsInRoot()
        assertEquals(0, probe.snapshot().delta(counts).composed(TimelineProbe.Kind.LIST, "growing-day"))
        assertEquals(before.width, viewport.width)
        assertTrue("the child row really grew", last.bottom > lastBefore.bottom)
        assertTrue(
            "the grown content still fits below the height cap",
            last.bottom - viewport.top < PlanMetrics.wideListMaxHeight.dp,
        )
        assertTrue(
            "the last row must fit in the resized list viewport: $last / $viewport",
            last.bottom <= viewport.bottom,
        )
        assertTrue(viewport.height > before.height)
    }
}

@Composable
private fun GrowingRow(grown: () -> Boolean) {
    val scale = LocalDensity.current.fontScale
    val height = PlanMetrics.tinyTileHeight.dp * scale * if (grown()) 2 else 1
    Box(Modifier.fillMaxWidth().height(height).testTag("pin.growingRow"))
}
