package net.chronoflow.app.perf

import androidx.compose.ui.unit.Density
import net.chronoflow.feature.launch.model.LaunchFrame
import net.chronoflow.feature.launch.model.LaunchFrameSampler
import net.chronoflow.feature.launch.model.LaunchSize
import net.chronoflow.feature.launch.model.LaunchTimeline
import net.chronoflow.feature.launch.model.MoonTarget
import net.chronoflow.feature.launch.model.withReadyExit
import net.chronoflow.feature.launch.model.withStatus
import net.chronoflow.feature.launch.model.withWelcomeExit
import net.chronoflow.feature.launch.welcome.WelcomeMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LaunchModeTimelineTests {
    @Test fun testModeTimelinesAndAsymmetricSafeAreaFallback() {
        val opening = LaunchTimeline.opening(LaunchSize(570.0, 730.0), false)
        val safe = MoonTarget.Insets(23.0, 31.0, 29.0, 47.0)
        val welcome = LaunchDiagnosticsParser.overrides(mapOf("cf.mode" to "welcome"))
        assertEquals(
            opening
                .withWelcomeExit(
                    1600.milliseconds,
                    opening.size,
                    MoonTarget.fallback(opening.size, safe, 28.0),
                ).tracks,
            welcome.timeline(opening, safe).tracks,
        )
        val ready = LaunchDiagnosticsParser.overrides(mapOf("cf.mode" to "ready"))
        val slow = LaunchDiagnosticsParser.overrides(mapOf("cf.mode" to "slow"))
        assertEquals(opening.withReadyExit(1500.milliseconds).tracks, ready.timeline(opening).tracks)
        assertEquals(
            opening.withStatus(2.seconds).withReadyExit(4600.milliseconds).tracks,
            slow.timeline(opening).tracks,
        )
    }

    @Test fun testWelcomeFallbackUsesTheCurrentTextSizeAndAllSafeInsets() {
        val normal = WelcomeMetrics.fallbackRowHeight(Density(3f, 1f))
        val larger = WelcomeMetrics.fallbackRowHeight(Density(3f, 1.3f))
        assertEquals(28.0, normal, 0.0)
        assertTrue(larger > normal)
        val size = LaunchSize(570.0, 730.0)
        val safe = MoonTarget.Insets(31.0, 37.0, 27.0, 59.0)
        val timeline =
            LaunchDiagnosticsParser
                .overrides(
                    mapOf("cf.mode" to "welcome"),
                ).timeline(LaunchTimeline.opening(size, false), safe, larger)
        val frame = LaunchFrame()
        LaunchFrameSampler(timeline).sampleInto(2.3, frame)
        assertEquals(37.0 + 24 + 14, frame.moonX, 1e-9)
        assertEquals(maxOf(31.0 + 16, 64.0) + larger / 2, frame.moonY, 1e-9)
    }
}
