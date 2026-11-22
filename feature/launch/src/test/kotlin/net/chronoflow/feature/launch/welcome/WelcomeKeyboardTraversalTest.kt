package net.chronoflow.feature.launch.welcome

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
class WelcomeKeyboardTraversalTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)
    private val expected = listOf("welcome.plan", "welcome.signIn")

    @Test fun tabMovesFromPlanToSignInAndEnterActivates() {
        var plans = 0
        var signIns = 0
        lateinit var keyboardMode: () -> Unit
        compose.setContent {
            val input = LocalInputModeManager.current
            keyboardMode = { input.requestInputMode(InputMode.Keyboard) }
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(reduceMotion = true) { WelcomeScreen(null, null, { plans++ }, { signIns++ }) }
            }
        }
        compose.runOnIdle { keyboardMode() }
        compose.onNodeWithTag(expected[0]).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithTag(expected[0]).assertIsFocused().performKeyInput { pressKey(Key.Tab) }
        compose.onNodeWithTag(expected[1]).assertIsFocused().performKeyInput { pressKey(Key.Enter) }
        assertEquals(0, plans)
        assertEquals(1, signIns)
    }
}
