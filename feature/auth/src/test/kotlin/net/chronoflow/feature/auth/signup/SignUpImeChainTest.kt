package net.chronoflow.feature.auth.signup

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import kotlinx.coroutines.CompletableDeferred
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SignUpImeChainTest : SignUpTestFixture() {
    @Test fun nextChainsAllFieldsAndGoSubmitsOnceWhileBusyDisablesEveryControl() {
        val gate = CompletableDeferred<String>()
        identity = { gate.await() }
        host()
        compose.onNodeWithTag("sign-up.name").performTextInput("Traveller")
        compose.onNodeWithTag("sign-up.name").performImeAction()
        compose
            .onNodeWithTag("sign-up.email")
            .assertIsFocused()
            .performTextInput("new@example.com")
        compose.onNodeWithTag("sign-up.email").performImeAction()
        compose
            .onNodeWithTag(
                "sign-up.password",
            ).assertIsFocused()
            .performTextInput("NewPassword123!")
        compose.onNodeWithTag("sign-up.password").performImeAction()
        compose
            .onNodeWithTag(
                "sign-up.confirmation",
            ).assertIsFocused()
            .performTextInput("NewPassword123!")
        compose.onNodeWithTag("sign-up.confirmation").performImeAction()
        flush()
        compose.onNodeWithTag("sign-up.confirmation").performSemanticsAction(SemanticsActions.OnImeAction) { it() }
        compose.onNodeWithTag("sign-up.name").assertIsNotEnabled()
        compose.onNodeWithTag("sign-up.email").assertIsNotEnabled()
        compose.onNodeWithTag("sign-up.password").assertIsNotEnabled()
        compose.onNodeWithTag("sign-up.confirmation").assertIsNotEnabled()
        compose.onNodeWithTag("sign-up.visibility").assertIsNotEnabled()
        compose.onNodeWithTag("sign-up.consent").assertIsNotEnabled()
        compose.onNodeWithTag("sign-up.back").assertIsNotEnabled()
        compose.onNodeWithTag("sign-up.submit").assertIsNotEnabled()
        compose.onNodeWithTag("auth.back").assertIsNotEnabled()
        compose.runOnIdle { gate.complete(AUTH_DEVICE_ID) }
        flush()
        assertEquals(1, identities)
        assertEquals(1, session.accepted.size)
    }
}
