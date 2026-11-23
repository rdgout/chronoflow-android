package net.chronoflow.benchmark

import android.content.Intent
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import net.chronoflow.core.testing.BoundedProfileInput
import net.chronoflow.core.testing.LargeTimelineFixtures
import java.util.regex.Pattern

/** Day 0 of large60 opens as on iOS, whatever the date is (OQ-1e-7). The now line sits 3 h below the day's grid top. */
const val FREEZE_AT = "2026-10-13T08:00:00Z"

/** The last day of large60 (1b golden). */
const val LAST_DAY_TAG = "trip.day.day-29871120-tour-9"

const val LARGE60_OPENING_FIRST_BLOCK = "caffeine-no_caffeine-29866935-caffeine-day-0"

private val listBlockTags = Pattern.compile("trip\\.block\\..*")

fun UiDevice.topListBlock(): String? =
    findObjects(By.res(listBlockTags))
        .mapNotNull { row ->
            try {
                row.resourceName?.let { it to row.visibleBounds.top }
            } catch (_: StaleObjectException) {
                null
            }
        }.minByOrNull { it.second }
        ?.first

private const val READY_WAIT_MS = 10_000L

fun cancelStaleTouch() {
    val now = SystemClock.uptimeMillis()
    val pointer =
        MotionEvent.PointerProperties().apply {
            id = 0
            toolType = MotionEvent.TOOL_TYPE_FINGER
        }
    val cancel =
        MotionEvent.obtain(
            now,
            now,
            MotionEvent.ACTION_CANCEL,
            1,
            arrayOf(pointer),
            arrayOf(MotionEvent.PointerCoords()),
            0,
            0,
            1f,
            1f,
            0,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            MotionEvent.FLAG_CANCELED,
        )
    try {
        // Rejection is harmless when the previous run left no pointer down.
        InstrumentationRegistry.getInstrumentation().uiAutomation.injectInputEvent(cancel, false)
    } finally {
        cancel.recycle()
    }
}

fun UiDevice.toggleTripView(list: Boolean) {
    val toggle = By.res("trip.viewToggle").enabled(true)
    check(wait(Until.hasObject(toggle), READY_WAIT_MS)) { "the view toggle never became enabled" }
    checkNotNull(findObject(toggle)).click()
    val mode = if (list) "trip.mode.list" else "trip.mode.timeline"
    check(wait(Until.hasObject(By.res(mode)), READY_WAIT_MS)) { "$mode never appeared" }
}

private val large60DayTags by lazy {
    LargeTimelineFixtures.large60.detail.plan.days
        .map { "trip.day.${it.id}" }
}

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

val large60OpeningWideListTag: String by lazy {
    "trip.wide.list.${LargeTimelineFixtures.large60.detail.plan.days.first().id}"
}

fun MacrobenchmarkScope.awaitWidePresented() {
    awaitPresented()
    check(device.wait(Until.hasObject(By.res(large60OpeningWideListTag)), READY_WAIT_MS)) {
        "$large60OpeningWideListTag never appeared"
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

/** Zero-based fixture index, resolved from the topmost visible day with one accessibility query. */
fun MacrobenchmarkScope.topDayIndex(): Int = large60DayTags.indexOf(topDay())

/** Five fixed drags travel about 2,750 dp on pixel; no accessibility scroll-end event wait. */
fun UiDevice.swipeTimeline(
    bounds: Rect,
    reverse: Boolean = false,
) {
    val x = bounds.left + bounds.width() * 8 / 100
    repeat(5) {
        val start = if (reverse) 15 else 85
        val end = if (reverse) 85 else 15
        check(swipe(x, bounds.top + bounds.height() * start / 100, x, bounds.top + bounds.height() * end / 100, 26))
        waitForIdle()
    }
}

fun UiDevice.jumpToLastDay(input: BoundedProfileInput? = null) {
    fun <T> step(
        label: String,
        action: () -> T,
    ): T = if (input == null) action() else input.step(label, action)
    val entry = Pattern.compile("trip\\.dayMenu\\.day\\.\\d+")
    step("open last-day menu") { requireTag("trip.dayMenu").click() }
    step("last-day menu ready") {
        check(wait(Until.hasObject(By.res(entry)), 5000)) { "the day menu did not open" }
    }
    val menu = step("last-day menu bounds") { findObject(By.scrollable(true).hasDescendant(By.res(entry))) }
    var scrolls = 0
    while (!step("day 59 visible") { hasObject(By.res("trip.dayMenu.day.59")) } && scrolls < 12) {
        step("last-day menu scroll ${scrolls + 1}/12") {
            if (menu != null) {
                menu.scroll(Direction.DOWN, 1f)
            } else {
                swipe(displayWidth / 2, displayHeight * 9 / 10, displayWidth / 2, displayHeight / 10, 20)
            }
        }
        scrolls++
    }
    step("select day 59") {
        checkNotNull(findObject(By.res("trip.dayMenu.day.59"))) { "trip.dayMenu.day.59 not reachable" }.click()
    }
    step("last-day menu closed") {
        check(wait(Until.gone(By.res(entry)), 5000)) { "the day menu did not close" }
    }
    step("last day ready") { requireTag(LAST_DAY_TAG, 10_000) }
    val page = step("last day page") { requireTag("trip.page").also { it.setGestureMargin(displayWidth / 5) } }
    var gestures = 0
    while (!step("plan end visible") { hasObject(By.res("trip.plan.end")) } && gestures < 6) {
        step("last-day gesture ${gestures + 1}/6") { swipeTimeline(page.visibleBounds) }
        gestures++
    }
    step("last day ends") {
        check(hasObject(By.res("trip.plan.end"))) { "trip.plan.end not reached in 6 bounded gestures" }
    }
}
