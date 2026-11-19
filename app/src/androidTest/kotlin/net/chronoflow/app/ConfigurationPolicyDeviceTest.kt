package net.chronoflow.app

import android.app.LocaleManager
import android.content.res.Configuration
import android.os.LocaleList
import android.view.View
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import net.chronoflow.core.testing.tiers.Smoke
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalTestApi::class)
@Smoke
class ConfigurationPolicyDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val context = instrumentation.targetContext
    private val locales get() = context.getSystemService(LocaleManager::class.java)

    private fun withMainActivity(block: (ActivityScenario<MainActivity>) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitUntilExactlyOneExists(hasTestTag("app.root"), 5_000)
            block(scenario)
        }
    }

    private fun <T> ActivityScenario<MainActivity>.read(read: (MainActivity) -> T): T {
        var result: T? = null
        onActivity { result = read(it) }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    @Test
    fun appLocaleArabicIsEnglishLtr() {
        locales.applicationLocales = LocaleList.forLanguageTags("ar-EG")
        try {
            withMainActivity { scenario ->
                val locale = scenario.read { it.resources.configuration.locales[0] }
                assertEquals("en", locale.toLanguageTag())
                assertEquals(View.LAYOUT_DIRECTION_LTR, scenario.read { it.window.decorView.layoutDirection })
                assertEquals("1,234,567", "%,d".format(locale, 1234567))
            }
        } finally {
            locales.applicationLocales = LocaleList.getEmptyLocaleList()
        }
    }

    @Test
    fun appLocaleDutchIsDutch() {
        locales.applicationLocales = LocaleList.forLanguageTags("nl")
        try {
            withMainActivity { scenario ->
                assertEquals(
                    "nl",
                    scenario.read {
                        it.resources.configuration.locales[0]
                            .language
                    },
                )
            }
        } finally {
            locales.applicationLocales = LocaleList.getEmptyLocaleList()
        }
    }

    @Test
    fun fontScaleTwoIsCapped() {
        device.executeShellCommand("settings put system font_scale 2.0")
        try {
            withMainActivity { scenario ->
                assertEquals(1.3f, scenario.read { it.resources.configuration.fontScale }, 0.001f)
            }
        } finally {
            device.executeShellCommand("settings put system font_scale 1.0")
        }
    }

    @Test
    fun nightModeChangeKeepsCapAndLocale() {
        device.executeShellCommand("settings put system font_scale 2.0")
        locales.applicationLocales = LocaleList.forLanguageTags("ar-EG")
        try {
            withMainActivity { scenario ->
                device.executeShellCommand("cmd uimode night yes")
                compose.waitUntil(5_000) {
                    scenario.read { it.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK } ==
                        Configuration.UI_MODE_NIGHT_YES
                }
                assertEquals(1.3f, scenario.read { it.resources.configuration.fontScale }, 0.001f)
                assertEquals(Locale.forLanguageTag("en"), scenario.read { it.resources.configuration.locales[0] })
                assertEquals(View.LAYOUT_DIRECTION_LTR, scenario.read { it.window.decorView.layoutDirection })
            }
        } finally {
            device.executeShellCommand("cmd uimode night no")
            device.executeShellCommand("settings put system font_scale 1.0")
            locales.applicationLocales = LocaleList.getEmptyLocaleList()
        }
    }
}
