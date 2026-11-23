package net.chronoflow.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
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
            check(device.wait(Until.hasObject(By.res("launch.opening")), 5000))
            check(device.wait(Until.gone(By.res("launch.opening")), 5000))
            check(device.wait(Until.hasObject(By.res("route.welcome")), 2000))
        }

    @Test
    fun timelineLarge60() =
        rule.collect(packageName) {
            pressHome()
            startActivityAndWait(large60Intent(packageName))
            awaitPresented()
            val bounds = checkNotNull(device.findObject(By.res("trip.page"))).visibleBounds
            check(topDayIndex() == 0) { "large60 did not open on day 0" }
            repeat(15) { device.swipeTimeline(bounds) }
            check(topDayIndex() in 35..50) { "15 gestures must end on day index 35..50" }
            check(!device.hasObject(By.res("trip.plan.end"))) { "bounded-swipe-v3 reached the plan end" }
        }
}
