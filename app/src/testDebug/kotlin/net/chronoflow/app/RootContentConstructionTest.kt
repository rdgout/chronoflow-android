@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

/** Android-only regression for Nav3 composition identity across launch and gate routing. */
@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RootContentConstructionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun navigatingFromTermsConstructsOnlyTheIncomingToday() {
        val state = mutableStateOf(RootState(Route.Terms, 1))
        val constructions = mutableListOf<Route>()
        compose.setContent {
            RootContentTestHost {
                RootContent(state.value, {}) { route ->
                    remember { constructions.add(route) }
                    RoutePlaceholder(route, null)
                }
            }
        }
        compose.onNodeWithTag("route.terms").assertExists()
        compose.runOnIdle { state.value = RootState(Route.Today, 2) }
        compose.onNodeWithTag("route.today").assertExists()
        compose.runOnIdle { assertEquals(listOf(Route.Terms, Route.Today), constructions) }
    }

    @Test fun gateRevisionKeepsTheCurrentTodaySubtree() {
        val state = mutableStateOf(RootState(Route.Today, 1))
        var constructions = 0
        compose.setContent {
            RootContentTestHost {
                RootContent(state.value, {}) { route ->
                    remember { constructions++ }
                    RoutePlaceholder(route, null)
                }
            }
        }
        compose.onNodeWithTag("route.today").assertExists()
        compose.runOnIdle { state.value = RootState(Route.Today, 2) }
        compose.onNodeWithTag("route.today").assertExists()
        compose.runOnIdle { assertEquals(1, constructions) }
    }
}
