@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import net.chronoflow.core.designsystem.component.CfTextField
import net.chronoflow.core.testing.tiers.Layout
import net.chronoflow.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Layout
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class CfTextFieldColorsTest(
    override val dark: Boolean,
) : PrimitiveTestFixture() {
    @Test fun normalAndErrorBordersKeepTheirTokensWithoutAFieldFill() {
        host {
            Column {
                CfTextField(TextFieldState(), stringResource(R.string.auth_email), testTag = "normal")
                CfTextField(
                    TextFieldState(),
                    stringResource(R.string.auth_email),
                    testTag = "error",
                    error = stringResource(R.string.auth_enter_a_valid_email_address_like_nam_166a13),
                )
            }
        }
        val normal = pixels("normal")
        val error = pixels("error")
        assertTokenColor(normal[normal.width / 2, 0], colors.input)
        assertTokenColor(error[error.width / 2, 0], colors.destructive)
        assertTokenColor(normal[normal.width / 2, normal.height / 2], colors.background)
    }

    @Test fun focusBorderAndOutsideOutlineUseRing() {
        host { CfTextField(TextFieldState(), stringResource(R.string.auth_email), testTag = "focused") }
        compose.onNodeWithTag("focused").performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        val field = compose.onNodeWithTag("focused").fetchSemanticsNode().boundsInRoot
        val scene = pixels("fixture")
        assertTokenColor(scene[field.center.x.toInt(), field.top.toInt()], colors.ring)
        assertTokenColor(scene[field.center.x.toInt(), (field.top - compose.density.density).toInt()], colors.ring)
    }

    @Test fun placeholderAndReadableHintUseMutedForeground() {
        host {
            CfTextField(
                TextFieldState(),
                stringResource(R.string.auth_email),
                testTag = "field",
                placeholder = stringResource(R.string.auth_email),
                hint = stringResource(R.string.auth_enter_your_email_and_we_ll_send_you_bf365b),
            )
        }
        val placeholder = compose.onNodeWithTag("field.placeholder", useUnmergedTree = true).textLayout()
        val hint = compose.onNodeWithTag("field.hint").textLayout()
        assertEquals(colors.mutedForeground, placeholder.layoutInput.style.color)
        assertEquals(colors.mutedForeground, hint.layoutInput.style.color)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun cases(): Collection<Array<Any>> = listOf(arrayOf(false), arrayOf(true))
    }
}
