package net.chronoflow.feature.auth.signin

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Layout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Layout
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SignInKeyboardTraversalTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private val expected =
        listOf(
            "sign-in.email",
            "sign-in.password",
            "sign-in.submit",
            "sign-in.forgot",
            "sign-in.signup",
            "sign-in.guest",
        )
    private val events = mutableListOf<SignInEvent>()

    private fun host() {
        val email = TextFieldState()
        val password = TextFieldState()
        lateinit var keyboardMode: () -> Unit
        compose.setContent {
            val input = LocalInputModeManager.current
            keyboardMode = { input.requestInputMode(InputMode.Keyboard) }
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) {
                    SignInContent(SignInUiState(), email, password, events::add)
                }
            }
        }
        compose.runOnIdle { keyboardMode() }
    }

    @Test fun tabFollowsTheFieldsAndActionsToTheGuestFooter() {
        host()
        compose.onNodeWithTag(expected[0]).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        tab(0)
        tab(1)
        tab(2)
        tab(3)
        tab(4)
        assertEquals(emptyList<SignInEvent>(), events)
    }

    @Test fun enterOnSubmitSendsThePasswordAction() {
        host()
        compose.onNodeWithTag("sign-in.submit").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithTag("sign-in.submit").performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf(SignInEvent.Password), events)
    }

    private fun tab(index: Int) {
        compose.onNodeWithTag(expected[index]).assertIsFocused().performKeyInput { pressKey(Key.Tab) }
        compose.onNodeWithTag(expected[index + 1]).assertIsFocused()
    }
}
