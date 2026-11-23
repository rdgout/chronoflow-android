package net.chronoflow.app.auth

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.ioViolationsForTest
import net.chronoflow.app.resetIoViolationsForTest
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.debug.RouteProbe
import org.junit.Assert.assertEquals
import org.junit.Test

@Smoke
@OptIn(ExperimentalTestApi::class)
class SignInJourneyTest : ScreenTest() {
    @Test fun typedDemoSignInReachesTheAppRoot() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val graph = (instrumentation.targetContext.applicationContext as ChronoFlowApp).graph
        instrumentation.runOnMainSync { graph.resetIoViolationsForTest() }
        launch(
            mapOf(
                "CF_AUTH_TEST_AUDIT_SCREEN" to "sign-in",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_MOCK_DELAY" to "0",
            ),
        )
        compose.waitUntilExactlyOneExists(hasTestTag("sign-in.email"), 5_000)
        compose.onNodeWithTag("sign-in.email").performTextInput("demo@example.com")
        compose.onNodeWithTag("sign-in.email").performImeAction()
        compose.onNodeWithTag("sign-in.password").assertIsFocused()
        compose.onNodeWithTag("sign-in.password").performTextInput("password")
        compose.onNodeWithTag("sign-in.password").performImeAction()
        waitFor(5_000) { RouteProbe.state.route == Route.Today }
        compose.onNodeWithTag("sign-in.email").assertDoesNotExist()
        instrumentation.runOnMainSync { assertEquals(0, graph.ioViolationsForTest) }
    }
}
