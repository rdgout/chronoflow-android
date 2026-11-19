package net.chronoflow.feature.trip.page

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HiddenStepsButtonTest : PageComponentTest() {
    @Test fun settingsIsDisabledWithoutACallbackAndInvokesOneWhenAvailable() {
        val enabled = mutableStateOf(false)
        var clicks = 0
        host { HiddenStepsButton(if (enabled.value) ({ clicks++ }) else null) }
        compose.onNodeWithTag("trip.hiddenSteps").assertIsNotEnabled()
        compose.runOnIdle { enabled.value = true }
        compose.onNodeWithTag("trip.hiddenSteps").assertIsEnabled().performClick()
        assertEquals(1, clicks)
    }
}
