package net.chronoflow.app

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.launch.SignedOutReadiness
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.toRoute
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.sync.session.RecordingAccountStore
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionGateRouter
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.sync.session.sessionResult
import net.chronoflow.feature.launch.model.LaunchState
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class LaunchConnectionTests {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun coldLaunchBridgeAndGateRouterSelectTheSameToday() =
        runTest(timeout = 60.seconds) {
            val navigator = RootNavigator()
            val controller =
                SessionController(
                    MemorySessionStore(),
                    RecordingAccountStore(),
                    SessionTeardown(sleep = {}),
                    StandardTestDispatcher(testScheduler),
                )
            val readiness =
                object : LaunchReadiness by
                SignedOutReadiness {
                    override fun gateUpdates() = flowOf(Gate.App)
                }
            val router =
                SessionGateRouter(
                    controller,
                    readiness,
                    backgroundScope,
                    onSignedOut = { navigator.navigate(Route.SignIn) },
                    onGate = { navigator.navigate(it.toRoute()) },
                    onVerify = { navigator.navigate(Route.Verify) },
                )
            try {
                controller.accept(sessionResult())
                LaunchSessionBridge(
                    navigator,
                ).update(LaunchState(session = SessionPresence.SignedIn("account"), gate = Gate.App))
                val before = navigator.state.value.revision
                assertEquals(Route.Today, navigator.state.value.route)
                router.start()
                runCurrent()
                assertEquals(before + 1, navigator.state.value.revision)
                assertEquals(Route.Today, navigator.state.value.route)
            } finally {
                router.stop()
                controller.close()
            }
        }

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
