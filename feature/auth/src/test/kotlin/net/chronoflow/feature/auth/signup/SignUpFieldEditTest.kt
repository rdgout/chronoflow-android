package net.chronoflow.feature.auth.signup

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.auth.model.AuthBannerKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SignUpFieldEditTest : SignUpTestFixture() {
    @Test fun unchangedFieldCallbacksPreserveMixedFailureUntilAnEditPassword() = edit("password", "password")

    @Test fun unchangedFieldCallbacksPreserveMixedFailureUntilAnEditConfirmation() =
        edit("confirmation", "password_confirmation")

    @Test fun unchangedConfirmationPreservesServerFeedbackUntilTheValueChanges() =
        edit("confirmation", "password_confirmation")

    private fun edit(
        tag: String,
        field: String,
    ) {
        host()
        compose.onNodeWithTag("sign-up.$tag").performTextInput("same")
        mixed(field)
        compose.onNodeWithTag("sign-up.$tag").performTextReplacement("same")
        flush()
        assertEquals(AuthBannerKind.EmailTaken, model.form.state.value.banner)
        assertNotNull(model.form.state.value.fields[field])
        assertNotNull(model.form.state.value.failure)
        compose.onNodeWithTag("sign-up.$tag").performTextReplacement("different")
        flush()
        assertNull(model.form.state.value.banner)
        assertNull(model.form.state.value.fields[field])
        assertNull(model.form.state.value.failure)
    }

    @Test fun programmaticSecretCleanupPreservesTheFailure() {
        model.editName("Traveller")
        model.editEmail("new@example.com")
        host()
        compose.onNodeWithTag("sign-up.password").performTextInput("short")
        compose.onNodeWithTag("sign-up.confirmation").performTextInput("short")
        compose.onNodeWithTag("sign-up.confirmation").performImeAction()
        flush()
        assertNotNull(model.form.state.value.failure)
        assertNotNull(model.form.state.value.fields["password"])
    }
}
