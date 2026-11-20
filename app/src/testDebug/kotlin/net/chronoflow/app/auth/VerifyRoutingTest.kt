package net.chronoflow.app.auth

import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import net.chronoflow.app.SessionTestApplication
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalCoroutinesApi::class)
@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = SessionTestApplication::class)
class VerifyRoutingTest : AuthResetActivityFixture() {
    private fun verify() {
        launch(extras = mapOf("CF_AUTH_TEST_SIGNED_IN" to "demo", "CF_AUTH_TEST_UNVERIFIED" to "1"))
        waitFor {
            val model = graph.auth.verification.model.value
            graph.rootNavigator.state.value.route == Route.Verify &&
                model?.uiState?.value?.hasStatus == true && !model.form.busy
        }
        compose.onNodeWithTag("verify.open-email").assertExists()
    }

    @Test fun systemBackLeavesTheUnverifiedSessionSignedIn() {
        verify()
        onActivity { it.onBackPressedDispatcher.onBackPressed() }
        assertTrue(graph.controller.presence is SessionPresence.SignedIn)
    }
}
