@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package net.chronoflow.core.ui.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import net.chronoflow.core.designsystem.component.CfSecureTextField
import net.chronoflow.core.designsystem.component.CfTextField
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h914dp-420dpi")
class CfTextFieldTest : PrimitiveTestFixture() {
    @Test fun fieldOwnsTheLabelErrorAndAutofillSemantics() {
        host {
            CfTextField(
                TextFieldState(),
                stringResource(R.string.auth_email),
                testTag = "field",
                error = stringResource(R.string.auth_enter_a_valid_email_address_like_nam_166a13),
                contentType = ContentType.EmailAddress,
            )
        }
        compose
            .onNodeWithTag("field")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentType, ContentType.EmailAddress))
    }

    @Test fun typingUpdatesOnlyTheCallerOwnedStateAndHidesThePlaceholder() {
        val state = TextFieldState()
        host {
            CfTextField(
                state,
                stringResource(R.string.auth_email),
                testTag = "field",
                placeholder = stringResource(R.string.auth_email),
            )
        }
        compose.onNodeWithTag("field.placeholder", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("field").performTextInput("sample")
        compose.runOnIdle { assertEquals("sample", state.text.toString()) }
        compose.onNodeWithTag("field.placeholder", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun supportingHintIsAReadableSiblingOutsideTheTouchArea() {
        host {
            CfTextField(
                TextFieldState(),
                stringResource(R.string.auth_email),
                testTag = "field",
                hint = stringResource(R.string.auth_enter_your_email_and_we_ll_send_you_bf365b),
            )
        }
        val field = compose.onNodeWithTag("field").fetchSemanticsNode()
        val hint = compose.onNodeWithTag("field.hint").fetchSemanticsNode()
        assertTrue(hint.config.contains(SemanticsProperties.Text))
        assertTrue(hint.boundsInRoot.top > field.boundsInRoot.bottom)
        assertEquals(48f * compose.density.density, field.boundsInRoot.height, 1f)
        assertFalse(field.config.contains(SemanticsProperties.Error))
    }

    @Test fun secureFieldObfuscatesTypedInputAndRevealsOnlyWhenRequested() {
        val state = TextFieldState()
        var revealed by mutableStateOf(false)
        host {
            CfSecureTextField(
                state,
                stringResource(R.string.auth_password),
                testTag = "secret",
                revealed = revealed,
            )
        }
        compose.onNodeWithTag("secret").performTextInput("sample")
        compose.mainClock.advanceTimeBy(2000)
        val hidden =
            compose
                .onNodeWithTag("secret")
                .textLayout()
                .layoutInput.text.text
        assertEquals(state.text.length, hidden.length)
        assertTrue(hidden.all { it == '\u2022' })
        compose.runOnIdle { revealed = true }
        val visible =
            compose
                .onNodeWithTag("secret")
                .textLayout()
                .layoutInput.text.text
        assertTrue(visible.contentEquals(state.text))
    }
}
