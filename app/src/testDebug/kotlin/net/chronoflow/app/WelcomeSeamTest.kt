package net.chronoflow.app

import android.app.Application
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.LaunchController
import net.chronoflow.feature.launch.LaunchMode
import net.chronoflow.feature.launch.LaunchOverrides
import net.chronoflow.feature.launch.model.ManualLaunchClock
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.milliseconds

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WelcomeSeamTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun welcomeActionsOpenTheSeamsAndBackReturnsToFinalWelcome() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val clock = ManualLaunchClock().apply { advance(2300.milliseconds) }
        val launch =
            LaunchController(moon = { ImageBitmap.imageResource(it, R.drawable.launch_moon) }, clockFactory = { clock })
        launch.captureAppearance(app.resources.configuration)
        launch.captureReduceMotion(true, false)
        launch.onActivityCreated(null, false, LaunchOverrides(mode = LaunchMode.Welcome, freezeAt = 2300.milliseconds))
        val graph = AppGraph(app, fontPreloader = FontPreloader { _, _ -> }, launchProvider = { launch })
        lateinit var back: OnBackPressedDispatcher
        compose.setContent {
            val dispatcher = requireNotNull(LocalOnBackPressedDispatcherOwner.current).onBackPressedDispatcher
            SideEffect { back = dispatcher }
            CfRoot(graph, true, false, CompletableDeferred(Unit), null)
        }
        compose.onNodeWithTag("launch.opening").captureToImage()
        compose.waitForIdle()
        compose.runOnIdle { launch.onExitFrame(2550.milliseconds.inWholeNanoseconds) }
        compose.waitForIdle()
        org.junit.Assert.assertTrue(requireNotNull(launch.launchIntro).hasPlayed)
        val initial = compose.onNodeWithTag("welcome.wordmark").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("welcome.signIn").performClick()
        compose.onNodeWithTag("sign-in.email").assertExists()
        compose.runOnIdle { back.onBackPressed() }
        val returned = compose.onNodeWithTag("welcome.wordmark").fetchSemanticsNode().boundsInRoot
        assertEquals(initial.top - 14 * DeviceProfiles.pixel8.density, returned.top, 0.5f)
        compose.onNodeWithTag("welcome.plan").performClick()
        compose.onNodeWithTag("route.guestWizard").assertExists()
        compose.runOnIdle { graph.welcomeDestinations.closeSeam() }
    }
}
