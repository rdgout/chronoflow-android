package net.chronoflow.app.auth

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import net.chronoflow.app.MainActivity
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Test

@Smoke
class SignInKeyboardTest : ScreenTest() {
    private lateinit var activity: MainActivity

    @Test fun fieldAndSubmitStayAboveTheSoftwareKeyboard() {
        launch(
            mapOf(
                "CF_AUTH_TEST_AUDIT_SCREEN" to "sign-in",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_MOCK_DELAY" to "0",
            ),
        ).onActivity { activity = it }
        val keyboard = AuthKeyboardAssertions(compose, activity, ::waitFor)
        compose.onNodeWithTag("sign-in.email").performClick()
        keyboard.requireSoftwareKeyboard()
        keyboard.assertRevealed("sign-in.email", "sign-in.submit")
        compose.onNodeWithTag("sign-in.email").performImeAction()
        compose.onNodeWithTag("sign-in.password").assertIsFocused()
        keyboard.requireSoftwareKeyboard()
        keyboard.assertRevealed("sign-in.password", "sign-in.submit")
    }
}
