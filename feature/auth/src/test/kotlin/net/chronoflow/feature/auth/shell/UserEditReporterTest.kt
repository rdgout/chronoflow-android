package net.chronoflow.feature.auth.shell

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import net.chronoflow.core.designsystem.component.CfTextField
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.compose.ProfileRule
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
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
class UserEditReporterTest {
    private val compose = createComposeRule()

    @get:Rule val rules: RuleChain = RuleChain.outerRule(ProfileRule(DeviceProfiles.pixel8)).around(compose)

    @Test fun onlyChangedUserInputReportsAnEdit() {
        val state = TextFieldState()
        val edits = mutableListOf<String>()
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(darkTheme = false) {
                    CfTextField(
                        state,
                        stringResource(R.string.auth_email),
                        testTag = "field",
                        inputTransformation =
                            UserEditReporter {
                                edits +=
                                    it
                            },
                    )
                }
            }
        }
        compose.onNodeWithTag("field").performTextInput("a")
        compose.onNodeWithTag("field").performTextInput("b")
        assertEquals(listOf("a", "ab"), edits)
        compose.onNodeWithTag("field").performTextReplacement("ab")
        assertEquals(listOf("a", "ab"), edits)
        compose.runOnIdle { state.clearText() }
        assertEquals(listOf("a", "ab"), edits)
    }
}
