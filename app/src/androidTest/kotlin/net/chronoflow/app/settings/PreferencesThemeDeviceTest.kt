package net.chronoflow.app.settings

import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.chronoflow.app.ChronoFlowApp
import net.chronoflow.app.MainActivity
import net.chronoflow.app.ioViolationsForTest
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.designsystem.generated.CfDarkColors
import net.chronoflow.core.model.config.ThemeChoice
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 31)
class PreferencesThemeDeviceTest : ScreenTest() {
    @Test fun themeChangeAppliesWithoutRestart() {
        val started = System.nanoTime()
        val scenario = launch(preferencesSeed())
        lateinit var initial: MainActivity
        scenario.onActivity { initial = it }
        assertLight(scenario)
        chooseDark()
        waitForDark()
        scenario.onActivity { assertSame(initial, it) }
        assertPersistedDark(scenario)
        assertTrue("theme check exceeded 10 seconds", System.nanoTime() - started < 10_000_000_000L)
    }

    @Test fun themeSurvivesRecreateAndRelaunch() {
        val started = System.nanoTime()
        val scenario = launch(preferencesSeed())
        assertLight(scenario)
        chooseDark()
        waitForDark()
        scenario.recreate()
        waitForDark()
        assertPersistedDark(scenario)
        scenario.close()
        val relaunched = launch(preferencesSeed() + ("cf.settings.theme" to null))
        waitForDark()
        assertPersistedDark(relaunched)
        assertTrue("theme persistence check exceeded 10 seconds", System.nanoTime() - started < 10_000_000_000L)
    }

    @Test fun themeChangeOnTripStaysStrictModeClean() {
        val started = System.nanoTime()
        val scenario =
            launch(
                mapOf(
                    "CF_AUTH_TEST_SIGNED_IN" to "demo",
                    "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                    "CF_PLAN_TEST_ROUTE" to "trip:0199a3c0-0000-7000-8000-000000000002",
                    "CF_PLAN_TEST_FREEZE_CLOCK" to "1",
                    "CF_MOCK_DELAY" to "0",
                    "cf.settings.theme" to "light",
                ),
            )
        compose.waitUntilExactlyOneExists(hasTestTag("trip.timeline.ready"), 5_000)
        assertLight(scenario)
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ChronoFlowApp
        runBlocking(app.graph.dispatchers.io) { app.graph.deviceAppearance.setTheme(ThemeChoice.Dark) }
        assertPersistedDark(scenario)
        compose.waitForIdle()
        compose.onNodeWithTag("trip.timeline.ready").assertExists()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertEquals(0, app.graph.ioViolationsForTest)
        }
        assertTrue("trip theme check exceeded 10 seconds", System.nanoTime() - started < 10_000_000_000L)
    }

    private fun preferencesSeed() =
        mapOf(
            "CF_AUTH_TEST_SIGNED_IN" to "demo",
            "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
            "CF_SETTINGS_ROUTE" to "preferences",
            "CF_MOCK_DELAY" to "0",
            "cf.settings.theme" to "light",
        )

    private fun chooseDark() {
        compose.waitUntilExactlyOneExists(hasTestTag("settings.preferences.theme.dark"), 5_000)
        compose.onNodeWithTag("settings.preferences.theme.dark").performScrollTo().performClick()
    }

    private fun waitForDark() {
        compose.waitUntilExactlyOneExists(hasTestTag("settings.preferences.screen"), 5_000)
        waitFor {
            val pixels = compose.onNodeWithTag("settings.preferences.screen").captureToImage().toPixelMap()
            matches(CfDarkColors.background, pixels[0, 0])
        }
    }

    private fun assertPersistedDark(scenario: ActivityScenario<MainActivity>) {
        waitFor {
            var mode = Configuration.UI_MODE_NIGHT_UNDEFINED
            scenario.onActivity { mode = it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK }
            mode == Configuration.UI_MODE_NIGHT_YES
        }
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as ChronoFlowApp
        assertEquals(
            ThemeChoice.Dark,
            runBlocking(app.graph.dispatchers.io) {
                app.graph.deviceAppearance.theme
                    .first()
            },
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertEquals(0, app.graph.ioViolationsForTest)
        }
    }

    private fun assertLight(scenario: ActivityScenario<MainActivity>) {
        scenario.onActivity {
            assertEquals(
                Configuration.UI_MODE_NIGHT_NO,
                it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK,
            )
        }
    }

    private fun matches(
        expected: Color,
        actual: Color,
    ): Boolean =
        abs(expected.red - actual.red) <= 2f / 255 &&
            abs(expected.green - actual.green) <= 2f / 255 &&
            abs(expected.blue - actual.blue) <= 2f / 255
}
