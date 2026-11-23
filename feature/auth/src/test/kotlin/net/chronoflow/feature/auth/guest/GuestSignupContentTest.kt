package net.chronoflow.feature.auth.guest

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import net.chronoflow.feature.auth.form.AuthPasswordPolicy
import net.chronoflow.feature.auth.model.AuthConsentText
import net.chronoflow.feature.auth.model.AuthFormModel
import net.chronoflow.feature.auth.model.AuthFormState
import net.chronoflow.feature.auth.model.GuestSignupState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class GuestSignupContentTest {
    private val profile = DeviceProfiles.pixel8
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(profile)).around(compose)
    private var state by mutableStateOf(GuestSignupUiState())
    private val events = mutableListOf<GuestSignupEvent>()
    private val fields = GuestSignupFields()

    private fun host() {
        compose.setContent {
            ProfileHost(
                profile,
            ) { ChronoFlowTheme { GuestSignupSheetFrame(state, fields = fields, onEvent = events::add) } }
        }
    }

    @Test fun readyHasNoBannerAndFourFields() {
        host()
        compose.onNodeWithTag("guest-signup.name").assertExists()
        compose.onNodeWithTag("guest-signup.email").assertExists()
        compose.onNodeWithTag("guest-signup.password").assertExists()
        compose.onNodeWithTag("guest-signup.confirmation").assertExists()
        compose.onNodeWithTag("auth.banner").assertDoesNotExist()
        compose.onNodeWithTag("guest-signup.visibility").assertDoesNotExist()
    }

    @Test fun failedAndRetryShowTheirBanners() {
        state = state.copy(guestState = GuestSignupState.Failed)
        host()
        compose.onNodeWithTag("auth.banner").assertExists()
        compose.onNodeWithTag("auth.banner.action").assertDoesNotExist()
        compose.runOnIdle { state = state.copy(guestState = GuestSignupState.Retry) }
        compose.onNodeWithTag("auth.banner").assertExists()
        compose.onNodeWithTag("auth.banner.action").assertDoesNotExist()
    }

    @Test fun reviewShowsTheActionAndDisablesSubmit() {
        state = state.copy(guestState = GuestSignupState.Review)
        host()
        compose.onNodeWithTag("auth.banner.action").performScrollTo().performClick()
        assertEquals(listOf(GuestSignupEvent.Review), events)
        compose.onNodeWithTag("guest-signup.submit").assertIsNotEnabled()
    }

    @Test fun passwordHintIsTheServerPolicy() {
        host()
        compose.onNodeWithTag("guest-signup.password.hint").assertExists()
        assertTrue(AuthPasswordPolicy.HINT_KEY.startsWith("Use at least 12 characters"))
    }

    @Test fun consentUsesTheGuestKey() {
        host()
        compose.onNodeWithTag("guest-signup.consent").performScrollTo().assertExists()
        assertEquals(R.string.auth_by_creating_an_account_you_accept_th_b18f33, GuestSignupCopy.consentResource)
        val text = RuntimeEnvironment.getApplication().getString(GuestSignupCopy.consentResource)
        assertEquals(2, AuthConsentText.parse(text).count { it is AuthConsentText.Segment.Link })
    }

    @Test fun busyDisablesEverything() {
        state =
            state.copy(
                form = AuthFormState(pending = AuthFormModel.Action.Register),
                guestState = GuestSignupState.Review,
            )
        host()
        compose.onNodeWithTag("guest-signup.name").assertIsNotEnabled()
        compose.onNodeWithTag("guest-signup.email").assertIsNotEnabled()
        compose.onNodeWithTag("guest-signup.password").assertIsNotEnabled()
        compose.onNodeWithTag("guest-signup.confirmation").assertIsNotEnabled()
        compose.onNodeWithTag("guest-signup.submit").assertIsNotEnabled()
        compose.onNodeWithTag("guest-signup.sign-in").assertIsNotEnabled()
        compose.onNodeWithTag("guest-signup.consent").assertIsNotEnabled()
        compose.onNodeWithTag("auth.banner.action").assertIsNotEnabled()
    }

    @Test fun goOnConfirmationSubmits() {
        host()
        compose.onNodeWithTag("guest-signup.confirmation").performScrollTo().performImeAction()
        assertEquals(listOf(GuestSignupEvent.Submit), events)
    }
}
