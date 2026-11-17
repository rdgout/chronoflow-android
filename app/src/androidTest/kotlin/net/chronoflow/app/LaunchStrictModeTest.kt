package net.chronoflow.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Test

@Smoke
class LaunchStrictModeTest : ScreenTest() {
    @Test fun seededLaunchSurvivesStrictModeThroughTheOpening() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val graph = (instrumentation.targetContext.applicationContext as ChronoFlowApp).graph
        instrumentation.runOnMainSync { graph.resetLaunchForTest() }
        launch(mapOf("CF_UI_TEST_PROFILE" to "launch"))
        waitFor(5000) { graph.launch.openingDone }
        compose.onNodeWithTag("launch.opening").assertExists()
    }
}
