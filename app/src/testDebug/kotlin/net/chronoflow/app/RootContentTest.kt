package net.chronoflow.app

import android.content.Intent
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.navigation.RootState
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RootContentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun termsRendersOneTaggedRoot() {
        compose.setContent {
            RootContentTestHost {
                RootContent(
                    RootState(Route.Terms, 1),
                    {},
                ) { RoutePlaceholder(it, LaunchPlatform.routeLabel) }
            }
        }
        compose.onNodeWithTag("route.terms").assertExists()
    }

    @Test fun repeatingARouteReplacesItsRememberedSubtree() {
        val state = mutableStateOf(RootState(Route.Terms, 1))
        var constructions = 0
        var presented = 0
        compose.setContent {
            RootContentTestHost {
                RootContent(state.value, {}) { route ->
                    val identity = remember { ++constructions }
                    SideEffect { presented = identity }
                    RoutePlaceholder(route, LaunchPlatform.routeLabel)
                }
            }
        }
        compose.onNodeWithTag("route.terms").assertExists()
        val first = presented
        compose.runOnIdle { state.value = RootState(Route.Terms, 2) }
        compose.onNodeWithTag("route.terms").assertExists()
        compose.runOnIdle { assertEquals(first + 1, presented) }
    }

    @Config(application = SessionTestApplication::class)
    @Test
    fun recreationKeepsTheProcessRouteWithoutAnotherNavigation() {
        val app = ApplicationProvider.getApplicationContext<SessionTestApplication>()
        val intent =
            Intent(app, MainActivity::class.java)
                .putExtra("CF_AUTH_TEST_ID", "0199a3c0-0000-7000-8000-000000000000")
                .putExtra("CF_UI_TEST_PROFILE", "screen")
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            scenario.onActivity { app.graph.rootNavigator.navigate(Route.Terms) }
            compose.onNodeWithTag("route.terms").assertExists()
            val revision = app.graph.rootNavigator.state.value.revision
            scenario.recreate()
            compose.onNodeWithTag("route.terms").assertExists()
            assertEquals(RootState(Route.Terms, revision), app.graph.rootNavigator.state.value)
        }
    }
}
