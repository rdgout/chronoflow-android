package net.chronoflow.benchmark

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import net.chronoflow.core.testing.tiers.Perf
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Perf
@RunWith(AndroidJUnit4::class)
class ProcessDeathLaunchTest {
    @Test fun killedProcessColdLaunchesWithTheSplash() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val pkg = InstrumentationRegistry.getArguments().getString("targetAppId") ?: "net.chronoflow.app.native"

        fun start() {
            device.executeShellCommand("monkey -p $pkg -c android.intent.category.LAUNCHER 1")
        }
        start()
        assertTrue(device.wait(Until.hasObject(By.res("app.root")), 3000))
        val before = device.executeShellCommand("pidof $pkg").trim()
        assertTrue(before.isNotEmpty())
        device.pressHome()
        device.executeShellCommand("am kill $pkg")
        val killed =
            device
                .executeShellCommand(
                    "sh -c 'for i in $(seq 20); do pidof $pkg >/dev/null || { echo killed; exit 0; }; sleep 0.25; done; exit 1'",
                ).trim()
        assertTrue("process stayed alive: $before", killed == "killed")
        start()
        assertTrue(device.wait(Until.hasObject(By.res("launch.opening")), 3000))
        val after = device.executeShellCommand("pidof $pkg").trim()
        assertTrue(after.isNotEmpty())
        assertNotEquals(before, after)
    }
}
