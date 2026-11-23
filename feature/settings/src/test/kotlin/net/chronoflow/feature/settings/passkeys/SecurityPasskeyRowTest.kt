package net.chronoflow.feature.settings.passkeys

import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.security.SecurityScreen
import net.chronoflow.feature.settings.security.SecurityScreenState
import net.chronoflow.feature.settings.security.SecuritySummary
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SecurityPasskeyRowTest : SettingsScreenTest() {
    @Test fun rowIsAbsentWhileTheCapabilityIsOff() {
        show { SecurityScreen(SecurityScreenState(SecuritySummary(false, null, 2), passkeysEnabled = false), {}) }
        compose.onNodeWithTag("settings.security.passkeys").assertDoesNotExist()
    }

    @Test fun rowIsAbsentWhenTheServerOmitsTheCount() {
        show { SecurityScreen(SecurityScreenState(SecuritySummary(false, null, null), passkeysEnabled = true), {}) }
        compose.onNodeWithTag("settings.security.passkeys").assertDoesNotExist()
    }

    @Test fun rowShowsThePluralCount() {
        show { SecurityScreen(SecurityScreenState(SecuritySummary(false, null, 2), passkeysEnabled = true), {}) }
        compose.onNodeWithTag("settings.security.passkeys").assertExists()
    }
}
