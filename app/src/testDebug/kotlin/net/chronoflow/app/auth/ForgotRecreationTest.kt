package net.chronoflow.app.auth

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import kotlinx.coroutines.test.runTest
import net.chronoflow.app.SessionTestApplication
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = SessionTestApplication::class)
class ForgotRecreationTest : AuthResetActivityFixture() {
    @Test fun recreationKeepsTheFormEmailAndProcessModel() {
        launch("forgot")
        val model = checkNotNull(graph.auth.forgotModel)
        compose.onNodeWithTag("forgot.email").performTextInput("typed@example.com")
        assertEquals("typed@example.com", model.email.value)
        recreate(Route.Forgot)
        assertSame(model, graph.auth.forgotModel)
        assertEquals("typed@example.com", model.email.value)
        compose.onNodeWithTag("forgot.email").assertExists()
    }

    @Test fun recreationKeepsTheSentStateAndBackReturnsToSignIn() {
        launch("forgot")
        val model = checkNotNull(graph.auth.forgotModel)
        runTest(dispatcher, timeout = 60.seconds) {
            model.editEmail("submitted@example.com")
            model.submit()
        }
        compose.onNodeWithTag("forgot.notice").assertExists()
        recreate(Route.Forgot)
        assertSame(model, graph.auth.forgotModel)
        assertEquals("submitted@example.com", model.sentEmail.value)
        compose.onNodeWithTag("forgot.back").performClick()
        waitFor { graph.rootNavigator.state.value.route == Route.SignIn }
    }

    @Test fun systemBackReturnsToSignIn() {
        launch("forgot")
        scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitFor { graph.rootNavigator.state.value.route == Route.SignIn }
    }
}
