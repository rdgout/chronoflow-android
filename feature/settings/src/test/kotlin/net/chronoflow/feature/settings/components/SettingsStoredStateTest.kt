package net.chronoflow.feature.settings.components

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
class SettingsStoredStateTest : SettingsScreenTest() {
    @Test fun unloadedAccountShowsLoading() {
        show { SettingsStoredState(false, false, {}) }
        compose.onNodeWithTag("settings.loading").assertExists()
        compose.onNodeWithTag("settings.missing").assertDoesNotExist()
    }

    @Test fun loadedMissingAccountShowsRetry() {
        show { SettingsStoredState(true, false, {}) }
        compose.onNodeWithTag("settings.missing").assertExists()
        compose.onNodeWithTag("settings.loading").assertDoesNotExist()
    }

    @Test fun failedLoadCanRequestSync() {
        var requests = 0
        val requestSync = { requests++ }
        show { SettingsStoredState(false, true, { requestSync() }) }
        compose.onNodeWithTag("settings.stored.retry").performClick()
        assertEquals(1, requests)
    }
}
