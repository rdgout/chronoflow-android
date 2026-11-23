package net.chronoflow.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

const val DEMO_TRIP_TAG = "trip.0199a3c0-0000-7000-8000-000000000001"
private val shellWord = Regex("[A-Za-z0-9_.-]+")

fun UiDevice.clearApp(pkg: String) {
    require(shellWord.matches(pkg))
    val emulator =
        listOf("ro.boot.qemu", "ro.kernel.qemu").any {
            executeShellCommand("getprop $it").trim() == "1"
        }
    check(emulator || InstrumentationRegistry.getArguments().getString("cf.perfClear") == "1") {
        "clearing physical-device data requires PERF_CLEAR=1 through the runner"
    }
    executeShellCommand("pm clear $pkg")
}

fun MacrobenchmarkScope.launch(extras: Map<String, String> = emptyMap()) =
    startActivityAndWait { intent -> extras.forEach { (key, value) -> intent.putExtra(key, value) } }

fun UiDevice.launchApp(
    pkg: String,
    extras: Map<String, String> = emptyMap(),
) {
    require(shellWord.matches(pkg))
    val flags =
        extras.entries.joinToString(" ") { (key, value) ->
            require(shellWord.matches(key) && shellWord.matches(value))
            "--es $key $value"
        }
    executeShellCommand(
        "am start -W -n $pkg/net.chronoflow.app.MainActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER $flags",
    )
}

fun UiDevice.requireTag(
    tag: String,
    timeout: Long = 5000,
): UiObject2 {
    check(wait(Until.hasObject(By.res(tag)), timeout)) { "$tag never appeared" }
    return checkNotNull(findObject(By.res(tag))) { "$tag disappeared" }
}

fun UiDevice.requireEnabledTag(
    tag: String,
    timeout: Long,
): UiObject2 {
    val selector = By.res(tag).enabled(true)
    check(wait(Until.hasObject(selector), timeout)) { "$tag never became enabled" }
    return checkNotNull(findObject(selector)) { "$tag disappeared" }
}

fun UiDevice.signInFromWelcome() {
    requireTag("welcome.signIn").click()
    requireTag("sign-in.email", 3000).text = "demo@example.com"
    requireTag("sign-in.password", 3000).text = "password"
    requireEnabledTag("sign-in.submit", 3000).click()
}

fun UiDevice.awaitDemoSession() {
    check(wait(Until.hasObject(By.res(Pattern.compile("welcome\\.signIn|today\\.screen"))), 6000)) {
        "neither welcome.signIn nor today.screen appeared"
    }
    if (hasObject(By.res("welcome.signIn"))) signInFromWelcome()
    requireTag("today.screen", 10_000)
}

fun MacrobenchmarkScope.signInDemo() {
    launch(mapOf("cf.perf-account" to "demo"))
    device.awaitDemoSession()
}

fun UiDevice.signInDemo(pkg: String) {
    launchApp(pkg, mapOf("cf.perf-account" to "demo"))
    awaitDemoSession()
}

fun UiDevice.dismissFirstTripOffer() {
    if (hasObject(By.res("reminders.firstTrip"))) {
        pressBack()
        check(wait(Until.gone(By.res("reminders.firstTrip")), 3000)) { "reminders.firstTrip stayed open" }
    }
}

fun UiDevice.firstVisibleBlock(): UiObject2 =
    checkNotNull(findObjects(By.res(Pattern.compile("trip\\.block\\..*"))).minByOrNull { it.visibleBounds.top }) {
        "no visible trip block"
    }
