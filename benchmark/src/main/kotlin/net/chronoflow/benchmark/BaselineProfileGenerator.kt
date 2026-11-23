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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

@Perf
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    private val packageName: String =
        InstrumentationRegistry.getArguments().getString("targetAppId") ?: "net.chronoflow.app"
    private val budget = ProfileCollectionBudget()

    private fun collectProfile(
        name: String,
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

    @Before fun clean() {
        BoundedProfileInput().use { input ->
            input.step("clear app") {
                UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).clearApp(packageName)
            }
        }
    }

    /** Collect the signed-in launch through the first Today content. */
    @Test
    fun startup() =
        collectProfile("startup", startup = true) {
            BoundedProfileInput(journeyTimeoutMillis = budget.journeyTimeoutMillis).use { input ->
                input.step("cancel stale touch") { cancelStaleTouch() }
                input.step("startup home") { pressHome() }
                input.step("startup launch") { startActivityAndWait(signedInStartupIntent(packageName)) }
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
                input.step("Today content") {
                    check(
                        device.wait(Until.hasObject(By.res("today.trip")), 5_000) ||
                            device.hasObject(By.res("today.empty")),
                    ) {
                        "Today content missing"
                    }
                }
            }
        }

    @After fun restoreWindowSize() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).executeShellCommand("wm size reset")
    }

    @Test fun timelineWide() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.executeShellCommand("wm size 1840x2208")
        try {
            collectProfile("timelineWide") {
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
        collectProfile("timelineLarge60") {
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
                device.jumpToLastDay(input)
            }
        }

    @Test fun startupSignedOut() =
        collectProfile("startupSignedOut", startup = true) {
            BoundedProfileInput(journeyTimeoutMillis = budget.journeyTimeoutMillis).use { input ->
                input.step("cancel stale touch") { cancelStaleTouch() }
                input.step("signed-out cold process") { killProcess() }
                input.step("signed-out home") { pressHome() }
                input.step("signed-out launch") { startActivityAndWait() }
                input.step("signed-out Welcome") { device.requireTag("welcome.signIn", 6000) }
            }
        }

    @Test fun startupSignedIn() =
        collectProfile("startupSignedIn", startup = true) {
            BoundedProfileInput(journeyTimeoutMillis = budget.journeyTimeoutMillis).use { input ->
                input.step("cancel stale touch") { cancelStaleTouch() }
                input.step("signed-in cold process") { killProcess() }
                input.step("demo sign in or restore") { signInDemo() }
                input.step("Today trip") { device.requireTag("today.trip", 10_000) }
            }
        }

    @Test fun tripJourney() =
        collectProfile("tripJourney") {
            BoundedProfileInput(journeyTimeoutMillis = budget.journeyTimeoutMillis).use { input ->
                input.step("cancel stale touch") { cancelStaleTouch() }
                input.step("trip cold process") { killProcess() }
                input.step("demo sign in or restore") { signInDemo() }
                input.step("My trips tab") { device.requireTag("tab.trips").click() }
                input.step("My trips list") { device.requireTag("trips.list") }
                input.step("open active trip") { device.requireTag(DEMO_TRIP_TAG).click() }
                input.step("active timeline ready") { device.requireTag("trip.timeline.ready", 10_000) }
                input.step("first-trip offer guard") { device.dismissFirstTripOffer() }
                val bounds = input.step("active trip bounds") { device.requireTag("trip.page").visibleBounds }
                input.step("five timeline swipes down") { device.swipeTimeline(bounds) }
                input.step("five timeline swipes up") { device.swipeTimeline(bounds, reverse = true) }
                input.step("active trip list") { device.toggleTripView(list = true) }
                val listBounds = input.step("active list bounds") { device.requireTag("trip.page").visibleBounds }
                input.step("list swipes") { device.swipeTimeline(listBounds) }
                input.step("active trip timeline") { device.toggleTripView(list = false) }
                input.step("active timeline returns") { device.requireTag("trip.timeline.ready", 10_000) }
                input.step("active step") { device.firstVisibleBlock().click() }
                input.step("active step ready") { device.requireTag("step.sheet") }
                input.step("dismiss active step") { device.pressBack() }
                input.step("active step closed") {
                    check(device.wait(Until.gone(By.res("step.sheet")), 3000)) { "step.sheet stayed open" }
                }
                input.step("active day menu") { device.requireTag("trip.dayMenu").click() }
                input.step("jump to highest visible day") {
                    val pattern = Pattern.compile("trip\\.dayMenu\\.day\\.\\d+")
                    check(device.wait(Until.hasObject(By.res(pattern)), 5000)) { "active day menu never opened" }
                    val days = device.findObjects(By.res(pattern))
                    checkNotNull(days.maxByOrNull { it.resourceName.substringAfterLast('.').toInt() }) {
                        "active day menu has no days"
                    }.click()
                    check(device.wait(Until.gone(By.res(pattern)), 3000)) { "active day menu stayed open" }
                }
                input.step("back to My trips") { device.pressBack() }
                input.step("Today tab") { device.requireTag("tab.today").click() }
                input.step("Today hero opens trip") { device.requireTag("today.trip").click() }
                input.step("Today trip scroll") { device.requireTag("trip.scroll") }
                input.step("back from Today trip") { device.pressBack() }
            }
        }

    @Test fun wizardToReady() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        BoundedProfileInput(
            stepTimeoutMillis = budget.setupTimeoutMillis,
            journeyTimeoutMillis = budget.setupTimeoutMillis,
        ).use { input ->
            input.step("wizard demo session") { device.signInDemo(packageName) }
            // Sign-in publishes the session before FullPull stores settings and trips together.
            input.step("wizard account snapshot stored") { device.requireTag("today.trip", 15_000) }
        }
        collectProfile("wizardToReady") {
            BoundedProfileInput(journeyTimeoutMillis = budget.journeyTimeoutMillis).use { input ->
                input.step("cancel stale touch") { cancelStaleTouch() }
                input.step("wizard cold process") { killProcess() }
                input.step("wizard launch") {
                    launch(mapOf("cf.perf-account" to "demo", "cf.perf-wizard" to "WizardSaveJourney"))
                }
                val entries = Pattern.compile("wizard\\.choice\\.one-way|wizard\\.ready")
                input.step("wizard entry or Welcome") {
                    val routes = Pattern.compile("${entries.pattern()}|welcome\\.signIn")
                    check(device.wait(Until.hasObject(By.res(routes)), 10_000)) {
                        "neither wizard entry nor welcome.signIn appeared"
                    }
                }
                if (input.step("wizard session needs recovery") { device.hasObject(By.res("welcome.signIn")) }) {
                    // A reinstall clears the session; omit the wizard extra until FullPull commits its snapshot.
                    BoundedProfileInput(
                        stepTimeoutMillis = budget.setupTimeoutMillis,
                        journeyTimeoutMillis = budget.setupTimeoutMillis,
                    ).use { recovery ->
                        recovery.step("wizard recover demo session") { signInDemo() }
                        recovery.step("wizard recovered account snapshot stored") {
                            device.requireTag("today.trip", 15_000)
                        }
                    }
                    input.step("wizard recovered cold process") { killProcess() }
                    input.step("wizard recovered launch") {
                        launch(mapOf("cf.perf-account" to "demo", "cf.perf-wizard" to "WizardSaveJourney"))
                    }
                }
                input.step("wizard entry") {
                    check(device.wait(Until.hasObject(By.res(entries)), 10_000)) {
                        "wizard entry never appeared after account sync"
                    }
                }
                if (input.step("one-way entry") { device.hasObject(By.res("wizard.choice.one-way")) }) {
                    input.step("one-way journey") { device.requireTag("wizard.choice.one-way").click() }
                    input.step("continue fine tune") {
                        device.requireEnabledTag("wizard.fine-tune.continue", 5000).click()
                    }
                    input.step("wizard Ready") { device.requireEnabledTag("wizard.ready.continue", 10_000) }
                }
                input.step("Ready or quiet recovery") { device.requireTag("wizard.ready", 10_000) }
            }
        }
    }
}
