package net.chronoflow.app

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CompletableDeferred
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.LaunchOverrides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

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
            newLaunchController().apply {
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
        val launch = newLaunchController().apply { claimOverlay(LaunchOverrides(skipOverlay = true)) }
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
}
