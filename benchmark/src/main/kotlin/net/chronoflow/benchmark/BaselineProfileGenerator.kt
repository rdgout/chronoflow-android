package net.chronoflow.benchmark

import android.util.Log
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import net.chronoflow.core.testing.BoundedProfileInput
import net.chronoflow.core.testing.ProfileCollectionBudget
import net.chronoflow.core.testing.tiers.Perf
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Perf
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    private val packageName: String =
        InstrumentationRegistry.getArguments().getString("targetAppId") ?: "net.chronoflow.app.native"
    private val budget = ProfileCollectionBudget()

    private fun collectProfile(
        startup: Boolean = false,
        block: MacrobenchmarkScope.() -> Unit,
    ) = BoundedProfileInput(
        stepTimeoutMillis = budget.collectorTimeoutMillis,
        journeyTimeoutMillis = budget.collectorTimeoutMillis,
    ).use { collection ->
        collection.step("profile collection") {
            rule.collect(
                packageName,
                maxIterations = budget.maxIterations,
                stableIterations = budget.stableIterations,
                includeInStartupProfile = startup,
                profileBlock = {
                    val name = if (startup) "startup" else "timelineLarge60"
                    val start = System.nanoTime()
                    Log.i("CfProfile", "$name iteration=$iteration begin")
                    try {
                        block()
                    } finally {
                        Log.i(
                            "CfProfile",
                            "$name iteration=$iteration end elapsedMs=${(System.nanoTime() - start) / 1_000_000}",
                        )
                    }
                },
            )
        }
    }

    /** Whatever `MainActivity` shows first at this stack top; Phase 13 adds Today. */
    @Test
    fun startup() =
        collectProfile(startup = true) {
            BoundedProfileInput(journeyTimeoutMillis = budget.journeyTimeoutMillis).use { input ->
                input.step("cancel stale touch") { cancelStaleTouch() }
                input.step("startup home") { pressHome() }
                input.step("startup launch") { startActivityAndWait() }
                input.step("launch overlay appears") {
                    check(device.wait(Until.hasObject(By.res("launch.opening")), 5000)) {
                        "launch overlay never appeared"
                    }
                }
                input.step("launch overlay closes") {
                    check(device.wait(Until.gone(By.res("launch.opening")), 5000)) {
                        "launch overlay never closed"
                    }
                }
                input.step("welcome wordmark") {
                    check(device.wait(Until.hasObject(By.res("welcome.wordmark")), 2000)) {
                        "welcome wordmark missing"
                    }
                }
                input.step("welcome sign in") {
                    check(device.wait(Until.hasObject(By.res("welcome.signIn")), 1000)) {
                        "welcome sign in missing"
                    }
                }
                // Keep collection open through the intro's final animation; the sign-in button remains visible.
                input.step("welcome intro completion") { device.wait(Until.gone(By.res("welcome.signIn")), 900) }
            }
        }

    @After fun restoreWindowSize() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).executeShellCommand("wm size reset")
    }

    @Test fun timelineWide() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("wm size 1840x2208")
        try {
            collectProfile {
                BoundedProfileInput(journeyTimeoutMillis = budget.journeyTimeoutMillis).use { input ->
                    input.step("wide window") { device.executeShellCommand("wm size 1840x2208") }
                    input.step("wide home") { pressHome() }
                    input.step("wide launch") { startActivityAndWait(large60Intent(packageName)) }
                    input.step("wide presented") { awaitWidePresented() }
                    input.step("wide open step") {
                        val list =
                            checkNotNull(device.findObject(By.res(large60OpeningWideListTag))) {
                                "opening wide list missing"
                            }
                        checkNotNull(list.findObject(By.res("trip.block.$LARGE60_OPENING_FIRST_BLOCK"))) {
                            "opening wide row missing"
                        }.click()
                    }
                    input.step("wide step presented") {
                        check(device.wait(Until.hasObject(By.res("step.sheet")), 5_000)) { "wide step missing" }
                    }
                    input.step("wide close step") { device.pressBack() }
                    input.step("wide step closed") {
                        check(device.wait(Until.gone(By.res("step.sheet")), 5_000)) { "wide step stayed open" }
                    }
                    val bounds =
                        input.step("wide page bounds") {
                            checkNotNull(device.findObject(By.res("trip.page"))) { "wide page missing" }.visibleBounds
                        }
                    repeat(3) { gesture ->
                        input.step("wide gesture ${gesture + 1}/3") { device.swipeTimeline(bounds) }
                    }
                    input.step("wide progress") { check(topDayIndex() > 0) { "wide profile did not scroll" } }
                    input.step("restore phone window") { device.executeShellCommand("wm size reset") }
                    input.step("phone toggle returns") {
                        check(device.wait(Until.hasObject(By.res("trip.viewToggle")), 5_000)) {
                            "phone toggle missing after resize"
                        }
                    }
                }
            }
        } finally {
            device.executeShellCommand("wm size reset")
        }
    }

    @Test
    fun timelineLarge60() =
        collectProfile {
            BoundedProfileInput(journeyTimeoutMillis = budget.journeyTimeoutMillis).use { input ->
                input.step("cancel stale touch") { cancelStaleTouch() }
                input.step("trip home") { pressHome() }
                input.step("trip launch") { startActivityAndWait(large60Intent(packageName)) }
                input.step("timeline presented") { awaitPresented() }
                val bounds =
                    input.step("trip bounds") {
                        checkNotNull(device.findObject(By.res("trip.page"))) {
                            "trip page missing"
                        }.visibleBounds
                    }
                input.step("opening day") {
                    check(topDayIndex() == 0) {
                        "large60 did not open on day 0"
                    }
                }
                // Opening starts after the summary; reveal it so the new page chrome gets profile coverage too.
                val x = bounds.left + bounds.width() / 2
                input.step("reveal summary") {
                    check(
                        device.swipe(
                            x,
                            bounds.top + bounds.height() * 15 / 100,
                            x,
                            bounds.top + bounds.height() * 85 / 100,
                            26,
                        ),
                    ) { "summary swipe rejected" }
                }
                input.step("summary visible") {
                    check(device.wait(Until.hasObject(By.res("trip.summary")), 5_000)) {
                        "summary did not appear"
                    }
                }
                input.step("open day menu") {
                    checkNotNull(device.findObject(By.res("trip.dayMenu"))) {
                        "day menu missing"
                    }.click()
                }
                input.step("day menu visible") {
                    check(device.wait(Until.hasObject(By.res("trip.dayMenu.day.0")), 5_000)) {
                        "day menu did not open"
                    }
                }
                input.step("select opening day") {
                    checkNotNull(device.findObject(By.res("trip.dayMenu.day.0"))) {
                        "day 0 missing"
                    }.click()
                }
                input.step("day menu closed") {
                    check(device.wait(Until.gone(By.res("trip.dayMenu.day.0")), 5_000)) {
                        "day menu did not close"
                    }
                }
                input.step("summary return day") {
                    check(topDayIndex() == 0) { "summary coverage did not return to day 0" }
                }
                input.step("open step sheet") {
                    val target = By.res("trip.block.$LARGE60_OPENING_FIRST_BLOCK").clickable(true)
                    check(device.wait(Until.hasObject(target), 5_000)) { "opening checkable click target missing" }
                    checkNotNull(device.findObject(target)).click()
                }
                input.step("expand native step sheet") {
                    check(device.wait(Until.hasObject(By.res("trip.sheet")), 5_000)) { "native sheet missing" }
                    val sheet = checkNotNull(device.findObject(By.res("trip.sheet"))).visibleBounds
                    check(
                        device.swipe(
                            sheet.centerX(),
                            sheet.top + sheet.width() / 16,
                            sheet.centerX(),
                            sheet.top / 10,
                            26,
                        ),
                    ) { "sheet expansion swipe rejected" }
                }
                input.step("step actions ready") {
                    check(device.wait(Until.hasObject(By.res("step.done")), 5_000)) { "step actions missing" }
                }
                input.step("mark step") { checkNotNull(device.findObject(By.res("step.done"))).click() }
                input.step("undo ready") {
                    check(device.wait(Until.hasObject(By.res("step.undo")), 5_000)) { "check-off did not land" }
                }
                input.step("undo step") { checkNotNull(device.findObject(By.res("step.undo"))).click() }
                input.step("undo landed") {
                    check(device.wait(Until.hasObject(By.res("step.done")), 5_000)) { "undo did not land" }
                }
                input.step("close step") { device.pressBack() }
                input.step("step closed") {
                    check(device.wait(Until.gone(By.res("step.sheet")), 5_000)) { "step sheet stayed open" }
                }
                repeat(
                    3,
                ) { gesture -> input.step("timeline gesture ${gesture + 1}/3") { device.swipeTimeline(bounds) } }
                input.step("timeline progress") {
                    check(topDayIndex() >= 5) { "profile gestures must cover the first six-day tour" }
                    check(!device.hasObject(By.res("trip.plan.end"))) {
                        "bounded-swipe-v3 reached the plan end"
                    }
                }
                input.step("open list") { device.toggleTripView(list = true) }
                val listBounds =
                    input.step("list bounds") {
                        val list =
                            checkNotNull(device.findObject(By.res("trip.page"))) {
                                "list page missing"
                            }
                        list.setGestureMargin(list.visibleBounds.width() / 5)
                        list.visibleBounds
                    }
                repeat(
                    3,
                ) { gesture -> input.step("list gesture ${gesture + 1}/3") { device.swipeTimeline(listBounds) } }
                input.step("return to timeline") { device.toggleTripView(list = false) }
                input.step("return timeline ready") {
                    check(device.wait(Until.hasObject(By.res("trip.timeline.ready")), 10_000)) {
                        "returned timeline never became ready"
                    }
                }
            }
        }
}
