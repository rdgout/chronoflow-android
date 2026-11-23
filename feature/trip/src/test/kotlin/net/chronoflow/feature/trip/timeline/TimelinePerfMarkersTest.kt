package net.chronoflow.feature.trip.timeline

import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import kotlinx.coroutines.CompletableDeferred
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.FakeTextMetrics
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.trip.timeline.TimelineProbe.Kind
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.TimeUnit

/** The invisible test hooks of the perf benchmarks: `trip.timeline.opening`, `trip.timeline.presented`, `trip.day.<id>`. */
@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TimelinePerfMarkersTest {
    private val composeRule = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(composeRule)

    private val gate = CompletableDeferred<Unit>()
    private val probe = CountingTimelineProbe()
    private val host =
        TimelineTestHost(
            metrics = {
                gate.await()
                FakeTextMetrics()
            },
        )

    @After fun close() {
        host.close()
    }

    private fun settle() {
        composeRule.waitForIdle()
        composeRule.onRoot().captureToImage()
    }

    @Test fun theMarkerFlipsTwoFramesAfterTheFirstRealReplay() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalTimelineProbe provides probe) {
                ProfileHost(DeviceProfiles.pixel8) { ChronoFlowTheme { host.Content() } }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        settle()
        composeRule.onNodeWithTag("trip.timeline.opening").assertExists()
        composeRule.onNodeWithTag("trip.timeline.presented").assertDoesNotExist()
        composeRule.onNodeWithTag("trip.timeline.preparing").assertExists()
        val openingDay = host.vm.anchor.value.dayID!!
        assertTrue("the placeholder drew", probe.snapshot().drew(Kind.STATIC, openingDay) >= 1)

        gate.complete(Unit)
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeByFrame()
        settle()
        repeat(2) { shadowOf(Looper.getMainLooper()).idleFor(16, TimeUnit.MILLISECONDS) }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("trip.timeline.presented").assertExists()
        composeRule.onNodeWithTag("trip.timeline.opening").assertDoesNotExist()
        composeRule.onNodeWithTag("trip.day.$openingDay").assertExists()
    }
}
