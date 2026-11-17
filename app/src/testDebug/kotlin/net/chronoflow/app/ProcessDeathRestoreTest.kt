package net.chronoflow.app

import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import net.chronoflow.core.testing.tiers.Logic
import net.chronoflow.feature.launch.LaunchController
import net.chronoflow.feature.launch.model.LaunchKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProcessDeathRestoreTest {
    @Test fun processDeathDropsTheRegistryAndClaimsAColdOverlay() = checkRestore(true)

    @Test fun recreationRestoresTheRegistryWithoutClaimingOrHoldingSplash() = checkRestore(false)

    private fun checkRestore(tamper: Boolean) {
        val app = ApplicationProvider.getApplicationContext<ChronoFlowApp>()
        app.graph.resetLaunchForTest()

        fun intent(profile: String) =
            Intent(app, MainActivity::class.java)
                .putExtra("CF_AUTH_TEST_ID", "0199a3c0-0000-7000-8000-000000000000")
                .putExtra("CF_UI_TEST_PROFILE", profile)
        val first =
            Robolectric
                .buildActivity(MainActivity::class.java, intent("screen"))
                .create()
                .start()
                .resume()
        first.get().savedStateRegistry.registerSavedStateProvider("cf.test") { Bundle().apply { putInt("v", 1) } }
        val bundle = Bundle()
        first
            .saveInstanceState(bundle)
            .pause()
            .stop()
            .destroy()
        if (tamper) bundle.putString(LaunchController.KEY_PROCESS_TOKEN, "other")
        app.graph.resetLaunchForTest()
        val second = Robolectric.buildActivity(MainActivity::class.java, intent("launch")).create(bundle)
        try {
            val launch = app.graph.launch
            val restored = second.get().savedStateRegistry.consumeRestoredStateForKey("cf.test")
            if (tamper) {
                assertEquals(LaunchKind.ProcessDeathRestore, launch.lastLaunchKind)
                assertNull(restored)
                assertTrue(launch.overlayClaimedByCurrentActivity)
            } else {
                assertEquals(LaunchKind.Recreated, launch.lastLaunchKind)
                assertEquals(1, restored?.getInt("v"))
                assertFalse(launch.overlayClaimedByCurrentActivity)
                assertFalse(launch.holdsSystemSplash())
            }
        } finally {
            second.destroy()
        }
    }
}
