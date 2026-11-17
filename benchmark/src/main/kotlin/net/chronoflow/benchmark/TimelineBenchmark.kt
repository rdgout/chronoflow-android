package net.chronoflow.benchmark

import android.graphics.Rect
import android.util.Log
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import net.chronoflow.core.testing.tiers.Perf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/** Ports iOS `TimelinePerfTests`: fixed workloads on large60, never a traversal of hundreds of gestures. */
@OptIn(ExperimentalMetricApi::class)
@Perf
@RunWith(AndroidJUnit4::class)
class TimelineBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()

    private val packageName: String =
        InstrumentationRegistry.getArguments().getString("targetAppId") ?: "net.chronoflow.app.native"

    private val compilation = CompilationMode.Partial(BaselineProfileMode.Require)
    private val dayMenuEntry = Pattern.compile("trip\\.dayMenu\\.day\\.\\d+")

    private lateinit var pageBounds: Rect
    private var startDay: String? = null

    @Test
    fun timelineFirstFrame() =
        rule.measureRepeated(
            packageName = packageName,
            metrics =
                listOf(
                    TraceSectionMetric(
                        "cf.timeline.firstFrame",
                        TraceSectionMetric.Mode.First,
                        label = "timelineFirstFrame",
                    ),
                    StartupTimingMetric(),
                    TimelineTraceMetric(firstFrame = true),
                ),
            iterations = 10,
            compilationMode = compilation,
            startupMode = StartupMode.COLD,
            setupBlock = {
                assertAnimatorsOn()
                pressHome()
            },
        ) {
            startActivityAndWait(large60Intent(packageName))
            awaitPresented()
        }

    @Test
    fun timelineScroll() =
        rule.measureRepeated(
            packageName = packageName,
            metrics =
                listOf(
                    FrameTimingMetric(),
                    MemoryUsageMetric(MemoryUsageMetric.Mode.Max),
                    TraceSectionMetric(
                        "cf.timeline.placeholder",
                        TraceSectionMetric.Mode.Count,
                        label = "timelinePlaceholder",
                    ),
                    TraceSectionMetric("cf.timeline.replay", TraceSectionMetric.Mode.Max, label = "timelineReplay"),
                    TraceSectionMetric("cf.timeline.replay", TraceSectionMetric.Mode.Count, label = "timelineReplay"),
                    TraceSectionMetric("cf.timeline.targets", TraceSectionMetric.Mode.Max, label = "timelineTargets"),
                    TimelineTraceMetric(),
                ),
            iterations = 3,
            compilationMode = compilation,
            startupMode = null,
            setupBlock = {
                assertAnimatorsOn()
                killProcess()
                pressHome()
                startActivityAndWait(large60Intent(packageName))
                awaitPresented()
                pageBounds = checkNotNull(device.findObject(By.res("trip.page"))).visibleBounds
                startDay = checkNotNull(topDay())
            },
        ) {
            repeat(FLINGS) { device.swipeTimeline(pageBounds) }
            val endDay = topDay()
            Log.i("CfBench", "Timeline scroll progress after $FLINGS bounded-swipe-v2 gestures: $startDay -> $endDay")
            check(!device.hasObject(By.res("trip.plan.end"))) { "bounded-swipe-v2 reached the plan end" }
            check(endDay != null && endDay != startDay) { "15 gestures did not advance the day: $startDay -> $endDay" }
        }

    @Test
    fun timelineLastDay() =
        rule.measureRepeated(
            packageName = packageName,
            metrics = listOf(FrameTimingMetric()),
            iterations = 1,
            compilationMode = compilation,
            startupMode = null,
            setupBlock = {
                assertAnimatorsOn()
                killProcess()
                pressHome()
                startActivityAndWait(large60Intent(packageName))
                awaitPresented()
            },
        ) {
            checkNotNull(device.findObject(By.res("trip.dayMenu"))) { "trip.dayMenu is missing" }.click()
            check(device.wait(Until.hasObject(By.res(dayMenuEntry)), WAIT_MS)) { "the day menu did not open" }
            val menu = device.findObject(By.scrollable(true).hasDescendant(By.res(dayMenuEntry)))
            var scrolls = 0
            while (!device.hasObject(By.res("trip.dayMenu.day.59")) && scrolls < MENU_SCROLLS) {
                if (menu != null) {
                    menu.scroll(Direction.DOWN, 1f)
                } else {
                    val w = device.displayWidth
                    val h = device.displayHeight
                    device.swipe(w / 2, h * 9 / 10, w / 2, h / 10, 20)
                }
                scrolls++
            }
            checkNotNull(
                device.findObject(By.res("trip.dayMenu.day.59")),
            ) { "trip.dayMenu.day.59 not reachable" }.click()
            check(device.wait(Until.gone(By.res(dayMenuEntry)), WAIT_MS)) { "the day menu did not close" }
            check(device.wait(Until.hasObject(By.res(LAST_DAY_TAG)), READY_WAIT_MS)) { "$LAST_DAY_TAG never appeared" }
            val list = checkNotNull(device.findObject(By.res("trip.page"))) { "trip.page is missing" }
            list.setGestureMargin(device.displayWidth / 5)
            var flings = 0
            while (!device.hasObject(By.res("trip.plan.end")) && flings < END_FLINGS) {
                device.swipeTimeline(list.visibleBounds)
                flings++
            }
            check(device.hasObject(By.res("trip.plan.end"))) { "trip.plan.end not reached in $END_FLINGS flings" }
        }

    private companion object {
        const val FLINGS = 15
        const val MENU_SCROLLS = 12
        const val END_FLINGS = 6
        const val WAIT_MS = 5_000L
        const val READY_WAIT_MS = 10_000L
    }
}
