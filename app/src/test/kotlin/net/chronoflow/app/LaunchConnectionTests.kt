package net.chronoflow.app

import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.feature.launch.model.LaunchState
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchConnectionTests {
    @Test fun launchDoesNotOverwriteSessionNavigation() {
        val navigator = RootNavigator()
        val bridge = LaunchSessionBridge(navigator)
        bridge.update(LaunchState(session = SessionPresence.SignedIn("account"), gate = Gate.App))
        navigator.navigate(Route.SignIn)
        val revision = navigator.state.value.revision
        bridge.update(LaunchState(session = SessionPresence.SignedIn("account"), gate = Gate.App, hasStoredData = true))
        assertEquals(Route.SignIn, navigator.state.value.route)
        assertEquals(revision, navigator.state.value.revision)
    }
}
