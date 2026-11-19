package net.chronoflow.app

import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.feature.launch.model.LaunchRoute
import net.chronoflow.feature.launch.model.LaunchState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LaunchSessionBridgeTest {
    @Test fun signedOutRoutesToWelcome() = checkState(LaunchState(session = SessionPresence.SignedOut), Route.Welcome)

    @Test fun signedInTermsRoutesToTerms() =
        checkState(LaunchState(session = SessionPresence.SignedIn("a"), gate = Gate.Terms), Route.Terms)

    @Test fun forcedUnknownGateRoutesToToday() =
        checkState(LaunchState(session = SessionPresence.SignedIn("a"), didForceReady = true), Route.Today)

    @Test fun unresolvedGateDoesNotNavigate() {
        val navigator = RootNavigator()
        LaunchSessionBridge(navigator).update(LaunchState(session = SessionPresence.SignedIn("a")))
        assertNull(navigator.state.value.route)
        assertEquals(0L, navigator.state.value.revision)
    }

    @Test fun laterLaunchRoutesRemainConnectedUntilAnotherOwnerNavigates() {
        val navigator = RootNavigator()
        val bridge = LaunchSessionBridge(navigator)
        bridge.update(LaunchState(session = SessionPresence.SignedIn("a"), gate = Gate.Terms))
        bridge.update(LaunchState(session = SessionPresence.SignedIn("a"), gate = Gate.App))
        assertEquals(Route.Today, navigator.state.value.route)
        assertEquals(2L, navigator.state.value.revision)
    }

    @Test fun welcomeMapsToWelcome() = assertEquals(Route.Welcome, LaunchRoute.Welcome.toRoute())

    @Test fun todayMapsToToday() = assertEquals(Route.Today, LaunchRoute.Today.toRoute())

    @Test fun verifyMapsToVerify() = assertEquals(Route.Verify, LaunchRoute.Verify.toRoute())

    @Test fun termsMapsToTerms() = assertEquals(Route.Terms, LaunchRoute.Terms.toRoute())

    @Test fun walkthroughMapsToFirstRunWalkthrough() =
        assertEquals(Route.Walkthrough(false), LaunchRoute.Walkthrough.toRoute())

    private fun checkState(
        state: LaunchState,
        expected: Route,
    ) {
        val navigator = RootNavigator()
        LaunchSessionBridge(navigator).update(state)
        assertEquals(expected, navigator.state.value.route)
    }
}
