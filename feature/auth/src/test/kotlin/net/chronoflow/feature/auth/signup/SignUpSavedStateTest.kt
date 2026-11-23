package net.chronoflow.feature.auth.signup

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import net.chronoflow.core.designsystem.theme.ChronoFlowTheme
import net.chronoflow.core.testing.DeviceProfiles
import net.chronoflow.core.testing.compose.ProfileHost
import net.chronoflow.core.testing.tiers.Logic
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class SignUpSavedStateTest : SignUpTestFixture() {
    @Test fun savedStateContainsNeitherPasswordBeforeOrAfterSubmission() {
        val registry = SaveableStateRegistry(restoredValues = null, canBeSaved = { true })
        compose.setContent {
            ProfileHost(DeviceProfiles.pixel8) {
                ChronoFlowTheme {
                    CompositionLocalProvider(LocalSaveableStateRegistry provides registry) { SignUpRoute(vm, opener) }
                }
            }
        }
        compose.onNodeWithTag("sign-up.password").performTextInput("Secret123!")
        compose.onNodeWithTag("sign-up.confirmation").performTextInput("Secret123!")
        assertFalse(registry.performSave().toString().contains("Secret123!"))
        compose.onNodeWithTag("sign-up.confirmation").performImeAction()
        flush()
        assertFalse(registry.performSave().toString().contains("Secret123!"))
    }
}
