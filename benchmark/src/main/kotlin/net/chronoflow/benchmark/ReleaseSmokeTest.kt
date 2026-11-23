package net.chronoflow.benchmark

import android.view.accessibility.AccessibilityEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import net.chronoflow.core.testing.tiers.Perf
import net.chronoflow.core.testing.verifyRecreatedSheet
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@Perf
@RunWith(AndroidJUnit4::class)
class ReleaseSmokeTest {
    @Before fun startWithAFreshProcess() {
        device.executeShellCommand("am force-stop $pkg")
    }

    @After fun noNativeCrashWasLogged() {
        assertFalse("native crash in crash buffer", device.executeShellCommand("logcat -b crash -d").contains(pkg))
    }

    private fun pid(): String =
        device.executeShellCommand("pidof $pkg").trim().also {
            assertTrue("app has no pid", it.isNotBlank())
        }

    @Test fun todayTripsAndSettingsOpen() {
        device.launchApp(pkg)
        device.requireTag("today.screen", 10_000)
        val before = pid()
        device.requireTag("tab.trips").click()
        device.requireTag("trips.list")
        device.requireTag("tab.settings").click()
        device.requireTag("settings.signOut")
        device.requireTag("tab.today").click()
        device.requireTag("today.screen")
        assertEquals(before, pid())
    }

    @Test fun tripSheetSurvivesRecreation() {
        device.launchApp(pkg)
        device.requireTag("today.screen", 10_000)
        val before = pid()
        device.requireTag("tab.trips").click()
        device.requireTag("trips.list")
        device.requireTag(DEMO_TRIP_TAG).click()
        device.requireTag("trip.timeline.ready", 10_000)
        device.firstVisibleBlock().click()
        device.requireTag("step.sheet")
        val old = device.executeShellCommand("settings get system font_scale").trim()
        val scale = if (old == "1.15") "1.2" else "1.15"
        try {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeAndWaitForEvent(
                { device.executeShellCommand("settings put system font_scale $scale") },
                { event ->
                    event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                        event.packageName?.toString() == pkg
                },
                5000,
            )
            verifyRecreatedSheet(
                requireSheet = { device.requireTag("step.sheet") },
                requireSameProcess = { assertEquals(before, pid()) },
                dismissSheet = { device.pressBack() },
                requireSheetGone = {
                    check(device.wait(Until.gone(By.res("step.sheet")), 3000)) { "restored step.sheet stayed open" }
                },
                requireUnderlyingPage = { device.requireTag("trip.page") },
            )
        } finally {
            if (old == "null") {
                device.executeShellCommand("settings delete system font_scale")
            } else {
                device.executeShellCommand("settings put system font_scale $old")
            }
        }
    }

    @Test fun wizardSavesToTripPage() {
        device.launchApp(pkg, mapOf("cf.perf-wizard" to "WizardSaveJourney"))
        device.requireTag("wizard.choice.one-way", 10_000)
        val before = pid()
        device.requireTag("wizard.choice.one-way").click()
        device.requireEnabledTag("wizard.fine-tune.continue", 5000).click()
        device.requireEnabledTag("wizard.ready.continue", 10_000).click()
        device.requireTag("trip.page")
        device.dismissFirstTripOffer()
        assertEquals(before, pid())
    }

    companion object {
        private val pkg get() =
            InstrumentationRegistry.getArguments().getString("targetAppId")
                ?: "net.chronoflow.app.native"
        private val device get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        @BeforeClass @JvmStatic
        fun signInOnce() {
            device.executeShellCommand("logcat -b crash -c")
            device.clearApp(pkg)
            device.signInDemo(pkg)
            device.requireTag("today.trip", 15_000)
        }
    }
}
