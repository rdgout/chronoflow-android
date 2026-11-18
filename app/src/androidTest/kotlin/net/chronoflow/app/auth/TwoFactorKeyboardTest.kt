package net.chronoflow.app.auth

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.app.MainActivity
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Test

@Smoke
@OptIn(ExperimentalTestApi::class)
class TwoFactorKeyboardTest : ScreenTest() {
    private lateinit var activity: MainActivity

    private fun checkKeyboard(recovery: Boolean) {
        launch(twoFactorScreenSeed).onActivity { activity = it }
        compose.waitUntilExactlyOneExists(hasTestTag("two-factor.code"), 5_000)
        if (recovery) compose.onNodeWithTag("two-factor.toggle").performClick()
        val field = if (recovery) "two-factor.recovery_code" else "two-factor.code"
        compose.onNodeWithTag(field).performClick()
        val keyboard = AuthKeyboardAssertions(compose, activity, ::waitFor)
        keyboard.requireSoftwareKeyboard()
        keyboard.assertRevealed(field, "two-factor.submit")
    }

    @Test fun codeVariantKeepsFieldAndVerifyAboveTheKeyboard() = checkKeyboard(false)

    @Test fun recoveryVariantKeepsFieldAndVerifyAboveTheKeyboard() = checkKeyboard(true)
}
