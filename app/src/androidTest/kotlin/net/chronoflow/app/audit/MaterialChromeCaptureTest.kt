package net.chronoflow.app.audit

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Capture
import net.chronoflow.debug.DebugRefreshProbe
import net.chronoflow.feature.settings.SettingsDebugCounters
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Capture
@OptIn(ExperimentalTestApi::class)
@RunWith(Parameterized::class)
class MaterialChromeCaptureTest(
    private val language: String,
    private val theme: String,
) : ScreenTest() {
    @get:Rule val captureTimings: TestRule = DeviceCaptureTimingRule()
    private val screenCapture = DeviceScreenCapture(compose)

    @Before fun requireRequestedCaptures() = DeviceScreenCapture.requireMaterial()

    private fun open(
        tag: String,
        extras: Map<String, String> = emptyMap(),
    ) {
        launch(
            mapOf(
                "CF_AUTH_TEST_SIGNED_IN" to "demo",
                "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                "CF_MOCK_DELAY" to "0",
                "cf.settings.language" to language,
                "cf.settings.theme" to theme,
            ) +
                extras,
        )
        ready(tag)
    }

    private fun ready(tag: String) = compose.waitUntilExactlyOneExists(hasTestTag(tag), 5_000)

    private fun shot(
        row: String,
        context: String,
    ) = screenCapture.capture("material/${row}_${context}_pixel_${language}_$theme")

    private fun settings(route: String) = mapOf("CF_SETTINGS_ROUTE" to route)

    private fun wizard(board: String) = mapOf("CF_WIZARD_BOARD_CAPTURE" to board, "CF_WIZARD_AUTO_OPEN" to "1")

    private fun trip(sheet: String) =
        mapOf(
            "CF_PLAN_TEST_ROUTE" to "trip:0199a3c0-0000-7000-8000-000000000001",
            "CF_PLAN_TEST_FREEZE_CLOCK" to "1",
            "CF_PLAN_TEST_SHEET" to sheet,
        )

    @Test fun capturesTabBar() {
        open("today.screen")
        shot("TabBar", "Today")
    }

    @Test fun capturesMediumTitle() {
        open("settings.profile", settings("none"))
        shot("MediumTitle", "expanded")
        compose.onNodeWithTag("settings.screen.content").performTouchInput { swipeUp() }
        shot("MediumTitle", "collapsed")
    }

    @Test fun capturesSheet() {
        open("step.sheet", trip("step:sleep-sleep-29865450"))
        shot("Sheet", "step")
        open("legend.sheet", trip("legend"))
        shot("Sheet", "legend")
    }

    @Test fun capturesAlert() {
        open("cf.dialog.confirm", wizard("WizardDiscard"))
        shot("Alert", "discardDestructive")
        compose.onNodeWithTag("cf.dialog.dismiss").performClick()
        open("settings.twoFactor.disable", settings("twoFactor") + ("CF_AUTH_TEST_SIGNED_IN" to "twofactor"))
        compose.onNodeWithTag("settings.twoFactor.disable").performScrollTo().performClick()
        ready("settings.security.prompt.forgot")
        compose.onNodeWithTag("settings.security.prompt.forgot").performClick()
        ready("settings.security.prompt.forgotAlert")
        shot("Alert", "passwordReset")
        compose.onNodeWithTag("cf.dialog.dismiss").performClick()
    }

    @Test fun capturesSwitch() {
        open("settings.preferences.Sleep", settings("preferences"))
        compose.onNodeWithTag("settings.preferences.Sleep").performScrollTo()
        shot("Switch", "on")
        compose.onNodeWithTag("settings.preferences.Sleep").performClick()
        shot("Switch", "off")
    }

    @Test fun capturesSegmented() {
        open("wizard.route.stops", wizard("WizardWhere"))
        shot("Segmented", "wizardStops")
        open(
            "reminders.lead",
            settings("reminders") +
                mapOf("cf.notificationAuthorization" to "granted", "cf.notificationPermissionRequested" to "1"),
        )
        shot("Segmented", "reminderLead")
    }

    @Test fun capturesDatePicker() {
        open("wizard.0.departure.date", wizard("WizardOutbound"))
        compose.onNodeWithTag("wizard.0.departure.date").performClick()
        ready("wizard.date-dialog")
        shot("DatePicker", "outbound")
        compose.onNodeWithTag("wizard.picker.cancel").performClick()
    }

    @Test fun capturesTimePicker() {
        open("wizard.0.departure.time", wizard("WizardOutbound"))
        compose.onNodeWithTag("wizard.0.departure.time").performClick()
        ready("wizard.time-dialog")
        shot("TimePicker", "outbound")
        compose.onNodeWithTag("wizard.picker.cancel").performClick()
    }

    @Test fun capturesSnackbar() {
        val initialLanguage = if (language == "en") "nl" else "en"
        open("settings.language.choice.$language", settings("language") + ("cf.settings.language" to initialLanguage))
        compose.onNodeWithTag("settings.language.choice.$language").performScrollTo().performClick()
        compose.waitUntil(5_000) { SettingsDebugCounters.languageSaved.get() == 1 }
        shot("Snackbar", "languageSaved")
    }

    @Test fun capturesPullToRefresh() {
        open(
            "trip.0199a3c0-0000-7000-8000-000000000001",
            mapOf("CF_PLAN_TEST_ROUTE" to "trips", "CF_MOCK_DELAY" to "5"),
        )
        pullTrips()
        val indicator =
            SemanticsMatcher.expectValue(
                SemanticsProperties.ProgressBarRangeInfo,
                ProgressBarRangeInfo.Indeterminate,
            )
        compose.waitUntilExactlyOneExists(indicator, 5_000)
        shot("PullToRefresh", "indicator")
        open("trip.0199a3c0-0000-7000-8000-000000000001", mapOf("CF_PLAN_TEST_ROUTE" to "trips"))
        pullTrips()
        compose.waitUntil(5_000) { DebugRefreshProbe.presentations.get() == 1 }
        shot("PullToRefresh", "resultCapsule")
    }

    private fun pullTrips() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val list = checkNotNull(device.wait(Until.findObject(By.res("trips.list")), 5_000))
        val bounds = list.visibleBounds
        device.swipe(
            bounds.centerX(),
            bounds.top + bounds.height() / 4,
            bounds.centerX(),
            bounds.bottom - bounds.height() / 6,
            30,
        )
        compose.waitUntil(5_000) { DebugRefreshProbe.accepted.get() == 1 }
    }

    @Test fun capturesProgress() {
        open("wizard.progress", wizard("WizardWhereEmpty"))
        shot("Progress", "wizardWhere")
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}-{1}")
        fun cases(): List<Array<Any>> =
            listOf(arrayOf("en", "light"), arrayOf("nl", "light"), arrayOf("en", "dark"), arrayOf("nl", "dark"))
    }
}
