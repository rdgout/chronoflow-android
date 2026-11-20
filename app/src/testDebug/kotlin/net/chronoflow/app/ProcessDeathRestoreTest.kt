package net.chronoflow.app

import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
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
import kotlin.time.Duration.Companion.seconds

@Logic
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = ProcessDeathRestoreTest.RegistryApplication::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProcessDeathRestoreTest {
    @Test fun processDeathDropsTheRegistryAndClaimsAColdOverlay() = checkRestore(true)

    @Test fun recreationRestoresTheRegistryWithoutClaimingOrHoldingSplash() = checkRestore(false)

    private fun checkRestore(tamper: Boolean) {
        val app = ApplicationProvider.getApplicationContext<RegistryApplication>()
        app.graph.resetLaunchForTest()

        fun intent(profile: String) =
            Intent(app, MainActivity::class.java)
                .putExtra("CF_AUTH_TEST_ID", "0199a3c0-0000-7000-8000-000000000000")
                .putExtra("CF_UI_TEST_PROFILE", profile)
                .putExtra("cf.mode", "ready")
        val first =
            Robolectric
                .buildActivity(MainActivity::class.java, intent("screen"))
                .create()
        first.get().savedStateRegistry.registerSavedStateProvider("cf.test") { Bundle().apply { putInt("v", 1) } }
        val bundle = Bundle()
        first
            .saveInstanceState(bundle)
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
            app.graph.appScope.cancel()
            runTest(app.mainDispatcher, timeout = 60.seconds) { app.graph.close() }
        }
    }

    class RegistryApplication : ChronoFlowApp() {
        // Registry restoration does not need launch readiness or foreground synchronization.
        val mainDispatcher = StandardTestDispatcher()

        override fun createGraph(): AppGraph =
            AppGraph(this, overrides = GraphOverrides(dispatchers = AppDispatchers(main = mainDispatcher)))
    }
}
