package net.chronoflow.feature.onboarding.terms

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.compose.warmNativeTextRuntime
import net.chronoflow.core.testing.tiers.Logic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TermsScreenTest {
    private val compose = createEmptyComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    private lateinit var activity: ComponentActivity
    private lateinit var host: ActivityController<ComponentActivity>

    @Before fun createHost() {
        host = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        activity = host.get()
    }

    @After fun closeHost() {
        host.pause().stop().destroy()
    }

    private fun show(
        state: TermsUiState,
        onEvent: (TermsEvent) -> Unit = {},
    ) {
        activity.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) {
                    TermsScreen(state, onEvent)
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun policyRowOpensTheTermsCustomTab() {
        show(TermsUiState(policies = AccountPolicies("1", "1")))
        compose.onNodeWithTag(TermsTags.POLICY).performScrollTo().performClick()
        assertEquals("https://chronoflow.net/terms", shadowOf(activity).nextStartedActivity.dataString)
    }

    @Test fun policyRowStaysEnabledWhileFailed() {
        show(TermsUiState(failed = true))
        compose.onNodeWithTag(TermsTags.FAILURE).assertExists()
        compose.onNodeWithTag(TermsTags.POLICY).performScrollTo().assertIsEnabled()
    }

    @Test fun signOutFailureAndDisabledButtonsFollowTheState() {
        show(TermsUiState(signOutBusy = true, signOutFailed = true))
        compose.onNodeWithTag(TermsTags.SIGN_OUT_FAILURE).assertExists()
        compose.onNodeWithTag(TermsTags.ACCEPT).assertIsNotEnabled()
        compose.onNodeWithTag(TermsTags.SIGN_OUT).assertIsNotEnabled()
    }

    @Test fun needsConfirmationInvokesTheSlotWithTheCount() {
        var count = -1
        var stay: () -> Unit = {}
        var event: TermsEvent? = null
        activity.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) {
                    TermsScreen(TermsUiState(pendingSignOutCount = 1), { event = it }, signOutConfirmation = {
                        n,
                        onStay,
                        _,
                        ->
                        SideEffect {
                            count = n
                            stay = onStay
                        }
                    })
                }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, count)
            stay()
            assertEquals(TermsEvent.Stay, event)
        }
    }

    companion object {
        private val fixtureReady by lazy {
            warmNativeTextRuntime(TermsScreenTest::class.java, 36)
        }

        @BeforeClass
        @JvmStatic
        fun warmFixtureOncePerJvm() {
            // Load the native text runtime before the first case resolves its screen fonts.
            fixtureReady
        }
    }
}
