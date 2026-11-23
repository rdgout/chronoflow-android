package net.chronoflow.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertEquals
import org.junit.Test

@Smoke
@OptIn(ExperimentalTestApi::class)
class WelcomeJourneyTest : ScreenTest() {
    @Test fun coldWelcomeOpensTheSignInSeamWithoutMainThreadIo() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val graph = (instrumentation.targetContext.applicationContext as ChronoFlowApp).graph
        instrumentation.runOnMainSync {
            graph.resetLaunchForTest()
            graph.resetIoViolationsForTest()
        }
        launch(mapOf("CF_UI_TEST_PROFILE" to "launch", "cf.session" to "out"))
        compose.waitUntilDoesNotExist(hasTestTag("launch.opening"), 5_000)
        val signIn = hasTestTag("welcome.signIn") and hasClickAction()
        compose.waitUntilExactlyOneExists(signIn, 3_000)
        compose.onNode(signIn).performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("sign-in.email"), 3_000)
        instrumentation.runOnMainSync { assertEquals(0, graph.ioViolationsForTest) }
    }
}
