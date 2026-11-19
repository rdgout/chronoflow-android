package net.chronoflow.feature.launch

import androidx.compose.ui.test.junit4.v2.createComposeRule
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.model.LaunchRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.milliseconds

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaunchForcedExitTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun unresolvedSessionForcesWelcomeAt8950() = checkForced(true, 8950, LaunchRoute.Welcome)

    @Test fun unresolvedStoredDataForcesTodayAt8400() = checkForced(false, 8400, LaunchRoute.Today)

    private fun checkForced(
        session: Boolean,
        end: Int,
        route: LaunchRoute,
    ) {
        val host = LaunchEndingTestHost(compose, unresolvedSession = session, unresolvedData = !session)
        compose.mainClock.advanceTimeBy(7900, ignoreFrameDuration = true)
        host.clock.advance(7900.milliseconds)
        compose.waitForIdle()
        host.advanceTo(end + 8)
        assertTrue(host.controller.handOffComplete)
        assertEquals(route, host.controller.route)
        assertTrue(requireNotNull(host.controller.handOffEndElapsedNanos) in end * 1_000_000L..(end + 8) * 1_000_000L)
        assertEquals(if (session) 0 else 1, host.source.forced)
    }
}
