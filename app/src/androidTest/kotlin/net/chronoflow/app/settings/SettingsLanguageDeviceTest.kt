package net.chronoflow.app.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import net.chronoflow.app.testing.ScreenTest
import net.chronoflow.core.testing.tiers.Smoke
import net.chronoflow.feature.settings.SettingsDebugCounters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@Smoke
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsLanguageDeviceTest : ScreenTest() {
    @Test fun languageChoiceUpdatesAppAndReturnsToAccount() {
        val started = System.nanoTime()
        val scenario =
            launch(
                mapOf(
                    "CF_AUTH_TEST_SIGNED_IN" to "demo",
                    "CF_AUTH_TEST_WALKTHROUGH_SEEN" to "1",
                    "CF_SETTINGS_ROUTE" to "language",
                    "CF_MOCK_DELAY" to "0",
                    // Selecting the current choice is a no-op, including under Dutch audit defaults.
                    "cf.settings.language" to "en",
                ),
            )
        compose.waitUntilExactlyOneExists(hasTestTag("settings.language.choice.nl"), 5_000)
        compose.onNodeWithTag("settings.language.choice.nl").performScrollTo().performClick()
        waitFor(5_000) {
            var language = ""
            scenario.onActivity {
                language =
                    it.resources.configuration.locales[0]
                        .language
            }
            language == "nl" && SettingsDebugCounters.languageSaved.get() == 1
        }
        assertEquals(1, SettingsDebugCounters.languageSaved.get())
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        compose.waitUntilExactlyOneExists(hasTestTag("settings.account.screen"), 5_000)
        compose.onNodeWithTag("settings.account.screen").assertIsDisplayed()
        scenario.onActivity {
            assertEquals(
                "nl",
                it.resources.configuration.locales[0]
                    .language,
            )
        }
        assertTrue("settings language check exceeded 10 seconds", System.nanoTime() - started < 10_000_000_000L)
    }
}
