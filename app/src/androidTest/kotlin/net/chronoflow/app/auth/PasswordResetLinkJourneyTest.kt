package net.chronoflow.app.auth

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.ioViolationsForTest
import net.chronoflow.app.resetIoViolationsForTest
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.AuthLinkUrls
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.debug.RouteProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

@Smoke
@OptIn(ExperimentalTestApi::class)
class PasswordResetLinkJourneyTest : ScreenTest() {
    @Test fun forgotLinkResetDoneReturnsToSignIn() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val app = context.applicationContext as ChronoFlowApp
        instrumentation.runOnMainSync { app.graph.resetIoViolationsForTest() }
        launch(mapOf("CF_AUTH_TEST_AUDIT_SCREEN" to "forgot", "CF_MOCK_DELAY" to "0"))
        val graph = app.graph
        compose.waitUntilExactlyOneExists(hasTestTag("forgot.email"), 5_000)
        compose.onNodeWithTag("forgot.email").performTextInput("demo@example.com")
        compose.onNodeWithTag("forgot.submit").performScrollTo().performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("forgot.notice"), 5_000)
        val link =
            runBlocking(Dispatchers.IO) {
                checkNotNull(graph.mockSources!!.latestResetLink(graph.dataSources.public(), "demo@example.com"))
            }
        val url = AuthLinkUrls.reset(link.token, link.email)
        val descriptor =
            instrumentation.uiAutomation.executeShellCommand(
                "am start -W -a android.intent.action.VIEW -c android.intent.category.BROWSABLE -d $url ${context.packageName}",
            )
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
        compose.waitUntilExactlyOneExists(hasTestTag("reset.password"), 5_000)
        compose.onNodeWithTag("reset.password").performTextInput("NewPassword12!")
        compose.onNodeWithTag("reset.confirmation").performTextInput("NewPassword12!")
        compose.onNodeWithTag("reset.submit").performScrollTo().performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("reset.continue"), 5_000)
        compose.onNodeWithTag("reset.continue").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("sign-in.email"), 5_000)
        assertEquals(Route.SignIn, RouteProbe.state.route)
        assertEquals(SessionPresence.SignedOut, graph.controller.presence)
        val sessionExists =
            runBlocking(Dispatchers.IO) {
                File(graph.storageRoot(), "${graph.namespace.directoryName}/session.bin").exists()
            }
        assertFalse(sessionExists)
        instrumentation.runOnMainSync { assertEquals(0, graph.ioViolationsForTest) }
    }
}
