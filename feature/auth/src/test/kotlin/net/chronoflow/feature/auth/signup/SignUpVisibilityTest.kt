package net.chronoflow.feature.auth.signup

import android.view.View
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SignUpVisibilityTest : SignUpTestFixture() {
    @Test fun testSharedVisibilityPreservesValuesAutoFillAndNativeNameTraits() {
        host()
        compose.onNodeWithTag("sign-up.password").performTextInput("NewPassword123!")
        compose.onNodeWithTag("sign-up.confirmation").performTextInput("NewPassword123!")
        val password = compose.onNodeWithTag("sign-up.password").fetchSemanticsNode()
        val confirmation = compose.onNodeWithTag("sign-up.confirmation").fetchSemanticsNode()
        val label = visibility()[SemanticsProperties.ContentDescription]
        assertEquals(Role.Switch, visibility()[SemanticsProperties.Role])
        assertEquals(ToggleableState.Off, visibility()[SemanticsProperties.ToggleableState])
        assertTrue(password.config.contains(SemanticsProperties.Password))
        compose.onNodeWithTag("sign-up.visibility").performScrollTo().performClick()
        flush()
        val revealed = compose.onNodeWithTag("sign-up.password").fetchSemanticsNode()
        val confirmed = compose.onNodeWithTag("sign-up.confirmation").fetchSemanticsNode()
        assertEquals(password.id, revealed.id)
        assertEquals(confirmation.id, confirmed.id)
        assertEquals("NewPassword123!", revealed.config[SemanticsProperties.EditableText].text)
        assertEquals("NewPassword123!", confirmed.config[SemanticsProperties.EditableText].text)
        assertEquals(label, visibility()[SemanticsProperties.ContentDescription])
        assertEquals(ToggleableState.On, visibility()[SemanticsProperties.ToggleableState])
        compose.onNodeWithTag("sign-up.visibility").performClick()
        flush()
        assertEquals(ToggleableState.Off, visibility()[SemanticsProperties.ToggleableState])
        assertTrue(
            compose
                .onNodeWithTag("sign-up.password")
                .fetchSemanticsNode()
                .config
                .contains(SemanticsProperties.Password),
        )
        assertEquals(setOf("personName"), hints("name"))
        assertEquals(setOf(View.AUTOFILL_HINT_EMAIL_ADDRESS, "newUsername"), hints("email"))
        assertEquals(setOf("newPassword"), hints("password"))
        assertEquals(setOf("newPassword"), hints("confirmation"))
    }

    private fun visibility() = compose.onNodeWithTag("sign-up.visibility").fetchSemanticsNode().config

    private fun hints(field: String): Set<*> {
        val type = compose.onNodeWithTag("sign-up.$field").fetchSemanticsNode().config[SemanticsProperties.ContentType]
        return (
            Class
                .forName("androidx.compose.ui.autofill.ContentType_androidKt")
                .getMethod("getContentHints", ContentType::class.java)
                .invoke(null, type) as Array<*>
        ).toSet()
    }
}
