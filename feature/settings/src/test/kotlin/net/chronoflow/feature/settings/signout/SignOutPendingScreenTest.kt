package net.chronoflow.feature.settings.signout

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import net.chronoflow.core.model.contracts.BlockType
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.home.PendingChange
import net.chronoflow.feature.settings.home.PendingText
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SignOutPendingScreenTest : SettingsScreenTest() {
    private val items = listOf(PendingChange("1", BlockType.AvoidLight, PendingText.CheckedOff(BlockType.AvoidLight)))

    @Test fun busyIgnoresSwipeAndBack() {
        var stays = 0
        show { SignOutPendingSheet(items, true, { stays++ }, {}) }
        compose.onNodeWithTag("settings.pending.stay").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("settings.pending.discard").assertIsNotEnabled()
        compose.onNodeWithTag("settings.pending").performTouchInput { swipeDown() }
        compose.runOnIdle {
            (
                org.robolectric.shadows.ShadowDialog
                    .getLatestDialog() as androidx.activity.ComponentDialog
            ).onBackPressedDispatcher.onBackPressed()
        }
        compose.onNodeWithTag("settings.pending").assertExists()
        assertEquals(0, stays)
        assertFalse(activity.isFinishing)
    }

    @Test fun stayClosesWithoutRemovingRows() {
        val visible = mutableStateOf(true)
        show { if (visible.value) SignOutPendingSheet(items, false, { visible.value = false }, {}) }
        compose.onNodeWithTag("settings.pending.stay").performScrollTo().performClick()
        compose.onNodeWithTag("settings.pending").assertDoesNotExist()
        assertEquals(1, items.size)
    }
}
