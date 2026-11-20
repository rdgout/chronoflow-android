package net.chronoflow.feature.onboarding.terms

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.model.api.AccountPolicies
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TermsScreenTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private lateinit var activity: ComponentActivity

    private fun show(
        state: TermsUiState,
        onEvent: (TermsEvent) -> Unit = {},
    ) {
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) {
                    val current = LocalActivity.current as ComponentActivity
                    SideEffect { activity = current }
                    TermsScreen(state, onEvent)
                }
            }
        }
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
        compose.setContent {
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
        compose.runOnIdle {
            assertEquals(1, count)
            stay()
            assertEquals(TermsEvent.Stay, event)
        }
    }
}
