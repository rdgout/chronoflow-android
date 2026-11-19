package net.chronoflow.app

import android.app.Application
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
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.launch.SessionPresence
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.LaunchController
import net.chronoflow.feature.launch.model.ManualLaunchClock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.milliseconds

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CfRootHandOffTest {
    @get:Rule val compose = createComposeRule()

    @Test fun signedInContentBecomesAccessibleOnlyAfterTheHandOff() {
        val launch = mount(SessionPresence.SignedIn("a"))
        compose.onNodeWithTag("app.root").assertExists()
        compose.mainClock.advanceTimeByFrame()
        val before = compose.onNodeWithTag("route.today", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue(hidden(before))
        advanceTo(launch, 1908)
        compose.mainClock.advanceTimeByFrame()
        val after = compose.onNodeWithTag("route.today", useUnmergedTree = true).fetchSemanticsNode()
        assertFalse(hidden(after))
        compose.onNodeWithTag("launch.opening").assertDoesNotExist()
    }

    @Test fun signedOutHandsOffToWelcome() {
        val launch = mount(SessionPresence.SignedOut)
        advanceTo(launch, 2558)
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("route.welcome").assertExists()
        compose.onNodeWithTag("launch.opening").assertDoesNotExist()
    }

    @Test fun termsGateHandsOffToTerms() {
        val launch = mount(SessionPresence.SignedIn("a"), Gate.Terms)
        advanceTo(launch, 1908)
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("route.terms").assertExists()
    }

    @Test fun screenProfileShowsAccessibleWelcomeImmediately() {
        mount(SessionPresence.SignedOut, screen = true)
        compose.onNodeWithTag("route.welcome").assertExists()
        assertFalse(hidden(compose.onNodeWithTag("route.welcome", useUnmergedTree = true).fetchSemanticsNode()))
        compose.onNodeWithTag("launch.opening").assertDoesNotExist()
    }

    private val clock = ManualLaunchClock()
    private val frames = Channel<Long>(Channel.CONFLATED)

    private fun mount(
        presence: SessionPresence,
        gate: Gate = Gate.App,
        screen: Boolean = false,
    ): LaunchController {
        compose.mainClock.autoAdvance = false
        val app = ApplicationProvider.getApplicationContext<Application>()
        val readiness =
            object : LaunchReadiness {
                override suspend fun session() = presence

                override suspend fun gate() = gate

                override suspend fun hasStoredData() = true

                override suspend fun allowFirstLoad() = Unit
            }
        val launch =
            LaunchController(moon = { ImageBitmap.imageResource(it, R.drawable.launch_moon) }, nanoTime = {
                compose.mainClock.currentTime *
                    1_000_000
            }, nextFrameNanos = {
                frames.receive()
            }, clockFactory = { clock }, readinessFactory = { readiness })
        launch.captureAppearance(app.resources.configuration)
        launch.captureReduceMotion(true, false)
        launch.onActivityCreated(null, screen)
        val graph = AppGraph(app, launchProvider = { launch })
        compose.setContent { CfRoot(graph, !screen, false, CompletableDeferred(), null) }
        compose.mainClock.advanceTimeByFrame()
        if (!screen &&
            launch.t0Nanos == LaunchController.UNSET
        ) {
            compose.onNodeWithTag("launch.opening").captureToImage()
        }
        compose.waitForIdle()
        return launch
    }

    private fun advanceTo(
        launch: LaunchController,
        ms: Int,
    ) {
        val end = launch.t0Nanos / 1_000_000 + ms
        while (compose.mainClock.currentTime < end) {
            val elapsed = compose.mainClock.currentTime - launch.t0Nanos / 1_000_000
            var step = minOf(8, end - compose.mainClock.currentTime)
            if (elapsed < 1550) step = minOf(step, 1550 - elapsed)
            compose.mainClock.advanceTimeBy(step, ignoreFrameDuration = true)
            clock.advance((compose.mainClock.currentTime - launch.t0Nanos / 1_000_000).milliseconds)
            frames.trySend(7)
            compose.waitForIdle()
        }
    }

    private fun hidden(node: SemanticsNode) =
        generateSequence(node) { it.parent }.any {
            SemanticsProperties.HideFromAccessibility in
                it.config
        }
}
