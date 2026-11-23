package net.chronoflow.app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.navigation.RootState
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Android-only regression for Nav3's default transition overlapping root destinations. */
@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = SessionTestApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RootRouteTransitionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun outgoingTermsIsRemovedImmediatelyWhenTodayReplacesIt() {
        compose.mainClock.autoAdvance = false
        val state = mutableStateOf(RootState(Route.Terms, 1))
        compose.setContent {
            ChronoFlowTheme {
                RootContent(state.value, {}) { RoutePlaceholder(it, null) }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("route.terms").assertExists()
        compose.runOnIdle { state.value = RootState(Route.Today, 2) }
        compose.waitForIdle()
        // Let Nav3 settle without waiting long enough to hide a short crossfade.
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.onNodeWithTag("route.today").assertExists()
        compose.onNodeWithTag("route.terms").assertDoesNotExist()
    }
}
