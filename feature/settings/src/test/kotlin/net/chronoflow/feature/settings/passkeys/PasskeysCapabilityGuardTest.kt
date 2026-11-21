package net.chronoflow.feature.settings.passkeys

import androidx.compose.ui.test.onNodeWithTag
import net.chronoflow.core.testing.tiers.Logic
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
class PasskeysCapabilityGuardTest : PasskeysTestHost() {
    @Test fun disabledCapabilityPopsBackWithoutComposingOrRequesting() {
        var backs = 0
        var requests = 0
        deps.security = {
            requests++
            error("Disabled capability sent a request")
        }
        route(false) { backs++ }
        compose.onNodeWithTag("settings.passkeys.screen").assertDoesNotExist()
        assertEquals(1, backs)
        assertEquals(0, requests)
    }
}
