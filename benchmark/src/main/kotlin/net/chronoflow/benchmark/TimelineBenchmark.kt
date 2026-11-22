package net.chronoflow.benchmark

import android.graphics.Rect
import android.util.Log
import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.Metric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import net.chronoflow.core.plan.drawing.TimelineTraceNames
import net.chronoflow.core.testing.BoundedProfileInput
import net.chronoflow.core.testing.tiers.Perf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Ports iOS `TimelinePerfTests`: fixed workloads on large60, never a traversal of hundreds of gestures. */
@OptIn(ExperimentalMetricApi::class)
@Perf
@RunWith(AndroidJUnit4::class)
class TimelineBenchmark {
    @get:Rule val rule = MacrobenchmarkRule()

    private val packageName: String =
        InstrumentationRegistry.getArguments().getString("targetAppId") ?: "net.chronoflow.app.native"

    private val compilation = CompilationMode.Partial(BaselineProfileMode.Require)

    private lateinit var pageBounds: Rect
    private var startDayIndex = -1
    private var startBlock: String? = null

    @Before fun clearInterruptedGesture() {
        BoundedProfileInput().use { input -> input.step("cancel stale touch") { cancelStaleTouch() } }
    }

    @Test
    fun timelineFirstFrame() =
        rule.measureRepeated(
            packageName = packageName,
            metrics =
                listOf(
                    TraceSectionMetric(
                        TimelineTraceNames.FIRST_FRAME,
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
            metrics = scrollMetrics(),
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
                startDayIndex = topDayIndex()
                check(startDayIndex == 0) { "large60 did not open on day 0: $startDayIndex" }
            },
        ) {
            repeat(FLINGS) { device.swipeTimeline(pageBounds) }
            val endDayIndex = topDayIndex()
            Log.i(
                "CfBench",
                "Timeline scroll progress after $FLINGS bounded-swipe-v3 gestures: day index $startDayIndex -> $endDayIndex",
            )
            check(endDayIndex in 35..50) { "15 gestures must end on day index 35..50: $startDayIndex -> $endDayIndex" }
            check(!device.hasObject(By.res("trip.plan.end"))) { "bounded-swipe-v3 reached the plan end" }
        }

    @After fun restoreWindowSize() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).executeShellCommand("wm size reset")
    }

    @Test fun timelineScrollWide() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            rule.measureRepeated(
                packageName = packageName,
                metrics = scrollMetrics(),
                iterations = 3,
                compilationMode = compilation,
                startupMode = null,
                setupBlock = {
                    assertAnimatorsOn()
                    device.executeShellCommand("wm size 1840x2208")
                    killProcess()
                    pressHome()
                    startActivityAndWait(large60Intent(packageName))
                    awaitWidePresented()
                    pageBounds = checkNotNull(device.findObject(By.res("trip.page"))).visibleBounds
                    startDayIndex = topDayIndex()
                    check(startDayIndex == 0) { "large60 wide did not open on day 0: $startDayIndex" }
                },
            ) {
                repeat(FLINGS) { device.swipeTimeline(pageBounds) }
                val end = topDayIndex()
                check(end > startDayIndex) { "wide timeline did not advance after $FLINGS gestures" }
                check(!device.hasObject(By.res("trip.plan.end"))) { "wide journey reached the plan end" }
            }
        } finally {
            device.executeShellCommand("wm size reset")
        }
    }

    private fun scrollMetrics(): List<Metric> =
        listOf(
            FrameTimingMetric(),
            MemoryUsageMetric(MemoryUsageMetric.Mode.Max),
            TraceSectionMetric(
                TimelineTraceNames.PLACEHOLDER,
                TraceSectionMetric.Mode.Count,
                label = "timelinePlaceholder",
            ),
            TraceSectionMetric(
                TimelineTraceNames.REPLAY,
                TraceSectionMetric.Mode.Max,
                label = "timelineReplay",
            ),
            TraceSectionMetric(
                TimelineTraceNames.REPLAY,
                TraceSectionMetric.Mode.Count,
                label = "timelineReplay",
            ),
            TraceSectionMetric(
                TimelineTraceNames.TARGETS,
                TraceSectionMetric.Mode.Max,
                label = "timelineTargets",
            ),
            TimelineTraceMetric(),
        )

    @Test
    fun listScroll() =
        rule.measureRepeated(
            packageName = packageName,
            metrics = listOf(FrameTimingMetric(), MemoryUsageMetric(MemoryUsageMetric.Mode.Max)),
            iterations = 3,
            compilationMode = compilation,
            startupMode = null,
            setupBlock = {
                assertAnimatorsOn()
                killProcess()
                pressHome()
                startActivityAndWait(large60Intent(packageName))
                awaitPresented()
                device.toggleTripView(list = true)
                check(device.wait(Until.hasObject(By.res("trip.block.$LARGE60_OPENING_FIRST_BLOCK")), READY_WAIT_MS)) {
                    "the opening list row never appeared"
                }
                val page = checkNotNull(device.findObject(By.res("trip.page")))
                page.setGestureMargin(page.visibleBounds.width() / 5)
                pageBounds = page.visibleBounds
                startBlock = checkNotNull(device.topListBlock()) { "the opening list has no block rows" }
            },
        ) {
            repeat(FLINGS) { device.swipeTimeline(pageBounds) }
            val endBlock = checkNotNull(device.topListBlock()) { "the scrolled list has no block rows" }
            check(endBlock != startBlock) { "the list did not advance after $FLINGS bounded-swipe-v3 gestures" }
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
            device.jumpToLastDay()
        }

    private companion object {
        const val FLINGS = 15
        const val READY_WAIT_MS = 10_000L
    }
}
