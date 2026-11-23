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
        val pkg = InstrumentationRegistry.getArguments().getString("targetAppId") ?: "net.chronoflow.app"

        fun start() {
            val result =
                device.executeShellCommand(
                    "am start -W -n $pkg/net.chronoflow.app.MainActivity " +
                        "-a android.intent.action.MAIN -c android.intent.category.LAUNCHER --es cf.reduceMotion false",
                )
            assertTrue(result, "Status: ok" in result)
        }
        start()
        assertTrue("missing app.root in $pkg", device.wait(Until.hasObject(By.res("app.root")), 3000))
        val before = device.executeShellCommand("pidof $pkg").trim()
        assertTrue(before.isNotEmpty())
        device.pressHome()
        assertTrue("app stayed in foreground", device.wait(Until.gone(By.pkg(pkg)), 3000))
        device.executeShellCommand("am kill $pkg")
        assertTrue(
            "process stayed alive: $before",
            device.wait({ it.executeShellCommand("pidof $pkg").isBlank() }, 5000),
        )
        start()
        assertTrue(device.wait(Until.hasObject(By.res("launch.opening")), 3000))
        val after = device.executeShellCommand("pidof $pkg").trim()
        assertTrue(after.isNotEmpty())
        assertNotEquals(before, after)
    }
}
