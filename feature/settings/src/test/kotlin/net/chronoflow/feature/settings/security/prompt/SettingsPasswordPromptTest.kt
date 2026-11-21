package net.chronoflow.feature.settings.security.prompt

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import net.chronoflow.core.designsystem.theme.CfTheme
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.security.SettingsSecurityPrompt
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsPasswordPromptTest : SettingsScreenTest() {
    private val password = TextFieldState()
    private var now = Instant.parse("2026-11-21T12:00:00Z")
    private var destructiveColor = Color.Unspecified
    private var submitted = 0
    private var reset = 0
    private val prefix = "settings.security.prompt"

    private fun content(
        busy: Boolean = false,
        error: String? = null,
        retryAt: Instant? = null,
        message: String? = null,
        action: SettingsSecurityPrompt = SettingsSecurityPrompt.Enable,
    ) {
        show {
            destructiveColor = CfTheme.colors.destructive
            SettingsPasswordPrompt(
                action.detail,
                action.button,
                action.destructive,
                password,
                busy,
                retryAt,
                { now },
                error,
                message,
                "rick@example.com",
                {},
                { submitted++ },
                { reset++ },
            )
        }
    }

    @Test fun emptyPasswordDisablesSubmit() {
        content()
        compose.onNodeWithTag("$prefix.submit").assertIsNotEnabled()
    }

    @Test fun checkingShowsLabelSpinnerAndDisablesFieldForgotAndCancel() {
        content(busy = true)
        compose
            .onNodeWithTag("$prefix.submit")
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription))
        compose.onNodeWithTag("$prefix.password").assertIsNotEnabled()
        compose.onNodeWithTag("$prefix.forgot").assertIsNotEnabled()
        compose.onNodeWithTag("$prefix.cancel").assertIsNotEnabled()
    }

    @Test fun wrongShowsErrorUnderTheField() {
        content(error = "The provided password was incorrect.")
        compose.onNodeWithTag("$prefix.error").assertExists()
        compose
            .onNodeWithTag("$prefix.password")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
    }

    @Test fun lockedCountsDownEverySecond() {
        content(retryAt = now.plusSeconds(2820))
        compose.onNodeWithTag("$prefix.error").assertExists()
        compose.onNodeWithTag("$prefix.password").assertIsNotEnabled()
        now = now.plusSeconds(2761)
        compose.mainClock.advanceTimeBy(1001)
        compose.onNodeWithTag("$prefix.error").assertExists()
        now = now.plusSeconds(59)
        compose.mainClock.advanceTimeBy(1001)
        compose.onNodeWithTag("$prefix.error").assertDoesNotExist()
        compose.onNodeWithTag("$prefix.password").assertIsEnabled()
    }

    @Test fun forgotOpensTheAlertAndSendLinkCallsReset() {
        content()
        compose.onNodeWithTag("$prefix.forgot").performClick()
        compose.onNodeWithTag("$prefix.forgotAlert").assertExists()
        compose.onNodeWithTag("cf.dialog.confirm").performClick()
        compose.runOnIdle { assertEquals(1, reset) }
    }

    @Test fun forgotIsDisabledWhileBusyOrLocked() {
        content(retryAt = now.plusSeconds(60))
        compose.onNodeWithTag("$prefix.forgot").assertIsNotEnabled()
    }

    @Test fun imeDoneSubmitsOnlyWhenEnabled() {
        content()
        compose.onNodeWithTag("$prefix.password").performImeAction()
        compose.runOnIdle { assertEquals(0, submitted) }
        compose.onNodeWithTag("$prefix.password").performTextInput("password")
        compose.onNodeWithTag("$prefix.password").performImeAction()
        compose.runOnIdle { assertEquals(1, submitted) }
    }

    @Test fun destructiveUsesDestructiveStyle() {
        password.edit { append("password") }
        content(action = SettingsSecurityPrompt.Disable)
        val image = compose.onNodeWithTag("$prefix.submit").captureToImage()
        assertEquals(destructiveColor.toArgb(), image.toPixelMap()[10, image.height / 2].toArgb())
    }

    @Test fun activationFailureShowsTheBanner() {
        content(message = "Couldn't turn on two-factor.")
        compose.onNodeWithTag("$prefix.feedback").assertExists()
    }

    @Test fun otherMessagesShowAsMutedText() {
        content(message = "Needs a connection.")
        compose.onNodeWithTag("$prefix.feedback").assertExists()
    }

    @Test
    @Config(qualifiers = "nl")
    fun dutchCopy() {
        content()
        compose.onNodeWithTag(prefix).assertExists()
        compose.onNodeWithTag("$prefix.cancel").assertExists()
        compose.onNodeWithTag("$prefix.forgot").assertExists()
    }
}
