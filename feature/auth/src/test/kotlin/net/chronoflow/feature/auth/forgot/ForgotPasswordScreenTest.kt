package net.chronoflow.feature.auth.forgot

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.auth.model.AuthFormModel
import net.chronoflow.feature.auth.model.AuthFormState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ForgotPasswordScreenTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private val state = mutableStateOf(ForgotPasswordUiState())
    private val email = TextFieldState()

    private fun host(onEvent: (ForgotPasswordEvent) -> Unit = {}) {
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(darkTheme = false) { ForgotPasswordScreen(state.value, email, onEvent) }
            }
        }
    }

    @Test fun imeSendEmitsOneSubmitAndBusyDisablesTheControls() {
        var submits = 0
        host {
            if (it ==
                ForgotPasswordEvent.Submit
            ) {
                submits++
                state.value =
                    state.value.copy(form = AuthFormState(pending = AuthFormModel.Action.SendResetLink))
            }
        }
        compose.onNodeWithTag("forgot.email").performTextInput("demo@example.com")
        compose.onNodeWithTag("forgot.email").performImeAction()
        compose.onNodeWithTag("forgot.email").assertIsNotEnabled()
        compose.onNodeWithTag("forgot.submit").assertIsNotEnabled().performClick()
        compose.onNodeWithTag("auth.back").assertIsNotEnabled()
        compose.onNodeWithTag("forgot.email").performSemanticsAction(SemanticsActions.OnImeAction) { it() }
        assertEquals(1, submits)
    }

    @Test fun sentNoticeReplacesTheFormAndExposesBothActions() {
        state.value = ForgotPasswordUiState(sentEmail = "submitted@example.com")
        var opens = 0
        var backs = 0
        host {
            if (it == ForgotPasswordEvent.OpenEmail) opens++
            if (it == ForgotPasswordEvent.Back) backs++
        }
        compose.onNodeWithTag("forgot.notice").assertExists()
        compose.onNodeWithTag("forgot.email").assertDoesNotExist()
        compose.onNodeWithTag("forgot.open-email").performClick()
        compose.onNodeWithTag("forgot.back").performClick()
        assertEquals(1, opens)
        assertEquals(1, backs)
    }

    @Test fun emailCarriesTheNativeEmailAddressHint() {
        host()
        assertEquals(
            ContentType.EmailAddress,
            compose
                .onNodeWithTag("forgot.email")
                .fetchSemanticsNode()
                .config[SemanticsProperties.ContentType],
        )
    }

    @Test fun sentExpiredStateRemovesTheDecorativeBadge() {
        state.value = ForgotPasswordUiState(expired = true)
        host()
        val before =
            compose
                .onNodeWithTag("auth.heading")
                .fetchSemanticsNode()
                .boundsInRoot.top
        compose.runOnIdle { state.value = state.value.copy(sentEmail = "submitted@example.com") }
        val after =
            compose
                .onNodeWithTag("auth.heading")
                .fetchSemanticsNode()
                .boundsInRoot.top
        assertEquals(64f * DeviceProfiles.pixel8.density, before - after, 0.5f)
        compose.onNodeWithTag("forgot.notice").assertExists()
    }
}
