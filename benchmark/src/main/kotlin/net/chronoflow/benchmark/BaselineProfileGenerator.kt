package net.chronoflow.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import net.chronoflow.core.testing.tiers.Perf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Perf
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    private val packageName: String =
        InstrumentationRegistry.getArguments().getString("targetAppId") ?: "net.chronoflow.app.native"

    /** Whatever `MainActivity` shows first at this stack top; Phase 13 adds Today. */
    @Test
    fun startup() =
        rule.collect(packageName, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
        }

    @Test
    fun timelineLarge60() =
        rule.collect(packageName) {
            pressHome()
            startActivityAndWait(large60Intent(packageName))
            awaitPresented()
            val page = device.findObject(By.res("trip.page"))
            page.setGestureMargin(device.displayWidth / 5)
            repeat(15) { page.fling(Direction.DOWN) }
        }
}
