package net.chronoflow.feature.auth.signin

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.auth.testing.AuthTestFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SignInSecretsTest : SignInTestFixture() {
    @Test fun recreationKeepsTheEmailAndDropsThePassword() {
        val fixture = AuthTestFixture(dispatcher)
        val vm = vm(fixture)
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            ProfileHost(
                DeviceProfiles.pixel8,
            ) { ChronoFlowTheme(darkTheme = false) { SignInScreen(vm, fixture.routes) } }
        }
        compose.onNodeWithTag("sign-in.email").performTextInput("demo@example.com")
        compose.onNodeWithTag("sign-in.password").performTextInput("Secret123!")
        restoration.emulateSavedInstanceStateRestore()
        flush()
        assertEquals("demo@example.com", fixture.model.email.value)
        assertEquals(
            "demo@example.com",
            compose
                .onNodeWithTag("sign-in.email")
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .text,
        )
        assertEquals(
            0,
            compose
                .onNodeWithTag(
                    "sign-in.password",
                ).fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .length,
        )
    }

    @Test fun nothingSavedContainsASecret() {
        val fixture = AuthTestFixture(dispatcher, twoFactor = true, password = "Secret123!")
        val vm = vm(fixture)
        val registry = SaveableStateRegistry(restoredValues = null, canBeSaved = { true })
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme(darkTheme = false) {
                    CompositionLocalProvider(
                        LocalSaveableStateRegistry provides registry,
                    ) { SignInScreen(vm, fixture.routes) }
                }
            }
        }
        compose.onNodeWithTag("sign-in.email").performTextInput("demo@example.com")
        compose.onNodeWithTag("sign-in.password").performTextInput("Secret123!")
        compose.onNodeWithTag("sign-in.password").performImeAction()
        flush()
        val challenge = fixture.model.navigation.challengeID
        assertNotNull(challenge)
        val saved = registry.performSave().toString()
        assertFalse(saved.contains("Secret123!"))
        assertFalse(saved.contains(challenge!!))
    }
}
