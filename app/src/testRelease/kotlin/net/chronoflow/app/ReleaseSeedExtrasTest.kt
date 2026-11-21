package net.chronoflow.app

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.test.StandardTestDispatcher
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.launch.LaunchOverrides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class ReleaseSeedExtrasTest {
    @Test fun releaseIgnoresSeedExtras() {
        val intent =
            Intent()
                .putExtra("CF_AUTH_TEST_ID", "019a562b-8000-7000-8000-000000000001")
                .putExtra("CF_AUTH_TEST_SIGNED_IN", "demo")
                .putExtra("CF_UI_TEST_PROFILE", "screen")
                .putExtra("cf.passkeyCapability", "1")
        assertEquals(LaunchOverrides.None, LaunchPlatform.overrides(intent))
        assertNull(LaunchPlatform.mockSources(StandardTestDispatcher()))
        assertNull(LaunchPlatform.passwordPromptSeed)
    }
}
