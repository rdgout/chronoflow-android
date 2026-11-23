package net.chronoflow.app

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.core.view.WindowCompat
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.designsystem.generated.CfLightColors
import net.chronoflow.core.model.auth.ResetPasswordLink
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.LaunchOverrides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CfRootTest {
    @get:Rule val compose = createComposeRule()

    @Test fun overlayComposesFirstAndShieldsTheContentOnTheNextFrame() {
        compose.mainClock.autoAdvance = false
        val app = ApplicationProvider.getApplicationContext<Application>()
        val launch =
            newLaunchController(AppGraph(app)).apply {
                captureAppearance(app.resources.configuration)
                captureReduceMotion(false, null)
                claimOverlay(LaunchOverrides.None)
            }
        val graph = AppGraph(app, launchProvider = { launch })
        var clicks = 0
        compose.setContent {
            CfRoot(graph, true, true, CompletableDeferred(), null) {
                Box(Modifier.fillMaxSize().testTag("root.fixture").clickable { clicks++ })
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("launch.opening").assertExists()
        compose.onNodeWithTag("root.fixture").assertDoesNotExist()
        compose.mainClock.advanceTimeByFrame()
        val node = compose.onNodeWithTag("root.fixture", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue(node.parent!!.config.contains(SemanticsProperties.HideFromAccessibility))
        compose.onNodeWithTag("app.root").performTouchInput { click(center) }
        assertEquals(0, clicks)
    }

    @Test fun skippedOverlayComposesAccessibleContentOnTheFirstFrame() {
        compose.mainClock.autoAdvance = false
        val app = ApplicationProvider.getApplicationContext<Application>()
        val launch = newLaunchController(AppGraph(app)).apply { claimOverlay(LaunchOverrides(skipOverlay = true)) }
        val graph = AppGraph(app, launchProvider = { launch })
        compose.setContent {
            CfRoot(
                graph,
                false,
                true,
                CompletableDeferred(),
                null,
            ) { Box(Modifier.testTag("root.fixture")) }
        }
        compose.onNodeWithTag("launch.opening").assertDoesNotExist()
        val node = compose.onNodeWithTag("root.fixture", useUnmergedTree = true).fetchSemanticsNode()
        assertFalse(node.parent!!.config.contains(SemanticsProperties.HideFromAccessibility))
    }

    @Test fun lightScreenUsesDarkSystemBarIcons() = verifySystemBarFlags(false)

    @Test fun darkScreenUsesLightSystemBarIcons() = verifySystemBarFlags(true)

    /** Android-only regression (new): Reset's navy sky needs light status icons without the launch overlay. */
    @Test fun lightResetSkyUsesLightStatusBarIcons() {
        RuntimeEnvironment.setQualifiers("+notnight")
        val app = ApplicationProvider.getApplicationContext<Application>()
        val dispatcher = StandardTestDispatcher()
        val graph =
            AppGraph(
                app,
                overrides = GraphOverrides(dispatchers = AppDispatchers(dispatcher, Dispatchers.IO, dispatcher)),
            )
        graph.auth.openReset(ResetPasswordLink("reset-token", "person@example.com"))
        var statusIcons: IconAppearance? = null
        try {
            compose.setContent {
                CfRoot(
                    graph,
                    false,
                    true,
                    CompletableDeferred(),
                    null,
                    onSystemBars = { night, overlay, sky ->
                        assertFalse(night)
                        assertFalse(overlay)
                        statusIcons = SystemBarStyles.statusBar(night, overlay, sky)
                    },
                )
            }
            compose.onNodeWithTag("reset.password").assertExists()
            assertEquals(IconAppearance.LightIcons, statusIcons)
        } finally {
            runTest(dispatcher, timeout = 60.seconds) { graph.close() }
        }
    }

    @Test fun lightRootPaintsTheAppBackgroundWithoutAnOverlay() = verifyRootBackground(false)

    @Test fun darkRootPaintsTheAppBackgroundWithoutAnOverlay() = verifyRootBackground(true)

    private fun verifyRootBackground(night: Boolean) {
        RuntimeEnvironment.setQualifiers(if (night) "+night" else "+notnight")
        val app = ApplicationProvider.getApplicationContext<Application>()
        compose.setContent { CfRoot(AppGraph(app), false, true, CompletableDeferred(), null) {} }
        compose.onNodeWithTag("launch.opening").assertDoesNotExist()
        val pixels = compose.onNodeWithTag("app.root").captureToImage().toPixelMap()
        val expected = (if (night) CfDarkColors else CfLightColors).background.toArgb()
        assertEquals(expected, pixels[0, 0].toArgb())
        assertEquals(expected, pixels[pixels.width / 2, pixels.height / 2].toArgb())
        assertEquals(expected, pixels[pixels.width - 1, pixels.height - 1].toArgb())
    }

    private fun verifySystemBarFlags(night: Boolean) {
        RuntimeEnvironment.setQualifiers(if (night) "+night" else "+notnight")
        val app = ApplicationProvider.getApplicationContext<ChronoFlowApp>()
        app.graph.resetLaunchForTest()
        val intent =
            Intent(app, MainActivity::class.java)
                .putExtra("CF_AUTH_TEST_ID", "0199a3c0-0000-7000-8000-000000000000")
                .putExtra("CF_UI_TEST_PROFILE", "screen")
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup()
        try {
            val window = controller.get().window
            val bars = WindowCompat.getInsetsController(window, window.decorView)
            assertFalse(bars.isAppearanceLightStatusBars)
            assertEquals(!night, bars.isAppearanceLightNavigationBars)
        } finally {
            controller.close()
        }
    }
}
