package net.chronoflow.app.auth

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import net.chronoflow.app.SessionTestApplication
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

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

    @Test fun drawnBackSignsOutRevokesTheOldTokenAndWipesTheAccount() {
        verify()
        val snapshot = graph.appScope.async { checkNotNull(graph.controller.activeSession()) }
        waitFor { snapshot.isCompleted }
        val old = snapshot.getCompleted()
        val model = checkNotNull(graph.auth.verification.model.value)
        compose.onNodeWithTag("auth.back").performClick()
        waitFor {
            graph.rootNavigator.state.value.route == Route.SignIn &&
                graph.controller.presence == SessionPresence.SignedOut
        }
        waitFor { model.scope.coroutineContext[Job]?.isCompleted == true }
        val evidence =
            graph.appScope.async(graph.dispatchers.io) {
                val root = graph.storageRoot()
                val exists = File(root, "ChronoFlowMock/Accounts/${old.session.accountId}").exists()
                val source = checkNotNull(graph.mockSources).dataSource(root, graph.overrides) { old.session.token }
                val revoked =
                    try {
                        source.fetchIdentity()
                        false
                    } catch (failure: DataFailure.Unauthorized) {
                        failure.status ==
                            401
                    }
                exists to revoked
            }
        waitFor { evidence.isCompleted }
        assertFalse(evidence.getCompleted().first)
        assertTrue(evidence.getCompleted().second)
        assertEquals(Route.SignIn, graph.rootNavigator.state.value.route)
        assertEquals(null, graph.auth.verification.model.value)
    }

    @Test fun systemBackLeavesTheUnverifiedSessionSignedIn() {
        verify()
        onActivity { it.onBackPressedDispatcher.onBackPressed() }
        assertTrue(graph.controller.presence is SessionPresence.SignedIn)
    }
}
