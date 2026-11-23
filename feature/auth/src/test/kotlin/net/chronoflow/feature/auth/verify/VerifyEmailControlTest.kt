package net.chronoflow.feature.auth.verify

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class VerifyEmailControlTest : VerifyTestFixture() {
    @Test fun testWaitingCountdownAndFailedHaveNoInputsAndKeepLargeActions() {
        host()
        compose.onNodeWithTag("verify.open-email").assertExists()
        compose.onNodeWithTag("verify.resend").assertExists()
        compose.onNodeWithTag("verify.different-email").assertExists()
        compose.onNodeWithTag("auth.back").assertExists()
        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetText)).assertCountEquals(0)
        compose.onNodeWithTag("verify.countdown").assertDoesNotExist()
        act { model.resend() }
        compose.onNodeWithTag("verify.countdown").assertExists()
        compose.onNodeWithTag("verify.resend").assertIsNotEnabled()
        act {
            model.receiveLink(null)
            model.activate()
        }
        compose.onNodeWithTag("verify.retry").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("verify.resend").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("verify.open-email").assertDoesNotExist()
        compose.onNodeWithTag("verify.different-email").assertDoesNotExist()
        compose.onNodeWithTag("verify.countdown").assertDoesNotExist()
    }
}
