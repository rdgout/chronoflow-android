package net.chronoflow.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
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
            val bounds = checkNotNull(device.findObject(By.res("trip.page"))).visibleBounds
            repeat(15) { device.swipeTimeline(bounds) }
            check(!device.hasObject(By.res("trip.plan.end"))) { "bounded-swipe-v2 reached the plan end" }
        }
}
