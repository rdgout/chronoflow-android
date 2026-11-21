package net.chronoflow.feature.settings.security

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import net.chronoflow.core.navigation.Route
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.settings.support.SettingsScreenTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SecurityScreenTest : SettingsScreenTest() {
    @Test fun rowsNavigateToPasswordAndTwoFactor() {
        val events = mutableListOf<SecurityEvent>()
        show {
            SecurityScreen(
                SecurityScreenState(SecuritySummary(false, null, 2), passkeysEnabled = true),
                events::add,
            )
        }
        compose.onNodeWithTag("settings.security.passkeys").assertExists()
        compose.onNodeWithTag("settings.security.password").performClick()
        compose.onNodeWithTag("settings.security.twoFactor").performClick()
        assertEquals(
            listOf(SecurityEvent.Navigate(Route.Password), SecurityEvent.Navigate(Route.TwoFactorSettings)),
            events,
        )
    }

    @Test fun missingCountHidesPasskeys() {
        show { SecurityScreen(SecurityScreenState(SecuritySummary(false, null, null), passkeysEnabled = true), {}) }
        compose.onNodeWithTag("settings.security.passkeys").assertDoesNotExist()
    }

    @Test fun releaseHidesPasskeys() {
        show { SecurityScreen(SecurityScreenState(SecuritySummary(true, null, 2)), {}) }
        compose.onNodeWithTag("settings.security.passkeys").assertDoesNotExist()
        compose.onNodeWithTag("settings.security.twoFactor").assertExists()
    }

    @Test fun missingSecurityShowsStoredState() {
        show { SecurityScreen(SecurityScreenState(loaded = true), {}) }
        compose.onNodeWithTag("settings.missing").assertExists()
    }
}
