@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.chronoflow.core.model.launch.Gate
import net.chronoflow.core.model.launch.LaunchReadiness
import net.chronoflow.core.model.launch.SignedOutReadiness
import net.chronoflow.core.model.session.SessionPresence
import net.chronoflow.core.navigation.RootNavigator
import net.chronoflow.core.navigation.RootState
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.navigation.toRoute
import net.chronoflow.core.sync.session.MemorySessionStore
import net.chronoflow.core.sync.session.RecordingAccountStore
import net.chronoflow.core.sync.session.SessionController
import net.chronoflow.core.sync.session.SessionGateRouter
import net.chronoflow.core.sync.session.SessionTeardown
import net.chronoflow.core.sync.session.sessionResult
import net.chronoflow.core.testing.sync.syncAccount
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.feature.launch.model.LaunchState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

/** Android-only regression for Nav3 composition identity across launch and gate routing. */
@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = SessionTestApplication::class)
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

    @Test fun coldLaunchBridgeAndGateRouterConstructTodayOnce() =
        runTest(timeout = 60.seconds) {
            val navigator = RootNavigator()
            val constructions = mutableListOf<Route>()
            val controller =
                SessionController(
                    MemorySessionStore(),
                    RecordingAccountStore(),
                    SessionTeardown(sleep = { awaitCancellation() }),
                    StandardTestDispatcher(testScheduler),
                )
            val readiness =
                object : LaunchReadiness by SignedOutReadiness {
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
                LaunchSessionBridge(navigator).update(
                    LaunchState(session = SessionPresence.SignedIn(syncAccount), gate = Gate.App),
                )
                compose.setContent {
                    val state by navigator.state.collectAsState()
                    RootContentTestHost {
                        RootContent(state, {}) { route ->
                            remember { constructions.add(route) }
                            RoutePlaceholder(route, null)
                        }
                    }
                }
                compose.onNodeWithTag("route.today").assertExists()
                assertEquals(listOf(Route.Today), constructions)
                val beforeHandOff = navigator.state.value.revision
                router.start()
                runCurrent()
                assertEquals(beforeHandOff + 1, navigator.state.value.revision)
                compose.onNodeWithTag("route.today").assertExists()
                compose.runOnIdle { assertEquals(listOf(Route.Today), constructions) }
            } finally {
                router.stop()
                controller.close()
            }
        }
}
