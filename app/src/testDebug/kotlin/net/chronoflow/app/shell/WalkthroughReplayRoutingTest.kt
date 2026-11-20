@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package net.chronoflow.app.shell

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import net.chronoflow.core.designsystem.layout.LocalSafeInsets
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.session.SessionSnapshot
import net.chronoflow.core.model.session.SignOutResult
import net.chronoflow.core.model.session.StoredSession
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.DeviceProfile
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.onboarding.OnboardingSession
import net.chronoflow.feature.onboarding.OnboardingSessions
import net.chronoflow.feature.onboarding.walkthrough.WalkthroughModel
import net.chronoflow.feature.onboarding.walkthrough.WalkthroughRoute
import net.chronoflow.feature.onboarding.walkthrough.WalkthroughViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class, qualifiers = "w411dp-h914dp-420dpi")
class WalkthroughReplayRoutingTest(
    private val profile: DeviceProfile,
) {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(compose)
    private var replayBottom = -1
    private var expectedBottom = -1
    private lateinit var holder: ShellViewModel
    private lateinit var activity: ComponentActivity
    private lateinit var current: WalkthroughViewModel
    private var writes = 0

    private fun host() {
        val session =
            OnboardingSession(
                SessionSnapshot(StoredSession("test-token", "account"), 1),
                enqueue = {
                    writes++
                    error("replay wrote")
                },
                policies = flowOf(null),
                signOut = { SignOutResult.SignedOut },
            )
        val sessions = OnboardingSessions { flowOf(session) }
        val dispatcher = UnconfinedTestDispatcher()
        compose.setContent {
            ProfileHost(profile) {
                ChronoFlowTheme(reduceMotion = true) {
                    val h = rememberShellViewModel(Route.Settings)
                    val a = LocalActivity.current as ComponentActivity
                    SideEffect {
                        holder = h
                        activity = a
                    }
                    AppShellScaffold(
                        h,
                        {},
                        today = {},
                        trips = {},
                        settings = { Box(Modifier.fillMaxSize().testTag("route.settings")) },
                        page = {
                            val factory =
                                remember {
                                    viewModelFactory {
                                        initializer {
                                            WalkthroughViewModel(
                                                true,
                                                sessions,
                                                dispatcher,
                                            )
                                        }
                                    }
                                }
                            val model: WalkthroughViewModel = viewModel(factory = factory)
                            val density = LocalDensity.current
                            val bottom = LocalSafeInsets.current.getBottom(density)
                            val expected =
                                with(density) {
                                    profile.insets.bottom.dp
                                        .roundToPx()
                                }
                            SideEffect {
                                current = model
                                replayBottom = bottom
                                expectedBottom = expected
                            }
                            WalkthroughRoute(model) { h.back() }
                        },
                        overlay = {},
                    )
                }
            }
        }
    }

    @Test fun replayHidesChromeAndEachEntryStartsFreshWithoutWrites() {
        host()
        compose.runOnIdle { holder.push(Route.Walkthrough(true)) }
        compose.onNodeWithTag("walkthrough.screen").assertExists()
        compose.onNodeWithTag("cf.nav.none").assertExists()
        compose.onNodeWithTag("tab.settings").assertDoesNotExist()
        assertEquals(expectedBottom, replayBottom)
        val first = current
        compose.onNodeWithTag("walkthrough.next").performClick()
        compose.onNodeWithTag("walkthrough.skip").performClick()
        compose.onNodeWithTag("route.settings").assertExists()
        compose.onNodeWithTag("tab.settings").assertExists()
        compose.runOnIdle { holder.push(Route.Walkthrough(true)) }
        compose.onNodeWithTag("walkthrough.screen").assertExists()
        assertNotSame(first, current)
        assertEquals(WalkthroughModel.Page.Timeline, current.state.value!!.page)
        compose.onNodeWithTag("walkthrough.skip").performClick()
        assertEquals(0, writes)
    }

    @Test fun systemBackExitsReplayWithoutWrites() {
        host()
        compose.runOnIdle { holder.push(Route.Walkthrough(true)) }
        compose.onNodeWithTag("walkthrough.screen").assertExists()
        compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithTag("route.settings").assertExists()
        assertEquals(0, writes)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun profiles() =
            listOf(DeviceProfiles.pixel8, DeviceProfiles.foldInner, DeviceProfiles.landscapeSideNav).map {
                arrayOf(it)
            }
    }
}
