package net.chronoflow.feature.trips

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.contracts.Language
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.clock.MinuteClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TripsAppBarRefreshLayoutTest {
    private val profile = DeviceProfiles.pixel8
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(compose)

    @Test fun downwardScrollExpandsTheBarBeforePullToRefresh() {
        var refreshes = 0
        val clock = MinuteClock(mutableStateOf(Instant.parse("2026-11-22T12:00:00Z")))
        compose.setContent {
            ProfileHost(profile) {
                ChronoFlowTheme(reduceMotion = true) {
                    TripsContent(
                        TripsUiState.Empty,
                        net.chronoflow.core.sync.refresh
                            .RefreshFeedbackState(),
                        Language.En,
                        { refreshes++ },
                        {},
                        {},
                        {},
                        clock = clock,
                    )
                }
            }
        }

        fun barHeight() =
            compose
                .onNodeWithTag("trips.appbar")
                .fetchSemanticsNode()
                .boundsInRoot.height
        val list = compose.onNodeWithTag("trips.list")
        val expanded = barHeight()
        list.performTouchInput { swipeUp() }
        val collapsed = barHeight()
        assertTrue("the upward swipe must collapse the bar", collapsed < expanded)
        // Cover the collapse range and touch slop, while staying below the native refresh threshold.
        list.performTouchInput {
            swipeDown(startY = height * .2f, endY = height * .2f + (expanded - collapsed) * 1.5f)
        }
        assertEquals("the bar must expand before refresh consumes the drag", expanded, barHeight(), 1f)
        assertEquals(0, refreshes)
        list.performTouchInput { swipeDown() }
        compose.waitForIdle()
        assertEquals("pull-to-refresh must still work with the bar expanded", 1, refreshes)
    }
}
