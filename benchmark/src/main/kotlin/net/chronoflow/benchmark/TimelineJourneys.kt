package net.chronoflow.benchmark

import android.content.Intent
import android.graphics.Rect
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

/** Day 0 of large60 opens as on iOS, whatever the date is (OQ-1e-7). The now line sits 3 h below the day's grid top. */
const val FREEZE_AT = "2026-10-13T08:00:00Z"

/** The last day of large60 (1b golden). */
const val LAST_DAY_TAG = "trip.day.day-29871120-tour-9"

private const val READY_WAIT_MS = 10_000L

fun large60Intent(pkg: String): Intent =
    Intent()
        .setClassName(pkg, "net.chronoflow.app.MainActivity")
        .putExtra("cf.trip-page", "large60")
        .putExtra("cf.freezeAt", FREEZE_AT)

/** iOS `TimelinePerfTests.waitForReady`: the ready day nodes, then the presented marker, 10 s each. */
fun MacrobenchmarkScope.awaitPresented() {
    check(device.wait(Until.hasObject(By.res("trip.timeline.ready")), READY_WAIT_MS)) {
        "trip.timeline.ready never appeared"
    }
    check(device.wait(Until.hasObject(By.res("trip.timeline.presented")), READY_WAIT_MS)) {
        "trip.timeline.presented never appeared"
    }
}

/** A perf run with the animator scales off measures nothing (04 section 15, risk 8). */
fun MacrobenchmarkScope.assertAnimatorsOn() {
    val scale = device.executeShellCommand("settings get global animator_duration_scale").trim()
    check(scale == "1" || scale == "1.0") { "Perf runs need animator_duration_scale = 1; run make perf-timeline" }
}

/** The resource name of the topmost visible day item: one query, the Android stand-in for iOS's `trip.dayMenu` value. */
fun MacrobenchmarkScope.topDay(): String? =
    device
        .findObjects(By.res(Pattern.compile("trip\\.day\\..+")))
        .minByOrNull { it.visibleBounds.top }
        ?.resourceName

/** Bounded swipe, about 2,800 dp including inertia on pixel; no accessibility scroll-end event wait. */
fun UiDevice.swipeTimeline(bounds: Rect) {
    val x = bounds.left + bounds.width() * 8 / 100
    check(swipe(x, bounds.top + bounds.height() * 85 / 100, x, bounds.top + bounds.height() * 15 / 100, 26))
    waitForIdle()
}
