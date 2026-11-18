@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.launch.LaunchController
import net.chronoflow.feature.launch.LaunchOverrides
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WelcomeDestinationsReleaseTest {
    private val compose = createComposeRule()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(object : ExternalResource() {
        override fun before() {
            val app = ApplicationProvider.getApplicationContext<Application>()
            shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
        }
    }).around(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun releaseWelcomeActionsRemainInert() = runTest(timeout = 60.seconds) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val launch = LaunchController(moon = { ImageBitmap.imageResource(it, R.drawable.launch_moon) })
        launch.captureAppearance(app.resources.configuration)
        launch.captureReduceMotion(true, true)
        launch.claimOverlay(LaunchOverrides(skipOverlay = true))
        val graph = AppGraph(
            app,
            fontPreloader = FontPreloader { _, _ -> },
            launchProvider = { launch },
            overrides = GraphOverrides(
                sessionCipher = SoftwareSessionCipher(),
                dispatchers = AppDispatchers(dispatcher, Dispatchers.IO, dispatcher),
            ),
        )
        try {
            graph.rootNavigator.navigate(Route.Welcome)
            val revision = graph.rootNavigator.state.value.revision
            compose.setContent { CfRoot(graph, false, true, CompletableDeferred(Unit), null) }
            compose.onNodeWithTag("welcome.signIn").performClick()
            compose.runOnIdle { assertEquals(Route.Welcome, graph.rootNavigator.state.value.route) }
            compose.onNodeWithTag("welcome.wordmark").assertExists()
            compose.onNodeWithTag("welcome.plan").performClick()
            compose.runOnIdle {
                assertEquals(Route.Welcome, graph.rootNavigator.state.value.route)
                assertEquals(revision, graph.rootNavigator.state.value.revision)
            }
            runCurrent()
        } finally {
            graph.close()
        }
    }
}
