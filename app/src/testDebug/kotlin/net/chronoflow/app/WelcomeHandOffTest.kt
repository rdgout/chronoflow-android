package net.chronoflow.app

import android.app.Application
import androidx.activity.FullyDrawnReporter
import androidx.activity.FullyDrawnReporterOwner
import androidx.activity.compose.LocalFullyDrawnReporterOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import net.chronoflow.app.perf.StubReadiness
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.LaunchController
import net.chronoflow.feature.launch.model.LaunchFrame
import net.chronoflow.feature.launch.model.ManualLaunchClock
import net.chronoflow.feature.launch.welcome.WelcomeLaunchDiagnostics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.Executor
import kotlin.time.Duration.Companion.milliseconds

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WelcomeHandOffTest(
    private val profile: DeviceProfile,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(compose)
    private val clock = ManualLaunchClock()
    private val frames = Channel<Long>(Channel.CONFLATED)
    private var reports = 0
    private val reporterOwner =
        object : FullyDrawnReporterOwner {
            override val fullyDrawnReporter = FullyDrawnReporter(Executor { it.run() }) { reports++ }
        }

    @Test fun realWelcomePublishesAnAnchorToTheCoordinator() {
        val launch = mount()
        advanceTo(launch, 1600)
        assertNotNull(launch.moonAnchorStore.frame)
        val target = requireNotNull(launch.moonAnchorStore.frame)
        assertEquals(target, WelcomeLaunchDiagnostics.measuredMoonTarget(launch))
        val landing = LaunchFrame()
        launch.handOff.sampler.sampleInto(2.3, landing)
        assertEquals(target.midX, landing.moonX, 0.5)
        assertEquals(target.midY, landing.moonY, 0.5)
        val root = compose.onNodeWithTag("launch.opening").fetchSemanticsNode()
        assertEquals(Offset.Zero, root.boundsInWindow.topLeft)
    }

    @Test fun measuredWelcomeLandingAtDefaultAndLargeFont() {
        val launch = mount()
        advanceTo(launch, 2300)
        val target = requireNotNull(launch.moonAnchorStore.frame)
        val frame = LaunchFrame()
        launch.handOff.sampler.sampleInto(2.3, frame)
        assertEquals(target.midX, frame.moonX, 0.5)
        assertEquals(target.midY, frame.moonY, 0.5)
        assertEquals(target.width / 104, frame.moonScale, 1e-6)
    }

    @Test fun teardownMakesWelcomeAccessibleAndReportsFullyDrawnOnce() {
        val launch = mount()
        advanceTo(launch, 2500)
        assertTrue(hidden(compose.onNodeWithTag("welcome.wordmark", useUnmergedTree = true).fetchSemanticsNode()))
        assertEquals(0, reports)
        advanceTo(launch, 2620)
        assertFalse(hidden(compose.onNodeWithTag("welcome.wordmark", useUnmergedTree = true).fetchSemanticsNode()))
        compose.onNodeWithTag("launch.opening").assertDoesNotExist()
        assertEquals(1, reports)
        advanceTo(launch, 3300)
        assertEquals(1, reports)
    }

    @Test fun fontGateHoldsWelcomeUntilFontsAreReady() {
        val launch = mount(fonts = false)
        advanceTo(launch, 1000)
        compose.onNodeWithTag("welcome.wordmark").assertDoesNotExist()
        advanceTo(launch, 1540)
        compose.onNodeWithTag("welcome.wordmark").assertExists()
        assertTrue(launch.fontsReady)
    }

    private fun mount(fonts: Boolean = true): LaunchController {
        compose.mainClock.autoAdvance = false
        val app = ApplicationProvider.getApplicationContext<Application>()
        val launch =
            LaunchController(
                moon = { ImageBitmap.imageResource(it, R.drawable.launch_moon) },
                nanoTime = { compose.mainClock.currentTime * 1_000_000 },
                nextFrameNanos = { frames.receive() },
                clockFactory = { clock },
                readinessFactory = { shared ->
                    object : net.chronoflow.core.model.launch.LaunchReadiness by StubReadiness(
                        StubConfig(session = StubSession.SignedOut),
                        shared,
                    ) {
                        override suspend fun session(): net.chronoflow.core.model.session.SessionPresence {
                            shared.sleepUntil(10.milliseconds)
                            return net.chronoflow.core.model.session.SessionPresence.SignedOut
                        }
                    }
                },
            )
        launch.captureAppearance(app.resources.configuration)
        launch.captureReduceMotion(true, false)
        launch.onActivityCreated(null, false)
        val firstCommit = CompletableDeferred<Unit>()
        if (fonts) firstCommit.complete(Unit)
        val graph = AppGraph(app, fontPreloader = FontPreloader { _, _ -> }, launchProvider = { launch })
        compose.setContent {
            CompositionLocalProvider(LocalFullyDrawnReporterOwner provides reporterOwner) {
                CfRoot(graph, true, false, firstCommit, null)
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("launch.opening").captureToImage()
        compose.waitForIdle()
        return launch
    }

    private fun advanceTo(
        launch: LaunchController,
        ms: Int,
    ) {
        val target = launch.t0Nanos / 1_000_000 + ms
        while (compose.mainClock.currentTime < target) {
            compose.mainClock.advanceTimeBy(
                minOf(8, target - compose.mainClock.currentTime),
                ignoreFrameDuration = true,
            )
            clock.advance((compose.mainClock.currentTime - launch.t0Nanos / 1_000_000).milliseconds)
            frames.trySend(7L)
            compose.waitForIdle()
        }
    }

    private fun hidden(node: SemanticsNode) =
        generateSequence(node) { it.parent }.any {
            SemanticsProperties.HideFromAccessibility in
                it.config
        }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun profiles(): List<Array<Any>> =
            listOf(DeviceProfiles.pixel8, DeviceProfiles.largeFont).map { arrayOf<Any>(it) }
    }
}
