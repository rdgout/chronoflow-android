package net.chronoflow.app

import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Test

@Smoke
class LaunchStrictModeTest : ScreenTest() {
    @Test fun seededLaunchSurvivesStrictModeThroughTheSlowHandOff() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val graph = (instrumentation.targetContext.applicationContext as ChronoFlowApp).graph
        instrumentation.runOnMainSync { graph.resetLaunchForTest() }
        launch(
            mapOf(
                "CF_UI_TEST_PROFILE" to "launch",
                "cf.session" to "in",
                "cf.stored" to "no",
                "cf.readyDelay" to "3.5",
                "cf.reduceMotion" to "false",
            ),
        )
        waitFor(5000) { graph.launch.handOffComplete }
        org.junit.Assert.assertNotNull(graph.launch.statusRaster)
        org.junit.Assert.assertEquals(1, graph.launch.statusAnnouncementCount)
        compose.onNodeWithTag("route.today").assertExists()
        compose.onNodeWithTag("launch.opening").assertDoesNotExist()
    }
}
