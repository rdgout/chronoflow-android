package net.chronoflow.feature.auth.signup

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
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
class SignUpRecreationTest : SignUpTestFixture() {
    @Test fun recreationKeepsNameEmailAndVisibilityButDropsBothPasswords() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { ProfileHost(DeviceProfiles.pixel8) { ChronoFlowTheme { SignUpRoute(vm, opener) } } }
        compose.onNodeWithTag("sign-up.name").performTextInput("Traveller")
        compose.onNodeWithTag("sign-up.email").performTextInput("new@example.com")
        compose.onNodeWithTag("sign-up.password").performTextInput("Secret123!")
        compose.onNodeWithTag("sign-up.confirmation").performTextInput("Secret123!")
        compose.onNodeWithTag("sign-up.visibility").performScrollTo().performClick()
        flush()
        restoration.emulateSavedInstanceStateRestore()
        flush()
        assertEquals("Traveller", model.name.value)
        assertEquals("new@example.com", model.email.value)
        assertTrue(model.showsPassword.value)
        assertEquals(
            "Traveller",
            compose
                .onNodeWithTag("sign-up.name")
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .text,
        )
        assertEquals(
            "new@example.com",
            compose
                .onNodeWithTag("sign-up.email")
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .text,
        )
        assertEquals(
            0,
            compose
                .onNodeWithTag(
                    "sign-up.password",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .length,
        )
        assertEquals(
            0,
            compose
                .onNodeWithTag(
                    "sign-up.confirmation",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .length,
        )
    }
}
