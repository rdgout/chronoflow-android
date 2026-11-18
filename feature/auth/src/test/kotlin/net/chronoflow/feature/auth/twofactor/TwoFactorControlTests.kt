package net.chronoflow.feature.auth.twofactor

import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import net.chronoflow.core.model.DataFailure
import net.chronoflow.core.model.FieldMessage
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.auth.model.AuthBannerKind
import net.chronoflow.feature.auth.model.AuthFormModel
import net.chronoflow.feature.auth.model.SignInModel
import net.chronoflow.feature.auth.model.TwoFactorModel
import net.chronoflow.feature.auth.testing.AUTH_DEVICE_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class TwoFactorControlTests : TwoFactorTestFixture() {
    @Test fun displayedCodeRejectsOverflowAndNonDigitsEvenWhenTheModelDoesNotChange() {
        host()
        val field = compose.onNodeWithTag(tag).performClick()
        field.performTextInput("a٣")
        flush()
        assertEquals("", fieldValue())
        assertEquals("", model.code)
        field.performTextInput("12b٣345678")
        flush()
        assertEquals(
            "123456",
            fieldValue(),
        )
        assertEquals(
            "123456",
            model.code,
        )
        field.performTextInput("7")
        flush()
        assertEquals(
            "123456",
            fieldValue(),
        )
        assertEquals(
            "123456",
            model.code,
        )
        field.performTextInput("x")
        flush()
        assertEquals(
            "123456",
            fieldValue(),
        )
        assertEquals(
            "123456",
            model.code,
        )
        field.assertIsFocused()
        assertFalse(model.form.busy)
    }

    @Test fun nativeInputTraitsAndModeToggleDiscardTheOldKeyboardConfiguration() {
        host()
        compose
            .onNodeWithTag(tag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentType,
                    ContentType.SmsOtpCode,
                ),
            ).assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ImeAction,
                    androidx.compose.ui.text.input.ImeAction.Go,
                ),
            ).performTextInput("123")
        flush()
        compose.onNodeWithTag("two-factor.toggle").performScrollTo().performClick()
        flush()
        compose.onNodeWithTag("two-factor.code").assertDoesNotExist()
        compose
            .onNodeWithTag(tag)
            .assertExists()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentType))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ImeAction,
                    androidx.compose.ui.text.input.ImeAction.Go,
                ),
            )
        assertEquals("", fieldValue())
        assertEquals("", model.code)
    }

    @Test fun recoveryPlaceholderSurvivesErrorsAndClearsWhenReturningToCode() {
        host(true)
        compose.runOnIdle {
            model.form.receive(
                DataFailure.Validation(
                    mapOf(model.field to FieldMessage("Please fill this in.")),
                    422,
                ),
            )
        }
        flush()
        compose.onNodeWithTag("two-factor.recovery_code.placeholder", useUnmergedTree = true).assertExists()
        assertEquals("", fieldValue())
        compose.onNodeWithTag("two-factor.toggle").performScrollTo().performClick()
        flush()
        compose.onNodeWithTag("two-factor.code.placeholder", useUnmergedTree = true).assertDoesNotExist()
        compose
            .onNodeWithTag(
                "two-factor.recovery_code.placeholder",
                useUnmergedTree = true,
            ).assertDoesNotExist()
        assertEquals("", fieldValue())
    }

    private fun failedAttempts(recovery: Boolean) {
        host(recovery)
        compose.onNodeWithTag(tag).performClick()
        failNext(DataFailure.Server(503))
        compose.onNodeWithTag("two-factor.submit").performScrollTo().performClick()
        flush()
        compose.onNodeWithTag(tag).assertIsEnabled().assertIsNotFocused()
        assertEquals(AuthBannerKind.Server, model.form.state.value.banner)
        failNext(DataFailure.Validation(mapOf(model.field to FieldMessage("Please fill this in.")), 422))
        compose.onNodeWithTag("two-factor.submit").performScrollTo().performClick()
        flush()
        compose
            .onNodeWithTag(tag)
            .assertIsEnabled()
            .assertIsFocused()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        assertNotNull(model.form.state.value.fields[model.field])
    }

    @Test fun failedAttemptsRefocusTheEnabledFieldWithoutEditingAwayFeedbackCode() = failedAttempts(false)

    @Test fun failedAttemptsRefocusTheEnabledFieldWithoutEditingAwayFeedbackRecovery() = failedAttempts(true)

    private fun followsModel(recovery: Boolean) {
        host(recovery)
        val first = if (recovery) "wrong-saved-code" else "123456"
        val other = if (recovery) "OtherCode-123" else "654321"
        compose.onNodeWithTag(tag).performTextInput(first)
        flush()
        assertEquals(first, fieldValue())
        assertEquals(first, model.code)
        compose.runOnIdle { model.editCode(other) }
        flush()
        assertEquals(other, fieldValue())
        compose.onNodeWithTag("two-factor.submit").performScrollTo().performClick()
        flush()
        assertEquals("", fieldValue())
        assertEquals("", model.code)
        compose.onNodeWithTag(tag).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        assertNotNull(model.form.state.value.fields[model.field])
        assertNull(model.form.state.value.banner)
    }

    @Test fun bothDisplayedProofsFollowModelEditsAndAttemptClearingWithoutErasingErrorsCode() = followsModel(false)

    @Test fun bothDisplayedProofsFollowModelEditsAndAttemptClearingWithoutErasingErrorsRecovery() = followsModel(true)

    private fun busy(language: String) {
        RuntimeEnvironment.setQualifiers(language)
        host()
        val idle =
            compose
                .onNodeWithTag("two-factor.submit")
                .fetchSemanticsNode()
                .boundsInRoot.size
        gate.armed = true
        compose.onNodeWithTag("two-factor.submit").performScrollTo().performClick()
        flush()
        assertTrue(model.form.busy)
        val pending =
            compose
                .onNodeWithTag("two-factor.submit")
                .fetchSemanticsNode()
                .boundsInRoot.size
        assertEquals(idle.width, pending.width, 0.5f)
        assertEquals(idle.height, pending.height, 0.5f)
        compose.onNodeWithTag(tag).assertIsNotEnabled()
        compose.onNodeWithTag("two-factor.submit").assertIsNotEnabled()
        compose.onNodeWithTag("two-factor.toggle").assertIsNotEnabled()
        compose.onNodeWithTag("auth.back").assertIsEnabled()
    }

    @Test fun verifyKeepsItsLabelAndSizeWhileBusyInBothLanguagesEn() = busy("en")

    @Test fun verifyKeepsItsLabelAndSizeWhileBusyInBothLanguagesNl() = busy("nl")
}
